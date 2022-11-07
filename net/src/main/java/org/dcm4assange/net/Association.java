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
    public static final int MAX_SEND_PDU_LENGTH = 65536;
    private static final AtomicInteger prevSerialNo = new AtomicInteger();
    private static final int COMMAND = 0b01;
    private static final int LAST = 0b10;
    private static final int LAST_COMMAND = 0b11;
    private static final int DATA = 0b00;
    private static final int LAST_DATA = 0b10;
    private static final int FIRST_DATA = 0b100;
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
    private final PDVInputStream pdvIn = new PDVInputStream();
    private PDVOutputStream pdvOut;

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
        LOG.info("{} << {}", name, aarq);
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
            pdvOut.writeCommandSet(pcid, dimse, commandSet);
            pdvOut.flush();
        } finally {
            writeLock.unlock();
        }
    }

    public void writeDimse(Byte pcid, Dimse dimse, DicomObject commandSet,
                           DataWriter dataWriter, String transferSyntax) throws IOException {
        writeLock.lock();
        try {
            pdvOut.writeCommandSet(pcid, dimse, commandSet);
            if (conn.isPackPDV()) {
                pdvOut.setPDVHeader();
            } else {
                pdvOut.flush();
            }
            pdvOut.writeDataset(dataWriter, transferSyntax);
            pdvOut.flush();
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
                as.pdvIn.onPDataTF(pduLength);
            }

            @Override
            public void onAReleaseRQ(Association as, int pduType, int pduLength) throws IOException {
                as.onAReleaseRQ(pduLength);
            }
        },
        EXPECT_AR_RP {
            @Override
            public void onPDataTF(Association as, int pduType, int pduLength) throws IOException {
                as.pdvIn.onPDataTF(pduLength);
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
            LOG.info("{} >> {}", name, aarq);
            name = aarq.getCalledAETitle() + role.delim + aarq.getCallingAETitle() + '(' + serialNo + ')';
            negotiate();
            LOG.info("{} << {}", name, aaac);
            write(aaac);
            established(aarq.getMaxPDULength(), aaac.getMaxOpsPerformed());
        } catch (AAssociateRJ aarj) {
            LOG.info("{} << {}", name, aarj);
            aarj.writeTo(out);
            sock.shutdownInput();
            deviceRuntime.closeSocketDelayed(conn, sock);
        }
    }

    private void established(int maxPDULength, int maxOpsInvoked) {
        pdvOut = new PDVOutputStream(minZeroAsMax(maxPDULength,
                minZeroAsMax(conn.getSendPDULength(), MAX_SEND_PDU_LENGTH)));
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
        aaac.setMaxPDULength(conn);
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
        LOG.info("{} >> {}", name, aaac);
        established(aaac.getMaxPDULength(), aaac.getMaxOpsInvoked());
        aaacReceived.complete(this);
    }

    private void onAAssociateRJ(int pduLength) throws IOException {
        AAssociateRJ aarj = AAssociateRJ.readFrom(in, pduLength);
        LOG.info("{} >> {}", name, aarj);
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

    private class PDVInputStream extends InputStream {
        int pduRemaining;
        int pdvRemaining;
        Byte pcid;
        int mch;

        @Override
        public int read() throws IOException {
            if (!nextPDV()) return -1;
            pdvRemaining--;
            return Utils.readUnsignedByte(in);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, b.length);
            if (len == 0) return 0;
            if (!nextPDV()) return -1;
            int n = in.read(b, off, Math.min(pdvRemaining, len));
            if (n == 0)
                throw new EOFException();
            pdvRemaining -= n;
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            if (n <= 0) return 0;
            if (!nextPDV()) return 0;
            n = in.skip(Math.min(pdvRemaining, n));
            pdvRemaining -= n;
            return n;
        }

        private void onPDataTF(int pduLength) throws IOException {
            LOG.trace("{} >> P-DATA-TF[len={}]", name, pduLength);
            pduRemaining = pduLength;
            if (pcid == null) {
                mch = COMMAND;
                pcid = (byte) parsePDVHeader();
                AAssociate.AC.PresentationContext pc = aaac.getPresentationContext(pcid);
                if (pc == null)
                    throw AAbort.userInitiated();
                if (pc.result != AAssociate.AC.Result.ACCEPTANCE)
                    throw AAbort.userInitiated();
                DicomObject commandSet = new DicomInputStream(this).readCommandSet();
                Dimse dimse = Dimse.of(commandSet);
                if (LOG.isInfoEnabled()) {
                    LOG.info("{} >> {}", name, dimse.toString(pcid, commandSet, getTransferSyntax(pcid)));
                    LOG.debug("{} >> Command:\n{}", name, commandSet);
                }
                mch = FIRST_DATA;
                dimse.handler.accept(Association.this, pcid, dimse, commandSet, this);
                pcid = null;
            }
        }

        boolean nextPDV() throws IOException {
            while (pdvRemaining == 0) {
                if ((mch & LAST) != 0) {
                    if ((mch & COMMAND) == 0)
                        LOG.debug("{} >> Data finished", name);
                    return false;
                }
                if (pduRemaining == 0) {
                    if (nextPDU() != 0x04)
                        throw new EOFException();
                }
                if (parsePDVHeader() != pcid) {
                    throw AAbort.userInitiated();
                }
            }
            return true;
        }

        private int parsePDVHeader() throws IOException {
            if ((pduRemaining -= 6) < 0)
                throw AAbort.invalidPDUParameterValue();
            int pdvlen = Utils.readInt(in);
            int pcid = Utils.readUnsignedByte(in);
            int mch = Utils.readUnsignedByte(in);
            LOG.trace("{} >> PDV[len={}, pcid={}, mch={}]", name, pdvlen, pcid, mch);
            if ((pdvRemaining = pdvlen - 2) < 0)
                throw AAbort.invalidPDUParameterValue();
            if ((pduRemaining -= pdvRemaining) < 0)
                throw AAbort.invalidPDUParameterValue();
            if ((mch & COMMAND) != (this.mch & COMMAND))
                throw AAbort.userInitiated();
            if (this.mch == FIRST_DATA)
                LOG.debug("{} >> Data start", name);
            this.mch = mch;
            return pcid;
        }

    }

    private class PDVOutputStream extends OutputStream {
        final byte[] pdu;
        int len;
        int pos;
        Byte pcid;
        byte mch;

        PDVOutputStream(int maxPDULength) {
            this.pdu = new byte[maxPDULength];
        }

        @Override
        public void write(int b) throws IOException {
            if (len == pdu.length) {
                flush();
                this.pos = 0;
                this.len = 6;
            }
            pdu[len++] = (byte) b;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int d;
            while (len > (d = pdu.length - this.len)) {
                System.arraycopy(b, off, pdu, this.len, d);
                this.len = pdu.length;
                flush();
                this.pos = 0;
                this.len = 6;
                len -= d;
            }
            System.arraycopy(b, off, pdu, this.len, len);
            this.len += len;
        }

        @Override
        public void flush() throws IOException {
            setPDVHeader();
            LOG.trace("{} << P-DATA-TF[len={}]", name, len);
            synchronized (out) {
                out.write(0x04);
                out.write(0);
                Utils.writeInt(out, len);
                out.write(pdu, 0, len);
                out.flush();
            }
            len = 0;
        }

        void writeCommandSet(Byte pcid, Dimse dimse, DicomObject commandSet) throws IOException {
            if (LOG.isInfoEnabled()) {
                LOG.info("{} << {}", name, dimse.toString(pcid, commandSet, getTransferSyntax(pcid)));
                LOG.debug("{} << Command:\n{}", name, commandSet);
            }
            this.pcid = pcid;
            mch = COMMAND;
            pos = 0;
            len = 6;
            new DicomOutputStream(this).writeCommandSet(commandSet);
            mch = LAST_COMMAND;
        }

        void writeDataset(DataWriter dataWriter, String transferSyntax) throws IOException {
            LOG.debug("{} << Data start", name);
            mch = DATA;
            pos = len;
            len += 6;
            dataWriter.writeTo(this, transferSyntax);
            mch = LAST_DATA;
            LOG.debug("{} << Data finished", name);
        }

        void setPDVHeader() {
            int pdvlen = len - pos - 4;
            ByteOrder.BIG_ENDIAN.intToBytes(pdvlen, pdu, pos);
            pdu[pos + 4] = pcid;
            pdu[pos + 5] = mch;
            LOG.trace("{} << PDV[len={}, pcid={}, mch={}]", name, pdvlen, pcid, mch);
        }
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
