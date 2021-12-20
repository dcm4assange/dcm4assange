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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Oct 2021
 */
public class DicomObjectTest {

    private static final String BULK_DATA_URI = "https://host/bulkdata";

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
}
