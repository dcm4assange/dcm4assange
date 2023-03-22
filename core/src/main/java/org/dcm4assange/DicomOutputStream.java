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

import org.dcm4assange.util.StringUtils;
import org.dcm4assange.util.ToggleEndian;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Sep 2021
 */
public class DicomOutputStream extends OutputStream  {
    private static final int BUFFER_SIZE = 8192;
    private static final int BULKDATA_VR_BIT = 0x80;
    private OutputStream out;
    private DicomEncoding encoding;
    private boolean includeGroupLength;
    private boolean undefSequenceLength;
    private boolean undefItemLength;
    private IncludeBulkData includeBulkData = IncludeBulkData.YES;
    private byte[] b12 = new byte[12];
    private byte[] buffer;

    public enum IncludeBulkData {
        YES, EXCLUDE_ATTRIBUTE, NULLIFY_VALUE, BULK_DATA_URI
    }

    public DicomOutputStream(OutputStream out) {
        this.out = Objects.requireNonNull(out);
    }

    public DicomEncoding encoding() {
        return encoding;
    }

    public DicomOutputStream withEncoding(DicomEncoding encoding) {
        if (!Objects.requireNonNull(encoding).explicitVR && includeBulkData == IncludeBulkData.BULK_DATA_URI)
            throw new IllegalStateException("BULK_DATA_URI not supported with IVR_LE encoding");
        this.encoding = encoding;
        if (encoding.deflated) {
            out = new DeflaterOutputStream(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
        }
        return this;
    }

    public DicomOutputStream withEncoding(DicomObject fmi) {
        String tsuid = fmi.getString(Tag.TransferSyntaxUID).orElseThrow(
                () -> new IllegalArgumentException("Missing Transfer Syntax UID in File Meta Information"));

        return withEncoding(DicomEncoding.of(tsuid));
    }

    public DicomOutputStream withIncludeGroupLength(boolean includeGroupLength) {
        this.includeGroupLength = includeGroupLength;
        return this;
    }

    public boolean includeGroupLength() {
        return includeGroupLength;
    }

    public boolean undefSequenceLength() {
        return undefSequenceLength;
    }

    public DicomOutputStream withUndefSequenceLength(boolean undefSequenceLength) {
        this.undefSequenceLength = undefSequenceLength;
        return this;
    }

    public boolean undefItemLength() {
        return undefItemLength;
    }

    public DicomOutputStream withUndefItemLength(boolean undefItemLength) {
        this.undefItemLength = undefItemLength;
        return this;
    }

    public IncludeBulkData includeBulkData() {
        return includeBulkData;
    }

    public DicomOutputStream withIncludeBulkData(IncludeBulkData includeBulkData) {
        if (Objects.requireNonNull(includeBulkData) == IncludeBulkData.BULK_DATA_URI && !encoding.explicitVR)
            throw new IllegalStateException("BULK_DATA_URI not supported with IVR_LE encoding");
        this.includeBulkData = includeBulkData;
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

    public DicomOutputStream writeFileMetaInformation(DicomObject fmi) throws IOException {
        ensureEncoding(DicomEncoding.EVR_LE);
        if (!fmi.containsValue(Tag.TransferSyntaxUID))
            throw new IllegalArgumentException("missing Transfer Syntax UID");

        byte[] b = new byte[132];
        b[128] = 'D';
        b[129] = 'I';
        b[130] = 'C';
        b[131] = 'M';
        write(b);
        write(fmi, true);
        return withEncoding(fmi);
    }

    public void writeDataSet(DicomObject dcmobj) throws IOException {
        if (encoding == null)
            throw new IllegalStateException("encoding not initialized");

        write(dcmobj, includeGroupLength);
    }

    public void writeCommandSet(DicomObject dcmobj) throws IOException {
        ensureEncoding(DicomEncoding.IVR_LE);
        if (dcmobj.isEmpty())
            throw new IllegalArgumentException("empty command set");

        write(dcmobj, true);
    }

    private void ensureEncoding(DicomEncoding encoding) {
        if (this.encoding == null)
            this.encoding = encoding;
        else if (this.encoding != encoding)
            throw new IllegalStateException("invalid encoding: " + encoding);
    }

    private void write(DicomObject dcmobj, boolean includeGroupLength) throws IOException {
        DicomObject tmp = new DicomObject(dcmobj);
        if (includeGroupLength) {
            tmp.calculateLengthWithGroupLength(this);
        } else {
            tmp.calculateLength(this);
        }
        tmp.writeTo(this, includeGroupLength);
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

    void write(DicomObject.Sequence seq, boolean includeGroupLength) throws IOException {
        writeHeader(seq.tag, VR.SQ, undefSequenceLength ? -1 : valueLength(seq));
        for (int i = 0, n = seq.size(); i < n; i++) {
            DicomObject item = seq.getItem(i);
            writeHeader(Tag.Item, null, undefItemLength ? -1 : item.length);
            item.writeTo(this, includeGroupLength);
            if (undefItemLength)
                writeHeader(Tag.ItemDelimitationItem, null, 0);
        }
        if (undefSequenceLength)
            writeHeader(Tag.SequenceDelimitationItem, null, 0);
    }

    private int valueLength(DicomObject.Sequence seq) {
        int length = (undefItemLength ? 16 : 8) * seq.size();
        for (int i = 0; i < seq.size(); i++) {
            length += seq.getItem(i).length;
        }
        return length;
    }

    void write(int tag, VR vr, byte[] b) throws IOException {
        writeHeader(tag, vr, (b.length + 1) & ~1);
        if (encoding.byteOrder == ByteOrder.LITTLE_ENDIAN || vr.type.toggleEndian() == null)
            write(b);
        else
            writeSwappedBytes(b, vr.type.toggleEndian(), buffer());
        if ((b.length & 1) != 0)
            write(vr.paddingByte);
    }

    private void writeSwappedBytes(byte[] b, ToggleEndian toggleEndian, byte[] buf) throws IOException {
        int remaining = b.length;
        int copy = 0;
        int pos = 0;
        while ((remaining -= copy) > 0) {
            pos += copy;
            copy = Math.min(remaining, buf.length);
            System.arraycopy(b, pos, buf, 0, copy);
            toggleEndian.apply(buf, copy);
            write(buf, 0, copy);
        }
    }

    void write(long header, MemoryCache.DicomInput dicomInput) throws IOException {
        int tag = dicomInput.tagAt(header & DicomInputStream.POSITION_HEADER_MASK);
        VR vr = VR.fromHeader(header);
        int vlen = dicomInput.header2valueLength(header);
        writeHeader(tag, vr, vlen);
        if (encoding.byteOrder == dicomInput.encoding.byteOrder || vr.type.toggleEndian() == null)
            dicomInput.cache().writeBytesTo(DicomObject.header2valuePosition(header), vlen, this);
        else {
            dicomInput.cache().writeSwappedBytesTo(DicomObject.header2valuePosition(header), vlen, this,
                    vr.type.toggleEndian(), buffer());
        }
    }

    private byte[] buffer() {
        byte[] buffer = this.buffer;
        if (buffer == null) {
            this.buffer = buffer = new byte[BUFFER_SIZE];
        }
        return buffer;
    }

    void write(int tag, VR vr, String bulkDataURI) throws IOException {
        switch (includeBulkData) {
            case YES -> writeBulkData(tag, vr, bulkDataURI);
            case NULLIFY_VALUE -> writeHeader(tag, vr, 0);
            case BULK_DATA_URI -> writeBulkDataURI(tag, vr, bulkDataURI);
        }
    }

    private void writeBulkData(int tag, VR vr, String bulkDataURI) throws IOException {
        URI uri = URI.create(bulkDataURI);
        Path path = Paths.get(uri.getPath());
        long offsetAndLength = offsetAndLength(uri.getFragment());
        int offset = (int) (offsetAndLength >>> 32);
        int length = (int) offsetAndLength;
        writeHeader(tag, vr, length);
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = buffer();
            skip(in, offset, buffer);
            copy(in, length, buffer);
        }
    }

    private void writeBulkDataURI(int tag, VR vr, String bulkDataURI) throws IOException {
        encoding.byteOrder.tagToBytes(tag, b12, 0);
        byte[] b = bulkDataURI.getBytes(StandardCharsets.UTF_8);
        int padding = b.length & 1;
        b12[4] = (byte) ((vr.code >>> 8) | BULKDATA_VR_BIT);
        b12[5] = (byte) vr.code;
        encoding.byteOrder.shortToBytes(b.length + padding, b12, 6);
        write(b12, 0, 8);
        write(b, 0, b.length);
        if (padding != 0) write(' ');
    }

    private static long offsetAndLength(String fragment) {
        if (fragment == null) return 0L;
        long result = 0;
        for (String param : StringUtils.split(fragment, ',')) {
            String[] keyValue = StringUtils.split(param, '=');
            switch (keyValue[0]) {
                case "offset":
                    result |= Long.parseLong(keyValue[1]) << 32;
                    break;
                case "length":
                    result |= Integer.parseInt(keyValue[1]) & 0xffffffffL ;
                    break;
            }
        }
        return result;
    }

    private static void skip(InputStream in, int length, byte[] buffer) throws IOException {
        int read;
        while (length > 0 && (read = in.read(buffer, 0, Math.min(length, BUFFER_SIZE))) > 0) {
            length -= read;
        }
    }

    private void copy(InputStream in, int length, byte[] buffer) throws IOException {
        int read = 0;
        if (length == -1) {
            int off = 0;
            int len = 0;
            do {
                len += 8;
                do {
                    if ((off = Math.max(8 - len, 0)) > 0) {
                        System.arraycopy(buffer, read - off, buffer, 0, off);
                    }
                    read = in.read(buffer, off, Math.min(len, BUFFER_SIZE - off));
                    if (read <= 0) throw new EOFException();
                    write(buffer, off, read);
                } while ((len -= read) > 0);
                len = ByteOrder.LITTLE_ENDIAN.bytesToInt(buffer, off + read - 4);
            } while (ByteOrder.LITTLE_ENDIAN.bytesToTag(buffer, off + read - 8) == Tag.Item);
        } else {
            do {
                read = in.read(buffer, 0, Math.min(length, BUFFER_SIZE));
                if (read <= 0) throw new EOFException();
                write(buffer, 0, read);
            } while ((length -= read) > 0);
        }
    }
}
