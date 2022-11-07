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

package org.dcm4assange.net;

import org.dcm4assange.UID;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2021
 */
public class AAssociateTest {

    @Test
    public void writeReadRQ() throws IOException {
        AAssociate.RQ rq = new AAssociate.RQ("CALLING_AET", "CALLED_AET");
        init(rq);
        rq.addPresentationContext(UID.StorageCommitmentPushModel, UID.ImplicitVRLittleEndian);
        rq.addPresentationContext(UID.StudyRootQueryRetrieveInformationModelFind, UID.ImplicitVRLittleEndian);
        rq.addPresentationContext(UID.ComprehensiveSRStorage,
                UID.ImplicitVRLittleEndian, UID.ExplicitVRLittleEndian);
        rq.putCommonExtendedNegotation(UID.ComprehensiveSRStorage, UID.Storage,
                UID.Comprehensive3DSRStorage, UID.ExtensibleSRStorage);
        rq.setUserIdentity(AAssociate.UserIdentity.USERNAME, true, new byte[] {'d', 'o', 'e'});
        byte[] bytes = write(rq);
        assertEquals(bytes.length, 6 + rq.pduLength());
        AAssociate.RQ rq2 = readRQ(bytes);
        assertEquals(rq.toString(), rq2.toString());
    }

    @Test
    public void writeReadAC() throws IOException {
        AAssociate.AC ac = new AAssociate.AC("CALLING_AET", "CALLED_AET");
        init(ac);
        ac.putPresentationContext((byte) 1, AAssociate.AC.Result.ACCEPTANCE, UID.ImplicitVRLittleEndian);
        ac.putPresentationContext((byte) 3, AAssociate.AC.Result.USER_REJECTION, UID.ImplicitVRLittleEndian);
        ac.putPresentationContext((byte) 5, AAssociate.AC.Result.ABSTRACT_SYNTAX_NOT_SUPPORTED,
                UID.ImplicitVRLittleEndian);
        ac.setUserIdentityServerResponse(new byte[]{0, 1});
        byte[] bytes = write(ac);
        assertEquals(bytes.length, 6 + ac.pduLength());
        AAssociate.AC ac2 = readAC(bytes);
        assertEquals(ac.toString(), ac2.toString());
    }

    private void init(AAssociate rqac) {
        rqac.setAsyncOpsWindow(0, 1);
        rqac.putRoleSelection(UID.StorageCommitmentPushModel, AAssociate.RoleSelection.SCP);
        rqac.putExtendedNegotation(UID.StudyRootQueryRetrieveInformationModelFind, new byte[]{0, 1});
    }

    private byte[] write(AAssociate rqac) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            rqac.writeTo(new DataOutputStream(out));
            return out.toByteArray();
        }
    }

    private AAssociate.RQ readRQ(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            assertEquals(0x01, in.readUnsignedByte());
            Utils.skipByte(in);
            int pduLength = in.readInt();
            return AAssociate.RQ.readFrom(in, pduLength);
        }
    }

    private AAssociate.AC readAC(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            assertEquals(0x02, in.readUnsignedByte());
            Utils.skipByte(in);
            int pduLength = in.readInt();
            return AAssociate.AC.readFrom(in, pduLength);
        }
    }
}