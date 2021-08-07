/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dcm4assange;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jun 2021
 */
public class DicomOutputStreamTest {

    private static final byte[] SEQ_IVR_LE_GROUP_LENGTH = {
            0, 82, 0, 0, 4, 0, 0, 0, 124, 0, 0, 0,
            0, 82, 48, -110, 116, 0, 0, 0,
            -2, -1, 0, -32, -1, -1, -1, -1,
            24, 0, 0, 0, 4, 0, 0, 0, 52, 0, 0, 0,
            24, 0, 20, -111, 44, 0, 0, 0,
            -2, -1, 0, -32, -1, -1, -1, -1,
            24, 0, 0, 0, 4, 0, 0, 0, 16, 0, 0, 0,
            24, 0, -126, -112, 8, 0, 0, 0, -1, -1, -1, -1, 102, 102, -10, 63,
            -2, -1, 13, -32, 0, 0, 0, 0,
            32, 0, 0, 0, 4, 0, 0, 0, 8, 0, 0, 0,
            32, 0, 19, -111, 0, 0, 0, 0,
            -2, -1, 13, -32, 0, 0, 0, 0,
            -2, -1, 0, -32, -1, -1, -1, -1,
            -2, -1, 13, -32, 0, 0, 0, 0
    };

    @Test
    public void writeDataSetIVR_LE() throws IOException {
        assertArrayEquals(DicomInputStreamTest.IVR_LE, writeDataSet(DicomEncoding.IVR_LE, false,
                DicomOutputStream.LengthEncoding.UNDEFINED_OR_ZERO,
                DicomOutputStream.LengthEncoding.UNDEFINED_OR_ZERO,
                readDataset(DicomInputStreamTest.IVR_LE, DicomEncoding.IVR_LE)));
    }

    @Test
    public void writeDataSetEVR_LE() throws IOException {
        assertArrayEquals(DicomInputStreamTest.EVR_LE, writeDataSet(DicomEncoding.EVR_LE, false,
                DicomOutputStream.LengthEncoding.UNDEFINED_OR_ZERO,
                DicomOutputStream.LengthEncoding.UNDEFINED_OR_ZERO,
                readDataset(DicomInputStreamTest.EVR_BE, DicomEncoding.EVR_BE)));
    }

    @Test
    public void writeDataSetEVR_BE() throws IOException {
        assertArrayEquals(DicomInputStreamTest.EVR_BE, writeDataSet(DicomEncoding.EVR_BE, false,
                DicomOutputStream.LengthEncoding.UNDEFINED_OR_ZERO,
                DicomOutputStream.LengthEncoding.UNDEFINED_OR_ZERO,
                readDataset(DicomInputStreamTest.EVR_LE, DicomEncoding.EVR_LE)));
    }

    @Test
    public void writeDataSetDEFL() throws IOException {
        byte[] DEFL_EVR_LE = DicomInputStreamTest.DEFL_EVR_LE();
        DicomObject fmi;
        DicomObject data;
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(DEFL_EVR_LE))) {
            fmi = dis.readFileMetaInformation();
            data = dis.readDataSet();
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (DicomOutputStream dos = new DicomOutputStream(bout)) {
            dos.writeFileMetaInformation(fmi);
            dos.writeDataSet(data);
        }
        assertArrayEquals(DEFL_EVR_LE, bout.toByteArray());
    }

    @Test
    public void writeSequenceEVR_LE() throws IOException {
        assertArrayEquals(DicomInputStreamTest.PER_FRAME_FUNCTIONAL_GROUPS_SEQ_EVR_LE,
                writeDataSet(DicomEncoding.EVR_LE, false,
                        DicomOutputStream.LengthEncoding.UNDEFINED_OR_ZERO,
                        DicomOutputStream.LengthEncoding.UNDEFINED_OR_ZERO,
                        readDataset(DicomInputStreamTest.PER_FRAME_FUNCTIONAL_GROUPS_SEQ_IVR_LE,
                                DicomEncoding.IVR_LE)));
    }

    @Test
    public void writeSequenceIVR_LE_ExplicitLength() throws IOException {
        assertArrayEquals(DicomInputStreamTest.PER_FRAME_FUNCTIONAL_GROUPS_SEQ_IVR_LE,
                writeDataSet(DicomEncoding.IVR_LE, false,
                        DicomOutputStream.LengthEncoding.EXPLICIT,
                        DicomOutputStream.LengthEncoding.EXPLICIT,
                        readDataset(DicomInputStreamTest.PER_FRAME_FUNCTIONAL_GROUPS_SEQ_IVR_LE,
                                DicomEncoding.IVR_LE)));
    }

    @Test
    public void writeSequenceEVR_LE_GroupLength() throws IOException {
        assertArrayEquals(SEQ_IVR_LE_GROUP_LENGTH,
                writeDataSet(DicomEncoding.IVR_LE, true,
                        DicomOutputStream.LengthEncoding.EXPLICIT,
                        DicomOutputStream.LengthEncoding.UNDEFINED,
                        readDataset(DicomInputStreamTest.PER_FRAME_FUNCTIONAL_GROUPS_SEQ_IVR_LE,
                                DicomEncoding.IVR_LE)));
    }

    @Test
    public void writeParsedCommandSet() throws IOException {
        writeCommandSet(DicomInputStreamTest.c_echo_rq());
    }

    @Test
    public void writeCommandSet() throws IOException {
        writeCommandSet(c_echo_rq());
    }

    @Test
    public void writeBulkDataIVR_LE() throws IOException {
        writeBulkData(DicomFileStream.ivrPxData(0x40000L));
    }

    @Test
    public void writeBulkDataEncapsulated() throws IOException {
        writeBulkData(DicomFileStream.encapsPxData(0x40000L));
    }

    private static void writeCommandSet(DicomObject cmd) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (DicomOutputStream dos = new DicomOutputStream(bout)) {
            dos.writeCommandSet(cmd);
        }
        assertArrayEquals(DicomInputStreamTest.C_ECHO_RQ, bout.toByteArray());
    }

    private static DicomObject readDataset(byte[] b, DicomEncoding encoding) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(b))) {
            return dis.withEncoding(encoding).readDataSet();
        }
    }

    private static byte[] writeDataSet(DicomEncoding encoding, boolean groupLength,
                                       DicomOutputStream.LengthEncoding seqEncoding,
                                       DicomOutputStream.LengthEncoding itemEncoding,
                                       DicomObject dcmObj) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (DicomOutputStream dos = new DicomOutputStream(bout)) {
            dos.withEncoding(encoding)
                    .withIncludeGroupLength(groupLength)
                    .withSequenceLengthEncoding(seqEncoding)
                    .withItemLengthEncoding(itemEncoding)
                    .writeDataSet(dcmObj);
        }
        return bout.toByteArray();
    }

    static DicomObject c_echo_rq() throws IOException {
        DicomObject cmd = DicomObject.newDicomObject();
        cmd.setString(Tag.AffectedSOPClassUID, VR.UI, UID.Verification);
        cmd.setInt(Tag.CommandField, VR.US, 48);
        cmd.setInt(Tag.MessageID, VR.US, 1);
        cmd.setInt(Tag.CommandDataSetType, VR.US, 0x0101);
        return cmd;
    }

    static void writeBulkData(DicomFileStream dfs) throws IOException {
        DicomObject fmi;
        DicomObject data;
        Path file = Files.createTempFile("in", ".dcm");
        Path out = Files.createTempFile("out", ".dcm");
        try {
            Files.copy(dfs, file, StandardCopyOption.REPLACE_EXISTING);
            try (DicomInputStream dis = new DicomInputStream(file, DicomInputStream::bulkDataPredicate)) {
                fmi = dis.readFileMetaInformation();
                data = dis.readDataSet();
            }
            try (DicomOutputStream dos = new DicomOutputStream(Files.newOutputStream(out))) {
                dos.writeFileMetaInformation(fmi);
                dos.writeDataSet(data);
            }
            assertEquals(Files.size(file), Files.size(out));
        } finally {
            Files.delete(file);
            Files.delete(out);
        }
    }
}
