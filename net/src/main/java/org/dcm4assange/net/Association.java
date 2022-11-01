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

import org.dcm4assange.ByteOrder;
import org.dcm4assange.DicomInputStream;
import org.dcm4assange.DicomObject;
import org.dcm4assange.DicomOutputStream;
import org.dcm4assange.conf.model.ApplicationEntity;
import org.dcm4assange.conf.model.Connection;
import org.dcm4assange.conf.model.TransferCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2021
 */
public class Association implements Runnable {
    static final Logger LOG = LoggerFactory.getLogger(Association.class);
    private static final AtomicInteger prevSerialNo = new AtomicInteger();
    private static final int DATA = 0;
    private static final int COMMAND = 1;
    private static final int LAST = 2;
    private static final int LAST_COMMAND = 3;
    private final int serialNo;
    private final DeviceRuntime deviceRuntime;
    private final Role role;
    private final Connection conn;
    private final Socket sock;
    private final InputStream in;
    private final OutputStream out;
    private final ReentrantLock writeLock = new ReentrantLock();
    final CompletableFuture<Association> aaacReceived = new CompletableFuture<>();
    final CompletableFuture<Association> arrpReceived = new CompletableFuture<>();
    private volatile String name;
    private volatile ApplicationEntity ae;
    private volatile AAssociate.RQ aarq;
    private volatile AAssociate.AC aaac;
    private volatile State state;
    private int pduRemaining;
    private int pdvRemaining;
    private Byte pcid;
    private int pdvmch = COMMAND;
    private byte[] pDataTF;
    private int pDataTFLength;

    private Association(DeviceRuntime deviceRuntime, Connection conn, Socket sock, Role role, State state)
            throws IOException {
        this.serialNo = prevSerialNo.incrementAndGet();
        this.name = "" + sock.getLocalSocketAddress()
                + role.delim + sock.getRemoteSocketAddress()
                + '(' + serialNo + ')';
        this.deviceRuntime = deviceRuntime;
        this.conn = conn;
        this.sock = sock;
        this.in = new BufferedInputStream(sock.getInputStream());
        this.out = new BufferedOutputStream(sock.getOutputStream());
        this.role = role;
        this.state = state;
    }

    @Override
    public String toString() {
        return name;
    }

    AAssociate.CommonExtendedNegotation commonExtendedNegotationFor(String cuid) {
        return aarq.getCommonExtendedNegotation(cuid);
    }

    static Association open(DeviceRuntime deviceRuntime, ApplicationEntity ae, Connection conn, Socket sock,
                            AAssociate.RQ rq) throws IOException {
        Association as = new Association(deviceRuntime, conn, sock, Role.REQUESTOR, State.EXPECT_AA_AC);
        as.ae = ae;
        as.write(rq);
        return as;
    }

    static Association accept(DeviceRuntime deviceRuntime, Connection conn, Socket sock) throws IOException {
        return new Association(deviceRuntime, conn, sock, Role.ACCEPTOR, State.EXPECT_AA_RQ);
    }

    public String getTransferSyntax(Byte pcid) {
        return aaac.getPresentationContext(pcid).transferSyntax;
    }

    @Override
    public void run() {
        try {
            while (!sock.isInputShutdown() && !sock.isClosed()) {
                nextPDU();
            }
        } catch (IOException ex) {
            state.onIOException(this, ex);
        }
    }

    private int nextPDU() throws IOException {
        LOG.trace("{}: waiting for PDU", name);
        int pduType = Utils.readUnsignedByte(in);
        Utils.skipByte(in);
        int pduLength = Utils.readInt(in);
        LOG.trace("{} >> PDU[type={}, len={}]", name, pduType, pduLength & 0xFFFFFFFFL);
        switch (pduType) {
            case 0x01 -> state.onAAssociateRQ(this, pduType, pduLength);
            case 0x02 -> state.onAAssociateAC(this, pduType, pduLength);
            case 0x03 -> state.onAAssociateRJ(this, pduType, pduLength);
            case 0x04 -> state.onPDataTF(this, pduType, pduLength);
            case 0x05 -> state.onAReleaseRQ(this, pduType, pduLength);
            case 0x06 -> state.onAReleaseRP(this, pduType, pduLength);
            case 0x07 -> state.onAAbort(this, pduType, pduLength);
            default -> state.onUnrecognizedPDU(this, pduType, pduLength);
        }
        return pduType;
    }

    public CompletableFuture<Association> release(long timeout, TimeUnit unit)
            throws IOException, InterruptedException, TimeoutException {
        state.release(this, timeout, unit);
        return arrpReceived;
    }

    void writeDimse(Byte pcid, Dimse dimse, DicomObject cmd) throws IOException {
        writeLock.lock();
        try {
            pDataTFLength = 4;
            pdvOutputStream.write(pcid);
            pdvOutputStream.write(LAST_COMMAND);
            new DicomOutputStream(pdvOutputStream).writeCommandSet(cmd);
            ByteOrder.BIG_ENDIAN.intToBytes(pDataTFLength - 4, pDataTF, 0);
            out.write(0x04);
            out.write(0);
            Utils.writeInt(out, pDataTFLength);
            out.write(pDataTF, 0, pDataTFLength);
            out.flush();
        } finally {
            writeLock.unlock();
        }
    }

    public enum Role {
        ACCEPTOR("<-"),
        REQUESTOR("->");

        private final String delim;

        Role(String delim) {
            this.delim = delim;
        }
    }

    private enum State {
        EXPECT_AA_RQ {
            @Override
            public void onAAssociateRQ(Association as, int pduType, int pduLength) throws IOException {
                as.onAAssociateRQ(pduLength);
            }
        },
        EXPECT_AA_AC {
            @Override
            public void onAAssociateAC(Association as, int pduType, int pduLength) throws IOException {
                as.onAAssociateAC(pduLength);
            }

            @Override
            public void onAAssociateRJ(Association as, int pduType, int pduLength) throws IOException {
                as.onAAssociateRJ(pduLength);
            }
        },
        EXPECT_PDATA_TF {
            @Override
            public void onPDataTF(Association as, int pduType, int pduLength) throws IOException {
                as.onPDataTF(pduLength);
            }

            @Override
            public void onAReleaseRQ(Association as, int pduType, int pduLength) throws IOException {
                as.onAReleaseRQ(pduLength);
            }
        },
        EXPECT_AR_RP {
            @Override
            public void onPDataTF(Association as, int pduType, int pduLength) throws IOException {
                as.onPDataTF(pduLength);
            }

            @Override
            public void onAReleaseRQ(Association as, int pduType, int pduLength) throws IOException {
                as.onAReleaseRQ(pduLength);
            }

            @Override
            public void onAReleaseRP(Association as, int pduType, int pduLength) throws IOException {
                as.onAReleaseRP(pduLength);
            }
        };

        public void onAAssociateRQ(Association as, int pduType, int pduLength) throws IOException {
            as.onUnexpectedPDU(pduType, pduLength);
        }

        public void onAAssociateAC(Association as, int pduType, int pduLength) throws IOException {
            as.onUnexpectedPDU(pduType, pduLength);
        }

        public void onAAssociateRJ(Association as, int pduType, int pduLength) throws IOException {
            as.onUnexpectedPDU(pduType, pduLength);
        }

        public void onPDataTF(Association as, int pduType, int pduLength) throws IOException {
            as.onUnexpectedPDU(pduType, pduLength);
        }

        public void onAReleaseRQ(Association as, int pduType, int pduLength) throws IOException {
            as.onUnexpectedPDU(pduType, pduLength);
        }

        public void onAReleaseRP(Association as, int pduType, int pduLength) throws IOException {
            as.onUnexpectedPDU(pduType, pduLength);
        }

        public void onAAbort(Association as, int pduType, int pduLength) throws IOException {
            as.onAAbort(pduLength);
        }

        public void onUnrecognizedPDU(Association as, int pduType, int pduLength) {
            as.onUnrecognizedPDU(pduType, pduLength);
        }

        public void onIOException(Association as, IOException ex) {
            as.onIOException(ex);
        }

        public void release(Association as, long timeout, TimeUnit unit) {
            throw new IllegalStateException("State: " + this);
        }
    }

    private void onAAssociateRQ(int pduLength) throws IOException {
        try {
            negotiate(AAssociate.RQ.readFrom(in, pduLength));
            write(aaac);
            state = State.EXPECT_PDATA_TF;
        } catch (AAssociateRJ aarj) {
            aarj.writeTo(out);
            sock.shutdownInput();
            deviceRuntime.closeSocketDelayed(conn, sock);
        }
    }

    private void negotiate(AAssociate.RQ aarq) throws AAssociateRJ {
        this.aarq = aarq;
        if ((aarq.getProtocolVersion() & 1) == 0)
            throw AAssociateRJ.protocolVersionNotSupported();

        if (!deviceRuntime.device.isSupportedApplicationContextNames(aarq.getApplicationContextName()))
            throw AAssociateRJ.applicationContextNameNotSupported();

        ae = deviceRuntime.device.getApplicationEntity(aarq.getCalledAETitle())
                .or(deviceRuntime.device::getDefaultApplicationEntity)
                .orElseThrow(AAssociateRJ::calledAETitleNotRecognized);
        if (!ae.isInstalled() || !ae.isAssociationAcceptor())
            throw AAssociateRJ.calledAETitleNotRecognized();

        if (!ae.isAcceptedCallingAET(aarq.getCallingAETitle()))
            throw AAssociateRJ.callingAETitleNotRecognized();

        aaac = new AAssociate.AC(aarq.getCallingAETitle(), aarq.getCalledAETitle());
        aaac.setMaxPDULength(conn.getReceivePDULength());
        pDataTF = new byte[minZeroAsMax(aarq.getMaxPDULength(), conn.getSendPDULength())];
        if (aarq.hasAsyncOpsWindow())
            aaac.setAsyncOpsWindow(
                    minZeroAsMax(aarq.getMaxOpsInvoked(), conn.getMaxOpsPerformed()),
                    minZeroAsMax(aarq.getMaxOpsPerformed(), conn.getMaxOpsInvoked()));
        AAssociate.UserIdentity userIdentity = aarq.getUserIdentity();
        if (userIdentity != null) {
            byte[] response = deviceRuntime.negotiateUserIdentity.apply(this);
            if (userIdentity.positiveResponseRequested) {
                aaac.setUserIdentityServerResponse(response);
            }
        }
        aarq.forEachPresentationContext(this::negotiatePresentationContext);
    }

    private void negotiatePresentationContext(Byte pcid, AAssociate.RQ.PresentationContext pc) {
        Optional<TransferCapability> tc = getTransferCapability(pc.abstractSyntax());
        tc.ifPresentOrElse(
                tc1 -> tc1.selectTransferSyntax(pc.transferSyntaxes).ifPresentOrElse(
                        ts -> aaac.putPresentationContext(pcid,
                                AAssociate.AC.Result.ACCEPTANCE, ts),
                        () -> aaac.putPresentationContext(pcid,
                                AAssociate.AC.Result.TRANSFER_SYNTAXES_NOT_SUPPORTED, pc.transferSyntaxes[0])),
                () -> aaac.putPresentationContext(pcid,
                        AAssociate.AC.Result.ABSTRACT_SYNTAX_NOT_SUPPORTED, pc.transferSyntaxes[0]));
     }

    private Optional<TransferCapability> getTransferCapability(String abstractSyntax) {
        AAssociate.RoleSelection roleSelection = aarq.getRoleSelection(abstractSyntax);
        if (roleSelection == null) {
            return ae.getTransferCapabilityOrDefault(TransferCapability.Role.SCP, abstractSyntax);
        }
        Optional<TransferCapability> scuTC = roleSelection.scu
                ? ae.getTransferCapabilityOrDefault(TransferCapability.Role.SCU, abstractSyntax)
                : Optional.empty();
        Optional<TransferCapability> scpTC = roleSelection.scp
                ? ae.getTransferCapabilityOrDefault(TransferCapability.Role.SCP, abstractSyntax)
                : Optional.empty();
        aaac.putRoleSelection(abstractSyntax,
                roleSelection = AAssociate.RoleSelection.of(scuTC.isPresent(), scpTC.isPresent()));
        return roleSelection.scp ? scpTC : scuTC;
    }

    static int minZeroAsMax(int i1, int i2) {
        return i1 == 0 ? i2 : i2 == 0 ? i1 : Math.min(i1, i2);
    }

    private void onAAssociateAC(int pduLength) throws IOException {
        this.aaac = AAssociate.AC.readFrom(in, pduLength);
    }

    private void onAAssociateRJ(int pduLength) throws IOException {
        AAssociateRJ.readFrom(in, pduLength);
    }

    private void onAAbort(int pduLength) throws IOException {
        AAbort.readFrom(in, pduLength);
        deviceRuntime.closeSocket(conn, sock);
    }

    private void onAReleaseRQ(int pduLength) throws IOException {
        if (pduLength != 4)
            throw AAbort.invalidPDUParameterValue();
        Utils.readInt(in);
        sock.shutdownInput();
        writeAReleaseRP();
        deviceRuntime.closeSocketDelayed(conn, sock);
    }

    private void onAReleaseRP(int pduLength) throws IOException {
        if (pduLength != 4)
            throw AAbort.invalidPDUParameterValue();
        Utils.readInt(in);
        deviceRuntime.closeSocket(conn, sock);
    }

    private void onIOException(IOException ex) {
    }

    private void onUnrecognizedPDU(int pduType, int pduLength) {

    }

    private void onUnexpectedPDU(int pduType, int pduLength) {

    }

    private void write(AAssociate rqac) throws IOException {
        writeLock.lock();
        try {
            rqac.writeTo(out);
        } finally {
            writeLock.unlock();
        }
    }

    private void writeAReleaseRQ() throws IOException {
        blockingWrite(0x05, 0, 0, 0);
    }

    private void writeAReleaseRP() throws IOException {
        blockingWrite(0x06, 0, 0, 0);
    }

    private boolean tryWrite(int pdutype, int result, int source, int reason, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {
        if (!writeLock.tryLock(timeout, unit)) return false;
        try {
            write(pdutype, result, source, reason);
        } finally {
            writeLock.unlock();
        }
        return true;
    }

    private void blockingWrite(int pdutype, int result, int source, int reason) throws IOException {
        writeLock.lock();
        try {
            write(pdutype, result, source, reason);
        } finally {
            writeLock.unlock();
        }
    }

    private void write(int pdutype, int result, int source, int reason) throws IOException {
        byte[] b = {
                (byte) pdutype,
                0,
                0, 0, 0, 4, // pdulen
                0,
                (byte) result,
                (byte) source,
                (byte) reason
        };
        out.write(b);
        out.flush();
    }

    private void onPDataTF(int pduLength) throws IOException {
        if ((pduRemaining = pduLength - 4) < 0)
            throw AAbort.invalidPDUParameterValue();

        onNextPDV();
    }

    private void onNextPDV() throws IOException {
        if ((pdvRemaining = Utils.readInt(in) - 2) < 0)
            throw AAbort.invalidPDUParameterValue();
        if ((pduRemaining -= 2 + pdvRemaining) < 0)
            throw AAbort.invalidPDUParameterValue();

        int pcid = Utils.readUnsignedByte(in);
        int pdvmch = Utils.readUnsignedByte(in);
        if ((pdvmch & COMMAND) != (this.pdvmch & COMMAND))
            throw AAbort.userInitiated();
        this.pdvmch = pdvmch;
        if (this.pcid == null) {
            this.pcid = (byte) pcid;
            AAssociate.AC.PresentationContext pc = aaac.getPresentationContext(this.pcid);
            if (pc == null)
                throw AAbort.userInitiated();
            if (pc.result != AAssociate.AC.Result.ACCEPTANCE)
                throw AAbort.userInitiated();
            DicomObject commandSet = new DicomInputStream(pdvInputStream).readCommandSet();
            Dimse dimse = Dimse.of(commandSet);
            this.pdvmch = DATA;
            dimse.handler.accept(this, this.pcid, dimse, commandSet, pdvInputStream);
            this.pdvmch = COMMAND;
            this.pcid = null;
        } else if (this.pcid.intValue() != pcid) {
            throw AAbort.userInitiated();
        }
    }

    void onDimseRQ(Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream) throws IOException {
        deviceRuntime.onDimseRQ(this, pcid, dimse, commandSet, dataStream);
    }

    void onCancelRQ(Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream) {
    }

    void onDimseRSP(Byte pcid, Dimse dimse, DicomObject commandSet, DicomObject dataSet) {
    }

    private final InputStream pdvInputStream = new InputStream() {
        @Override
        public int read() throws IOException {
            return readPDV();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return readPDV(b, off, len);
        }
    };
    private int readPDV() throws IOException {
        while (pdvRemaining == 0) {
            if ((pdvmch & LAST) != 0) return -1;
            if (pduRemaining == 0) {
                if (nextPDU() != 0x04)
                    throw new EOFException();
            } else
                onNextPDV();
        }
        pdvRemaining--;
        return Utils.readUnsignedByte(in);
    }

    private int readPDV(byte[] b, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        while (pdvRemaining == 0) {
            if ((pdvmch & 2) != 0) return -1;
            if (pduRemaining == 0) {
                if (nextPDU() != 0x04)
                    throw new EOFException();
            } else {
                onNextPDV();
            }
        }
        int n = in.read(b, off, Math.min(pdvRemaining, len));
        if (n == 0)
            throw new EOFException();
        pdvRemaining -= n;
        return n;
    }

    private final OutputStream pdvOutputStream = new OutputStream() {

        @Override
        public void write(int b) throws IOException {
            pDataTF[pDataTFLength++] = (byte) b;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            System.arraycopy(b, off, pDataTF, pDataTFLength, len);
            pDataTFLength += len;
        }
    };
}
