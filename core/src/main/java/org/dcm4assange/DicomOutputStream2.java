/*
 * Copyright 2021 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dcm4assange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.ToIntBiFunction;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Sep 2021
 */
public class DicomOutputStream2 extends OutputStream  {
    private static final int BUFFER_SIZE = 8192;
    private OutputStream out;
    private DicomEncoding encoding;
    private boolean includeGroupLength;
    private boolean undefSequenceLength;
    private boolean undefItemLength;
    private byte[] b12 = new byte[12];
    private byte[] swapBuffer;

    public DicomOutputStream2(OutputStream out) {
        this.out = Objects.requireNonNull(out);
    }

    public DicomEncoding encoding() {
        return encoding;
    }

    public DicomOutputStream2 withEncoding(DicomEncoding encoding) {
        this.encoding = Objects.requireNonNull(encoding);
        if (encoding.deflated) {
            out = new DeflaterOutputStream(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
        }
        return this;
    }

    public DicomOutputStream2 withEncoding(DicomObject2 fmi) {
        String tsuid = fmi.getString(Tag.TransferSyntaxUID).orElseThrow(
                () -> new IllegalArgumentException("Missing Transfer Syntax UID in File Meta Information"));

        return withEncoding(DicomEncoding.of(tsuid));
    }

    public DicomOutputStream2 withIncludeGroupLength(boolean includeGroupLength) {
        this.includeGroupLength = includeGroupLength;
        return this;
    }

    public boolean includeGroupLength() {
        return includeGroupLength;
    }

    public boolean undefSequenceLength() {
        return undefSequenceLength;
    }

    public DicomOutputStream2 withUndefSequenceLength(boolean undefSequenceLength) {
        this.undefSequenceLength = undefSequenceLength;
        return this;
    }

    public boolean undefItemLength() {
        return undefItemLength;
    }

    public DicomOutputStream2 withUndefItemLength(boolean undefItemLength) {
        this.undefItemLength = undefItemLength;
        return this;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public DicomOutputStream2 writeFileMetaInformation(DicomObject2 fmi) throws IOException {
        ensureEncoding(DicomEncoding.EVR_LE);
        if (!fmi.containsValue(Tag.TransferSyntaxUID))
            throw new IllegalArgumentException("missing Transfer Syntax UID");

        byte[] b = new byte[132];
        b[128] = 'D';
        b[129] = 'I';
        b[130] = 'C';
        b[131] = 'M';
        write(b);
        write(fmi, DicomObject2::calculateLengthWithGroupLength);
        return withEncoding(fmi);
    }

    public void writeDataSet(DicomObject2 dcmobj) throws IOException {
        if (encoding == null)
            throw new IllegalStateException("encoding not initialized");

        write(dcmobj, includeGroupLength ? DicomObject2::calculateLengthWithGroupLength : DicomObject2::calculateLength);
    }

    public void writeCommandSet(DicomObject2 dcmobj) throws IOException {
        ensureEncoding(DicomEncoding.IVR_LE);
        if (dcmobj.isEmpty())
            throw new IllegalArgumentException("empty command set");

        write(dcmobj, DicomObject2::calculateLengthWithGroupLength);
    }

    private void ensureEncoding(DicomEncoding encoding) {
        if (this.encoding == null)
            this.encoding = encoding;
        else if (this.encoding != encoding)
            throw new IllegalStateException("invalid encoding: " + encoding);
    }

    private void write(DicomObject2 dcmobj, ToIntBiFunction<DicomObject2, DicomOutputStream2> calc) throws IOException {
        DicomObject2 tmp = new DicomObject2(dcmobj);
        calc.applyAsInt(tmp, this);
        tmp.writeTo(this);
    }

    void writeHeader(int tag, VR vr, int length) throws IOException {
        write(b12, 0, fillHeader(tag, vr, length, b12));
    }

    private int fillHeader(int tag, VR vr, int length, byte[] b12) {
        ByteOrder byteOrder = encoding.byteOrder;
        byteOrder.tagToBytes(tag, b12, 0);
        if (!encoding.explicitVR || vr == null) {
            byteOrder.intToBytes(length, b12, 4);
            return 8;
        }
        b12[4] = (byte) (vr.code >>> 8);
        b12[5] = (byte) vr.code;
        if (vr.evr8) {
            byteOrder.shortToBytes(length, b12, 6);
            return 8;
        }
        b12[6] = 0;
        b12[7] = 0;
        byteOrder.intToBytes(length, b12, 8);
        return 12;
    }

    void write(Sequence seq) throws IOException {
        writeHeader(seq.tag, VR.SQ, undefSequenceLength ? -1 : valueLength(seq));
        for (int i = 0, n = seq.size(); i < n; i++) {
            DicomObject2 item = seq.getItem(i);
            writeHeader(Tag.Item, null, undefItemLength ? -1 : item.length());
            item.writeTo(this);
            if (undefItemLength)
                writeHeader(Tag.ItemDelimitationItem, null, 0);
        }
        if (undefSequenceLength)
            writeHeader(Tag.SequenceDelimitationItem, null, 0);
    }

    private int valueLength(Sequence seq) {
        int length = (undefItemLength ? 16 : 8) * seq.size();
        for (int i = 0; i < seq.size(); i++) {
            length += seq.getItem(i).length();
        }
        return length;
    }

    void write(int tag, VR vr, byte[] b) throws IOException {
        writeHeader(tag, vr, (b.length + 1) & ~1);
        write(b);
        if ((b.length & 1) != 0)
            write(vr.paddingByte);
    }

    void write(long header, MemoryCache.DicomInput dicomInput) throws IOException {
        int tag = dicomInput.tagAt(header & 0x00ffffffffffffffL);
        VR vr = VR.fromHeader(header);
        int vlen = dicomInput.header2valueLength(header);
        writeHeader(tag, vr, vlen);
        if (encoding.byteOrder == dicomInput.encoding.byteOrder || vr.type.toggleEndian() == null)
            dicomInput.cache().writeBytesTo(DicomObject2.header2valuePosition(header), vlen, this);
        else {
            byte[] swapBuffer = this.swapBuffer;
            if (swapBuffer == null) {
                this.swapBuffer = swapBuffer = new byte[BUFFER_SIZE];
            }
            dicomInput.cache().writeSwappedBytesTo(DicomObject2.header2valuePosition(header), vlen, this,
                    vr.type.toggleEndian(), swapBuffer);
        }
    }
}
