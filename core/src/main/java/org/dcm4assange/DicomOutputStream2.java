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
import java.util.function.IntPredicate;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Sep 2021
 */
public class DicomOutputStream2 extends OutputStream  {
    private OutputStream out;
    private DicomEncoding encoding;
    private boolean includeGroupLength;
    private boolean undefSequenceLength;
    private boolean undefItemLength;

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
        if (encoding != null)
            throw new IllegalStateException("encoding already initialized: " + encoding);

        if (!fmi.containsValue(Tag.TransferSyntaxUID))
            throw new IllegalArgumentException("missing Transfer Syntax UID");


        byte[] b = new byte[132];
        b[128] = 'D';
        b[129] = 'I';
        b[130] = 'C';
        b[131] = 'M';
        write(b);
        new DicomObject2(fmi)
                .createEncoder(DicomEncoding.EVR_LE, true, undefSequenceLength, undefItemLength)
                .writeTo(this);
        return withEncoding(fmi);
    }

    public void writeDataSet(DicomObject2 dcmobj) throws IOException {
        if (encoding == null)
            throw new IllegalStateException("encoding not initialized");

        new DicomObject2(dcmobj)
                .createEncoder(encoding, includeGroupLength, undefSequenceLength, undefItemLength)
                .writeTo(this);
    }

    public void writeCommandSet(DicomObject2 dcmobj) throws IOException {
        if (encoding != null)
            throw new IllegalStateException("encoding already initialized: " + encoding);

        if (dcmobj.isEmpty())
            throw new IllegalArgumentException("empty command set");

        new DicomObject2(dcmobj)
                .createEncoder(DicomEncoding.IVR_LE, true, undefSequenceLength, undefItemLength)
                .writeTo(this);
    }
}
