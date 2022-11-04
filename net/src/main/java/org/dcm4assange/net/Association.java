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

import org.dcm4assange.*;
import org.dcm4assange.conf.model.ApplicationEntity;
import org.dcm4assange.conf.model.Connection;
import org.dcm4assange.conf.model.TransferCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
    private static final int LAST_DATA = 2;
    private static final int LAST_COMMAND = 3;
    private final int serialNo;
    private final DeviceRuntime deviceRuntime;
    private final Role role;
    private final Connection conn;
    private final Socket sock;
    private final InputStream in;
    private final OutputStream out;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final CompletableFuture<Association> aaacReceived = new CompletableFuture<>();
    private final CompletableFuture<Association> arrpReceived = new CompletableFuture<>();
    private final AtomicInteger messageID = new AtomicInteger();
    private volatile String name;
    private volatile ApplicationEntity ae;
    private volatile AAssociate.RQ aarq;
    private volatile AAssociate.AC aaac;
    private volatile BlockingQueue<OutstandingRSP> outstandingRSPs;
    private volatile State state;
    private int ipduRemaining;
    private int ipdvRemaining;
    private Byte ipcid;
    private int ipdvmch = COMMAND;
    private byte[] opdu;
    private int opduLen;
    private int opdvPos;
    private Byte opcid;
    private byte opdvmch;

    private Association(DeviceRuntime deviceRuntime, Connection conn, Socket sock, Role role)
            throws IOException {
        this.serialNo = prevSerialNo.incrementAndGet();
        this.deviceRuntime = deviceRuntime;
        this.conn = conn;
        this.sock = sock;
        this.in = new BufferedInputStream(sock.getInputStream());
        this.out = new BufferedOutputStream(sock.getOutputStream());
        this.role = role;
    }

    private Association(DeviceRuntime deviceRuntime, Connection conn, Socket sock,
                        ApplicationEntity ae, AAssociate.RQ aarq)
            throws IOException {
        this(deviceRuntime, conn, sock, Role.REQUESTOR);
        this.name = aarq.getCallingAETitle() + role.delim + aarq.getCalledAETitle() + '(' + serialNo + ')';
        this.ae = ae;
        this.aarq = aarq;
        this.state = State.EXPECT_AA_AC;
        write(aarq);
    }

    private Association(DeviceRuntime deviceRuntime, Connection conn, Socket sock)
            throws IOException {
        this(deviceRuntime, conn, sock, Role.ACCEPTOR);
        this.name = "" + sock.getLocalSocketAddress()
                + role.delim + sock.getRemoteSocketAddress()
                + '(' + serialNo + ')';
        this.state = State.EXPECT_AA_RQ;
    }

    @Override
    public String toString() {
        return name;
    }

    AAssociate.CommonExtendedNegotation commonExtendedNegotationFor(String cuid) {
        return aarq.getCommonExtendedNegotation(cuid);
    }

    static CompletableFuture<Association> open(DeviceRuntime deviceRuntime, ApplicationEntity ae, Connection conn, Socket sock,
                            AAssociate.RQ rq) throws IOException {
        Association as = new Association(deviceRuntime, conn, sock, ae, rq);
        deviceRuntime.executorService.execute(as);
        return as.aaacReceived;
    }

    static Association accept(DeviceRuntime deviceRuntime, Connection conn, Socket sock) throws IOException {
        return new Association(deviceRuntime, conn, sock);
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

    public CompletableFuture<DimseRSP> cecho() throws IOException, InterruptedException {
        return cecho(UID.Verification);
    }

    public CompletableFuture<DimseRSP> cecho(String sopClassUID) throws IOException, InterruptedException {
        int msgid = messageID.incrementAndGet();
        return invoke(sopClassUID, msgid, Dimse.C_ECHO_RQ,
                Dimse.C_ECHO_RQ.mkRQ(msgid, sopClassUID, null, Dimse.NO_DATASET));
    }

    public CompletableFuture<DimseRSP> cstore(String sopClassUID, String sopInstanceUID,
                                              DataWriter dataWriter, String transferSyntax)
            throws IOException, InterruptedException {
        int msgid = messageID.incrementAndGet();
        return invoke(sopClassUID, msgid, Dimse.C_STORE_RQ,
                Dimse.C_STORE_RQ.mkRQ(msgid, sopClassUID, sopInstanceUID, Dimse.WITH_DATASET),
                dataWriter, transferSyntax);
    }

    private CompletableFuture<DimseRSP> invoke(String abstractSyntax, int msgid, Dimse dimse, DicomObject commandSet)
            throws IOException, InterruptedException {
        Byte pcid = pcidFor(abstractSyntax);
        OutstandingRSP outstandingRSP = new OutstandingRSP(msgid, new CompletableFuture<>());
        outstandingRSPs.put(outstandingRSP);
        writeDimse(pcid, dimse, commandSet);
        return outstandingRSP.futureDimseRSP;
    }

    private CompletableFuture<DimseRSP> invoke(String abstractSyntax, int msgid, Dimse dimse, DicomObject commandSet,
                                               DataWriter dataWriter, String transferSyntax)
            throws IOException, InterruptedException {
        Byte pcid = pcidFor(abstractSyntax, transferSyntax);
        OutstandingRSP outstandingRSP = new OutstandingRSP(msgid, new CompletableFuture<>());
        outstandingRSPs.put(outstandingRSP);
        writeDimse(pcid, dimse, commandSet, dataWriter, transferSyntax);
        return outstandingRSP.futureDimseRSP;
    }

    private Byte pcidFor(String abstractSyntax) {
        return aarq.pcidsFor(abstractSyntax)
                .filter(aaac::isAcceptance)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No accepted Presentation Context"));
    }

    private Byte pcidFor(String abstractSyntax, String transferSyntax) {
        return aarq.pcidsFor(abstractSyntax, transferSyntax)
                .filter(pcid -> aaac.acceptedTransferSyntax(pcid, transferSyntax))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No accepted Presentation Context"));
    }

    public CompletableFuture<Association> release() throws IOException {
        writeAReleaseRQ();
        return arrpReceived;
    }

    public void writeDimse(Byte pcid, Dimse dimse, DicomObject commandSet) throws IOException {
        writeLock.lock();
        try {
            opcid = pcid;
            opdvmch = COMMAND;
            opdvPos = 0;
            opduLen = 6;
            new DicomOutputStream(pdvOutputStream).writeCommandSet(commandSet);
            opdvmch = LAST_COMMAND;
            writePDataTF();
        } finally {
            writeLock.unlock();
        }
    }

    public void writeDimse(Byte pcid, Dimse dimse, DicomObject commandSet,
                           DataWriter dataWriter, String transferSyntax) throws IOException {
        writeLock.lock();
        try {
            opcid = pcid;
            opdvmch = COMMAND;
            opdvPos = 0;
            opduLen = 6;
            new DicomOutputStream(pdvOutputStream).writeCommandSet(commandSet);
            opdvmch = LAST_COMMAND;
            setPDVHeader();
            opdvmch = DATA;
            opdvPos = opduLen;
            opduLen += 6;
            dataWriter.writeTo(pdvOutputStream, transferSyntax);
            opdvmch = LAST_DATA;
            writePDataTF();
        } finally {
            writeLock.unlock();
        }
    }

    private void setPDVHeader() {
        int pdvlen = opduLen - opdvPos - 4;
        ByteOrder.BIG_ENDIAN.intToBytes(pdvlen, opdu, opdvPos);
        opdu[opdvPos + 4] = opcid;
        opdu[opdvPos + 5] = opdvmch;
        LOG.trace("{} << PDV[len={}, pcid={}, mch={}]", name, pdvlen, opcid, opdvmch);
    }

    private void writePDataTF() throws IOException {
        setPDVHeader();
        LOG.trace("{} << PDU[type=4, len={}]", name, opduLen);
        out.write(0x04);
        out.write(0);
        Utils.writeInt(out, opduLen);
        out.write(opdu, 0, opduLen);
        out.flush();
        opdvPos = 0;
        opduLen = 6;
    }

    private final OutputStream pdvOutputStream = new OutputStream() {

        @Override
        public void write(int b) throws IOException {
            if (opduLen == opdu.length)
                writePDataTF();
            opdu[opduLen++] = (byte) b;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int d;
            while (len > (d = opdu.length - opduLen)) {
                System.arraycopy(b, off, opdu, opduLen, d);
                opduLen = opdu.length;
                writePDataTF();
                len -= d;
            }
            System.arraycopy(b, off, opdu, opduLen, len);
            opduLen += len;
        }
    };

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

    }

    private void onAAssociateRQ(int pduLength) throws IOException {
        try {
            aarq = AAssociate.RQ.readFrom(in, pduLength);
            name = aarq.getCalledAETitle() + role.delim + aarq.getCallingAETitle() + '(' + serialNo + ')';
            negotiate();
            write(aaac);
            established(aarq.getMaxPDULength(), aaac.getMaxOpsPerformed());
        } catch (AAssociateRJ aarj) {
            aarj.writeTo(out);
            sock.shutdownInput();
            deviceRuntime.closeSocketDelayed(conn, sock);
        }
    }

    private void established(int maxPDULength, int maxOpsInvoked) {
        opdu = new byte[maxPDULength != 0 ? maxPDULength : Connection.DEF_MAX_PDU_LENGTH];
        outstandingRSPs = newBlockingQueue(maxOpsInvoked);
        state = State.EXPECT_PDATA_TF;
    }

    private void negotiate() throws AAssociateRJ {
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

    private static BlockingQueue<OutstandingRSP> newBlockingQueue(int limit) {
        return new LinkedBlockingQueue<>(limit > 0 ? limit : Integer.MAX_VALUE);
    }

    private void onAAssociateAC(int pduLength) throws IOException {
        this.aaac = AAssociate.AC.readFrom(in, pduLength);
        established(aaac.getMaxPDULength(), aaac.getMaxOpsInvoked());
        aaacReceived.complete(this);
    }

    private void onAAssociateRJ(int pduLength) throws IOException {
        AAssociateRJ aarj = AAssociateRJ.readFrom(in, pduLength);
        deviceRuntime.closeSocket(conn, sock);
        aaacReceived.completeExceptionally(aarj);
    }

    private void onAAbort(int pduLength) throws IOException {
        AAbort aAbort = AAbort.readFrom(in, pduLength);
        deviceRuntime.closeSocket(conn, sock);
        aaacReceived.completeExceptionally(aAbort);
        arrpReceived.completeExceptionally(aAbort);
    }

    private void onAReleaseRQ(int pduLength) throws IOException {
        LOG.info("{} >> A-RELEASE-RQ[len={}]", name, pduLength);
        if (pduLength != 4)
            throw AAbort.invalidPDUParameterValue();
        Utils.readInt(in);
        sock.shutdownInput();
        writeAReleaseRP();
        deviceRuntime.closeSocketDelayed(conn, sock);
    }

    private void onAReleaseRP(int pduLength) throws IOException {
        LOG.info("{} >> A-RELEASE-RP[len={}]", name, pduLength);
        if (pduLength != 4)
            throw AAbort.invalidPDUParameterValue();
        Utils.readInt(in);
        deviceRuntime.closeSocket(conn, sock);
        arrpReceived.complete(this);
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
        LOG.info("{} << A-RELEASE-RQ[len=4]", name);
        blockingWrite(0x05, 0, 0, 0);
        state = State.EXPECT_AR_RP;
    }

    private void writeAReleaseRP() throws IOException {
        LOG.info("{} << A-RELEASE-RP[len=4]", name);
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
        sock.shutdownOutput();
    }

    private void onPDataTF(int pduLength) throws IOException {
        ipduRemaining = pduLength;
        onNextPDV();
    }

    private void onNextPDV() throws IOException {
        if ((ipduRemaining -= 4) < 0)
            throw AAbort.invalidPDUParameterValue();
        int pdvlen = Utils.readInt(in);
        int pcid = Utils.readUnsignedByte(in);
        int pdvmch = Utils.readUnsignedByte(in);
        LOG.trace("{} >> PDV[len={}, pcid={}, mch={}]", name, pdvlen, opcid, opdvmch);
        if ((ipdvRemaining = pdvlen - 2) < 0)
            throw AAbort.invalidPDUParameterValue();
        if ((ipduRemaining -= 2 + ipdvRemaining) < 0)
            throw AAbort.invalidPDUParameterValue();
        if ((pdvmch & COMMAND) != (this.ipdvmch & COMMAND))
            throw AAbort.userInitiated();
        this.ipdvmch = pdvmch;
        if (this.ipcid == null) {
            this.ipcid = (byte) pcid;
            AAssociate.AC.PresentationContext pc = aaac.getPresentationContext(this.ipcid);
            if (pc == null)
                throw AAbort.userInitiated();
            if (pc.result != AAssociate.AC.Result.ACCEPTANCE)
                throw AAbort.userInitiated();
            DicomObject commandSet = new DicomInputStream(pdvInputStream).readCommandSet();
            Dimse dimse = Dimse.of(commandSet);
            this.ipdvmch = DATA;
            dimse.handler.accept(this, this.ipcid, dimse, commandSet, pdvInputStream);
            this.ipdvmch = COMMAND;
            this.ipcid = null;
        } else if (this.ipcid.intValue() != pcid) {
            throw AAbort.userInitiated();
        }
    }

    void onDimseRQ(Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream) throws IOException {
        deviceRuntime.onDimseRQ(this, pcid, dimse, commandSet, dataStream);
    }

    void onCancelRQ(Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream) {
    }

    void onDimseRSP(Byte pcid, Dimse dimse, DicomObject commandSet, DicomObject dataSet) {
        int messageID = commandSet.getInt(Tag.MessageIDBeingRespondedTo).getAsInt();
        OutstandingRSP outstandingRSP =
                outstandingRSPs.stream().filter(o -> o.messageID == messageID).findFirst().get();
        outstandingRSPs.remove(outstandingRSP);
        outstandingRSP.futureDimseRSP.complete(new DimseRSP(dimse, commandSet, dataSet));
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

        @Override
        public long skip(long n) throws IOException {
            return skipPDV(n);
        }
    };
    private int readPDV() throws IOException {
        while (ipdvRemaining == 0) {
            if ((ipdvmch & LAST) != 0) return -1;
            if (ipduRemaining == 0) {
                if (nextPDU() != 0x04)
                    throw new EOFException();
            } else
                onNextPDV();
        }
        ipdvRemaining--;
        return Utils.readUnsignedByte(in);
    }

    private int readPDV(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        while (ipdvRemaining == 0) {
            if ((ipdvmch & LAST) != 0) return -1;
            if (ipduRemaining == 0) {
                if (nextPDU() != 0x04)
                    throw new EOFException();
            } else {
                onNextPDV();
            }
        }
        int n = in.read(b, off, Math.min(ipdvRemaining, len));
        if (n == 0)
            throw new EOFException();
        ipdvRemaining -= n;
        return n;
    }

    private long skipPDV(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        while (ipdvRemaining == 0) {
            if ((ipdvmch & LAST) != 0) return 0;
            if (ipduRemaining == 0) {
                if (nextPDU() != 0x04)
                    throw new EOFException();
            } else {
                onNextPDV();
            }
        }
        n = in.skip(Math.min(ipdvRemaining, n));
        ipdvRemaining -= n;
        return n;
    }

    public interface DataWriter {
        void writeTo(OutputStream out, String tsuid) throws IOException;
    }

    private static class OutstandingRSP {
        final int messageID;
        final CompletableFuture<DimseRSP> futureDimseRSP;

        private OutstandingRSP(int messageID, CompletableFuture<DimseRSP> futureDimseRSP) {
            this.messageID = messageID;
            this.futureDimseRSP = futureDimseRSP;
        }
    }
}
