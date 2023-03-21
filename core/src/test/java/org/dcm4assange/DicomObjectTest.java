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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Oct 2021
 */
public class DicomObjectTest {

    private static final String BULK_DATA_URI = "https://host/bulkdata";
    private static final byte[] TEXT = { 'T', 'E', 'X', 'T' };

    @Test
    public void serialize() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(DicomInputStreamTest.c_echo_rq());
            oos.writeObject(DicomInputStreamTest.readDataset(DicomInputStreamTest.EVR_BE, DicomEncoding.EVR_BE));
            oos.writeObject(bulkData());
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            DicomInputStreamTest.assert_c_echo_rq((DicomObject) ois.readObject());
            DicomInputStreamTest.assertDataSet((DicomObject) ois.readObject());
            assertBulkData((DicomObject) ois.readObject());
        }
    }


    @Test
    public void setAndGet() {
        DicomInputStreamTest.assertDataSet(createDataset());
    }

    private Object bulkData() {
        DicomObject item = new DicomObject();
        item.setBulkDataURI(Tag.WaveformData, VR.OB, BULK_DATA_URI);
        DicomObject dataset = new DicomObject();
        dataset.newSequence(Tag.WaveformSequence).add(item);
        return dataset;
    }

    private void assertBulkData(DicomObject dataset) {
        assertEquals(BULK_DATA_URI, dataset.getItem(Tag.WaveformSequence).orElseGet(Assertions::fail)
                .getBulkDataURI(Tag.WaveformData).orElseGet(Assertions::fail));
    }

    static DicomObject createDataset() {
        DicomObject dcmObj = new DicomObject();
        dcmObj.setString(Tag.SelectorAEValue, VR.AE, "TEXT");
        dcmObj.setString(Tag.SelectorASValue, VR.AS, "099Y");
        dcmObj.setInt(Tag.SelectorATValue, VR.AT, Tag.SelectorATValue);
        dcmObj.setString(Tag.SelectorDAValue, VR.DA, "20210403");
        dcmObj.setString(Tag.SelectorCSValue, VR.CS, "TEXT");
        dcmObj.setString(Tag.SelectorDTValue, VR.DT, "202104032321");
        dcmObj.setInt(Tag.SelectorISValue, VR.IS, 1);
        dcmObj.setInt(Tag.SelectorOBValue, VR.OB, -1);
        dcmObj.setString(Tag.SelectorLOValue, VR.LO, "TEXT");
        dcmObj.setFloat(Tag.SelectorOFValue, VR.OF, 1.f);
        dcmObj.setString(Tag.SelectorLTValue, VR.LT, "TEXT");
        dcmObj.setInt(Tag.SelectorOWValue, VR.OW, -1);
        dcmObj.setString(Tag.SelectorPNValue, VR.PN, "TEXT");
        dcmObj.setString(Tag.SelectorTMValue, VR.TM, "2321");
        dcmObj.setString(Tag.SelectorSHValue, VR.SH, "TEXT");
        dcmObj.setBytes(Tag.SelectorUNValue, VR.UN, TEXT);
        dcmObj.setString(Tag.SelectorSTValue, VR.ST, "TEXT");
        dcmObj.setString(Tag.SelectorUCValue, VR.UC, "TEXT");
        dcmObj.setString(Tag.SelectorUTValue, VR.UT, "TEXT");
        dcmObj.setString(Tag.SelectorURValue, VR.UR, "TEXT");
        dcmObj.setInt(Tag.SelectorDSValue, VR.DS, 1);
        dcmObj.setDouble(Tag.SelectorODValue, VR.OD, 1.);
        dcmObj.setDouble(Tag.SelectorFDValue, VR.FD, 1.);
        dcmObj.setLong(Tag.SelectorOLValue, VR.OL, -1L);
        dcmObj.setFloat(Tag.SelectorFLValue, VR.FL, 1.f);
        dcmObj.setLong(Tag.SelectorULValue, VR.UL, -1L);
        dcmObj.setInt(Tag.SelectorUSValue, VR.US, -1);
        dcmObj.setLong(Tag.SelectorSLValue, VR.SL, -1L);
        dcmObj.setInt(Tag.SelectorSSValue, VR.SS, -1);
        dcmObj.setString(Tag.SelectorUIValue, VR.UI, UID.Verification);
        dcmObj.newSequence(Tag.SelectorCodeSequenceValue).add(new DicomObject());
        dcmObj.setLong(Tag.SelectorOVValue, VR.OV, -1L);
        dcmObj.setLong(Tag.SelectorSVValue, VR.SV, -1L);
        dcmObj.setLong(Tag.SelectorUVValue, VR.UV, -1L);
        return dcmObj;
    }

}
