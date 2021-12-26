package org.dcm4assange.net;

import org.dcm4assange.Implementation;
import org.dcm4assange.UID;
import org.dcm4assange.util.UIDUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.dcm4assange.util.StringUtils.EMPTY_STRINGS;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Nov 2019
 */
public abstract class AAssociate {
    public static final int DEF_MAX_PDU_LENGTH = 16378;
    // to fit into SunJSSE TLS Application Data Length 16408

    private int protocolVersion = 1;
    private String calledAETitle = "ANONYMOUS";
    private String callingAETitle = "ANONYMOUS";
    private String applicationContextName = UID.DICOMApplicationContext;
    private int maxPDULength = DEF_MAX_PDU_LENGTH;
    private String implClassUID = Implementation.CLASS_UID;
    private String implVersionName = Implementation.VERSION_NAME;
    private int asyncOpsWindow = -1;
    private final Map<String, RoleSelection> roleSelectionMap = new LinkedHashMap<>();
    private final Map<String, byte[]> extNegMap = new LinkedHashMap<>();

    public String getCalledAETitle() {
        return calledAETitle;
    }

    public void setCalledAETitle(String calledAETitle) {
        this.calledAETitle = calledAETitle;
    }

    public String getCallingAETitle() {
        return callingAETitle;
    }

    public void setCallingAETitle(String callingAETitle) {
        this.callingAETitle = callingAETitle;
    }

    public String getApplicationContextName() {
        return applicationContextName;
    }

    public void setApplicationContextName(String applicationContextName) {
        this.applicationContextName = applicationContextName;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getImplClassUID() {
        return implClassUID;
    }

    public void setImplClassUID(String implClassUID) {
        this.implClassUID = implClassUID;
    }

    public String getImplVersionName() {
        return implVersionName;
    }

    public void setImplVersionName(String implVersionName) {
        this.implVersionName = implVersionName;
    }

    public int getMaxPDULength() {
        return maxPDULength;
    }

    public void setMaxPDULength(int maxPDULength) {
        this.maxPDULength = maxPDULength;
    }

    public int getMaxOpsInvoked() {
        return asyncOpsWindow != -1 ? asyncOpsWindow >> 16 : 1;
    }

    public int getMaxOpsPerformed() {
        return asyncOpsWindow != -1 ? asyncOpsWindow & 0xffff : 1;
    }

    public void setAsyncOpsWindow(int maxOpsInvoked, int maxOpsPerformed) {
        this.asyncOpsWindow = maxOpsInvoked << 16 | maxOpsPerformed;
    }

    public void clearAsyncOpsWindow() {
        this.asyncOpsWindow = -1;
    }

    public boolean hasAsyncOpsWindow() {
        return asyncOpsWindow != -1;
    }

    public void putRoleSelection(String cuid, RoleSelection roleSelection) {
        roleSelectionMap.put(cuid, roleSelection);
    }

    public RoleSelection getRoleSelection(String cuid) {
        return roleSelectionMap.get(cuid);
    }

    public void putExtendedNegotation(String cuid, byte[] extNeg) {
        extNegMap.put(cuid, extNeg.clone());
    }

    public byte[] getExtendedNegotation(String cuid) {
        return clone(extNegMap.get(cuid));
    }

    String toString(String header) {
        StringBuilder sb = new StringBuilder(512)
                .append(header)
                .append(System.lineSeparator())
                .append("  called-AE-title: ")
                .append(calledAETitle)
                .append(System.lineSeparator())
                .append("  calling-AE-title: ")
                .append(callingAETitle)
                .append(System.lineSeparator())
                .append("  application-context-name: ");
        UIDUtils.promptTo(applicationContextName, sb)
                .append(System.lineSeparator())
                .append("  implementation-class-uid: ")
                .append(implClassUID)
                .append(System.lineSeparator());
        if (implVersionName != null) {
            sb.append("  implementation-version-name: ")
                    .append(implVersionName)
                    .append(System.lineSeparator());
        }
        sb.append("  max-pdu-length: ")
                .append(maxPDULength)
                .append(System.lineSeparator());
        if (asyncOpsWindow != -1) {
            sb.append("  max-ops-invoked: ")
                    .append(asyncOpsWindow >> 16)
                    .append(System.lineSeparator())
                    .append("  max-ops-performed: ")
                    .append(asyncOpsWindow & 0xffff)
                    .append(System.lineSeparator());
        }
        promptUserIdentityTo(sb);
        promptPresentationContextsTo(sb);
        roleSelectionMap.forEach((cuid, rs) -> promptTo(cuid, rs, sb));
        extNegMap.forEach((cuid, b) -> promptTo(cuid, b, sb));
        promptCommonExtendedNegotationTo(sb);
        return sb.append(']').toString();
    }

    private void promptTo(String cuid, RoleSelection roleSelection, StringBuilder sb) {
        sb.append("  RoleSelection[")
                .append(System.lineSeparator())
                .append("    sop-class: ");
        UIDUtils.promptTo(cuid, sb)
                .append(System.lineSeparator())
                .append("    role(s): ").append(roleSelection)
                .append(System.lineSeparator())
                .append("  ]")
                .append(System.lineSeparator());
    }

    private void promptTo(String cuid, byte[] info, StringBuilder sb) {
        sb.append("  ExtendedNegotiation[")
                .append(System.lineSeparator())
                .append("    sop-class: ");
        UIDUtils.promptTo(cuid, sb)
                .append(System.lineSeparator())
                .append("    info: [");
        for (int i = 0; i < info.length; i++) {
            if (i  > 0) sb.append(", ");
            sb.append(info[i]);
        }
        sb.append(']')
                .append(System.lineSeparator())
                .append("  ]")
                .append(System.lineSeparator());
    }

    abstract void promptUserIdentityTo(StringBuilder sb);

    abstract void promptPresentationContextsTo(StringBuilder sb);

    void promptCommonExtendedNegotationTo(StringBuilder sb) {}

    static byte[] clone(byte[] value) {
        return value != null ? value.clone() : null;
    }

    int pduLength() {
        return 76 + applicationContextName.length() + presentationContextLength() + userItemLength();
    }

    abstract int presentationContextLength();

    int userItemLength() {
        return (asyncOpsWindow != -1 ? 24 : 16)
                + implClassUID.length()
                + implVersionName.length()
                + roleSelectionMap.keySet().stream().mapToInt(cuid -> 8 + cuid.length()).sum()
                + extNegMap.entrySet().stream()
                    .mapToInt(e -> 6 + e.getKey().length() + e.getValue().length)
                    .sum();
    }

    void writeTo(DataOutputStream dos) throws IOException {
        byte[] buf = new byte[64];
        dos.writeShort(protocolVersion);
        dos.writeShort(0);
        Arrays.fill(buf, 0, 32, (byte) 0x20);
        calledAETitle.getBytes(0, calledAETitle.length(), buf, 0);
        callingAETitle.getBytes(0, callingAETitle.length(), buf, 16);
        dos.write(buf);
        dos.writeShort(0x1000);
        writeLengthASCII(dos, applicationContextName, buf);
        writePresentationContextTo(dos, buf);
        dos.writeShort(0x5000);
        dos.writeShort(userItemLength());
        dos.writeInt(0x51000004);
        dos.writeInt(maxPDULength);
        dos.writeShort(0x5200);
        writeLengthASCII(dos, implClassUID, buf);
        dos.writeShort(0x5500);
        writeLengthASCII(dos, implVersionName, buf);
        if (asyncOpsWindow != -1) {
            dos.writeInt(0x53000004);
            dos.writeInt(asyncOpsWindow);
        }
        for (Map.Entry<String, RoleSelection> e : roleSelectionMap.entrySet()) {
            dos.writeShort(0x5400);
            dos.writeShort((4 + e.getKey().length()));
            writeLengthASCII(dos,  e.getKey(), buf);
            dos.writeBoolean(e.getValue().scu);
            dos.writeBoolean(e.getValue().scp);
        }
        for (Map.Entry<String, byte[]> e : extNegMap.entrySet()) {
            dos.writeShort(0x5600);
            dos.writeShort((2 + e.getKey().length() + e.getValue().length));
            writeLengthASCII(dos, e.getKey(), buf);
            dos.write(e.getValue());
        }
        writeCommonExtendedNegotationTo(dos, buf);
        writeUserIdentityTo(dos, buf);
    }

    abstract void writePresentationContextTo(DataOutputStream dos, byte[] buf) throws IOException;

    void writeCommonExtendedNegotationTo(DataOutputStream dos, byte[] buf) throws IOException {
    }

    abstract void writeUserIdentityTo(DataOutputStream dos, byte[] buf) throws IOException;

    void parse(DataInputStream dis, int pduLength) throws IOException {
        int remaining = pduLength - 68;
        if (remaining < 0)
            throw AAbort.invalidPDUParameterValue();
        byte[] buf = new byte[64];
        protocolVersion = dis.readShort();
        if ((dis.read() | dis.read()) < 0)
            throw new EOFException();
        dis.readFully(buf);
        calledAETitle = new String(buf, 0, 0, 16).trim();
        callingAETitle = new String(buf, 0, 16, 32).trim();
        applicationContextName = null;
        maxPDULength = -1;
        implClassUID = null;
        implVersionName = null;
        int itemType, itemLength, itemRemaining, subitemType, subitemLength, uidLen;
        while (remaining > 0) {
            if ((remaining -= 4) < 0)
                throw AAbort.invalidPDUParameterValue();
            itemType = dis.readUnsignedByte();
            if (dis.read() < 0)
                throw new EOFException();
            itemLength = dis.readUnsignedShort();
            if ((remaining -= itemLength) < 0)
                throw AAbort.invalidPDUParameterValue();
            switch (itemType) {
                case 0x10:
                    applicationContextName = readASCII(dis, itemLength, buf);
                    break;
                case 0x20:
                    parsePresentationContextRQ(dis, itemLength, buf);
                    break;
                case 0x21:
                    parsePresentationContextAC(dis, itemLength, buf);
                    break;
                case 0x50:
                    itemRemaining = itemLength;
                    while (itemRemaining > 0) {
                        if ((itemRemaining -= 4) < 0)
                            throw AAbort.invalidPDUParameterValue();
                        subitemType = dis.readUnsignedByte();
                        if (dis.read() < 0)
                            throw new EOFException();
                        subitemLength = dis.readUnsignedShort();
                        if ((itemRemaining -= subitemLength) < 0)
                            throw AAbort.invalidPDUParameterValue();
                        switch (subitemType) {
                            case 0x51:
                                if (subitemLength != 4)
                                    throw AAbort.invalidPDUParameterValue();
                                maxPDULength = dis.readInt();
                                break;
                            case 0x52:
                                implClassUID =  readASCII(dis, subitemLength, buf);
                                break;
                            case 0x53:
                                if (subitemLength != 4)
                                    throw AAbort.invalidPDUParameterValue();
                                asyncOpsWindow = dis.readInt();
                                break;
                            case 0x54:
                                if (subitemLength < 4)
                                    throw AAbort.invalidPDUParameterValue();
                                uidLen = dis.readUnsignedShort();
                                if (subitemLength != 4 + uidLen)
                                    throw AAbort.invalidPDUParameterValue();
                                roleSelectionMap.put(
                                        readASCII(dis, uidLen, buf),
                                        RoleSelection.of(dis.readBoolean(), dis.readBoolean()));
                                break;
                            case 0x55:
                                implVersionName =  readASCII(dis, subitemLength, buf);
                                break;
                            case 0x56:
                                if (subitemLength < 2)
                                    throw AAbort.invalidPDUParameterValue();
                                uidLen = dis.readUnsignedShort();
                                if (subitemLength < 2 + uidLen)
                                    throw AAbort.invalidPDUParameterValue();
                                extNegMap.put(
                                        readASCII(dis, uidLen, buf),
                                        readBytes(dis, subitemLength - (2 + uidLen)));
                                break;
                            case 0x57:
                                parseCommonExtendedNegotation(dis, subitemLength, buf);
                                break;
                            case 0x58:
                                parseUserIdentityRQ(dis, subitemLength, buf);
                                break;
                            case 0x59:
                                parseUserIdentityAC(dis, subitemLength, buf);
                                break;
                            default:
                                throw AAbort.unexpectedOrUnrecognizedItem(subitemType);
                        }
                    }
                    break;
                default:
                    throw AAbort.unexpectedOrUnrecognizedItem(itemType);
            }
        }
        if (applicationContextName == null) {
            throw AAbort.invalidPDUParameterValue();
        }
        if (maxPDULength == -1) {
            throw AAbort.invalidPDUParameterValue();
        }
        if (implClassUID == null) {
            throw AAbort.invalidPDUParameterValue();
        }
    }

    void parsePresentationContextRQ(DataInputStream dis, int itemLength, byte[] buf)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    void parsePresentationContextAC(DataInputStream dis, int itemLength, byte[] buf)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    void parseCommonExtendedNegotation(DataInputStream dis, int subitemLength, byte[] buf)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    void parseUserIdentityRQ(DataInputStream dis, int subitemLength, byte[] buf)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    void parseUserIdentityAC(DataInputStream dis, int subitemLength, byte[] buf)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    static String readASCII(DataInputStream dis, int length, byte[] buf) throws IOException {
        dis.readFully(buf, 0, length);
        return new String(buf, 0, 0, length);
    }

    static void writeLengthASCII(DataOutputStream dos, String s, byte[] buf) throws IOException {
        int length = s.length();
        s.getBytes(0, length, buf, 0);
        dos.writeShort(length);
        dos.write(buf, 0, length);
    }

    static byte[] readBytes(DataInputStream dis, int length) throws IOException {
        byte[] buf = new byte[length];
        dis.readFully(buf, 0, length);
        return buf;
    }

    public static class RQ extends AAssociate {

        private final Map<Byte, PresentationContext> pcs = new LinkedHashMap<>();
        private final Map<String, CommonExtendedNegotation> commonExtNegMap = new LinkedHashMap<>();
        private UserIdentity userIdentity;

        public RQ() {}

        public static RQ readFrom(DataInputStream dis, int pduLength) throws IOException {
            RQ rq = new RQ();
            rq.parse(dis, pduLength);
            return rq;
        }

        public void putPresentationContext(Byte id, String abstractSyntax, String... transferSyntaxes) {
            pcs.put(id, new PresentationContext(abstractSyntax, transferSyntaxes));
        }

        public Byte findOrAddPresentationContext(String abstractSyntax, String transferSyntax) {
            return pcidsFor(abstractSyntax, transferSyntax).findFirst().orElseGet(
                    () -> addPresentationContext(abstractSyntax, transferSyntax));
        }

        private Byte addPresentationContext(String abstractSyntax, String transferSyntax) {
            if (pcs.size() >= 128)
                throw new IllegalStateException("Maximal number (128) of Presentation Contexts reached");

            Byte pcid = IntStream.iterate(pcs.size() * 2 + 1, i -> i + 2)
                    .mapToObj(i -> Byte.valueOf((byte) i))
                    .filter(((Predicate<Byte>) pcs.keySet()::contains).negate())
                    .findFirst()
                    .get();
            pcs.put(pcid, new PresentationContext(abstractSyntax, transferSyntax));
            return pcid;
        }

        public PresentationContext getPresentationContext(Byte id) {
            return pcs.get(id);
        }

        public void forEachPresentationContext(BiConsumer<Byte, PresentationContext> action) {
            pcs.forEach(action);
        }

        Stream<Byte> pcidsFor(String abstractSyntax) {
            return pcs.entrySet().stream()
                    .filter(e -> e.getValue().equalsAbstractSyntax(abstractSyntax))
                    .map(Map.Entry::getKey);
        }

        Stream<Byte> pcidsFor(String abstractSyntax, String transferSyntax) {
            return pcs.entrySet().stream()
                    .filter(e -> e.getValue().matches(abstractSyntax, transferSyntax))
                    .map(Map.Entry::getKey);
        }

        public void putCommonExtendedNegotation(String cuid, String serviceClass, String... relatedSOPClasses) {
            commonExtNegMap.put(cuid, new CommonExtendedNegotation(serviceClass, relatedSOPClasses));
        }

        public CommonExtendedNegotation getCommonExtendedNegotation(String cuid) {
            return commonExtNegMap.get(cuid);
        }

        public UserIdentity getUserIdentity() {
            return userIdentity;
        }

        public void setUserIdentity(int type, boolean positiveResponseRequested, byte[] primaryField) {
            setUserIdentity(type, positiveResponseRequested, primaryField, new byte[0]);
        }

        public void setUserIdentity(int type, boolean positiveResponseRequested, byte[] primaryField,
                byte[] secondaryField) {
            this.userIdentity = new UserIdentity(type, positiveResponseRequested, primaryField, secondaryField);
        }

        @Override
        public String toString() {
            return toString("A-ASSOCIATE-RQ[");
        }

        @Override
        void promptUserIdentityTo(StringBuilder sb) {
            if (userIdentity != null) userIdentity.promptTo(sb);
        }

        @Override
        void promptPresentationContextsTo(StringBuilder sb) {
            pcs.forEach((pcid, pc) -> pc.promptTo(pcid, sb));
        }

        @Override
        protected void promptCommonExtendedNegotationTo(StringBuilder sb) {
            commonExtNegMap.forEach((cuid, cen) -> cen.promptTo(cuid, sb));
        }

        @Override
        int presentationContextLength() {
            return pcs.values().stream().mapToInt(pc -> 4 + pc.itemLength()).sum();
        }

        @Override
        int userItemLength() {
            return super.userItemLength()
                    + commonExtNegMap.entrySet().stream()
                        .mapToInt(e -> 6 + e.getKey().length() + e.getValue().length())
                        .sum()
                    + (userIdentity != null ? 4 + userIdentity.itemLength() : 0);
        }

        @Override
        void writePresentationContextTo(DataOutputStream dos, byte[] buf) throws IOException {
            for (Map.Entry<Byte, RQ.PresentationContext> e : pcs.entrySet()) {
                dos.writeShort(0x2000);
                dos.writeShort(e.getValue().itemLength());
                dos.writeShort(e.getKey().intValue() << 8);
                e.getValue().writeTo(dos, buf);
            }
        }

        @Override
        void parsePresentationContextRQ(DataInputStream dis, int itemLength, byte[] buf)
                throws IOException {
            int remaining = itemLength - 4;
            if (remaining < 0)
                throw AAbort.invalidPDUParameterValue();
            byte pcid = (byte) dis.readUnsignedByte();
            if ((dis.read() | dis.read() | dis.read()) < 0)
                throw new EOFException();
            String abstractSyntax = null;
            List<String> transferSyntaxes = new ArrayList<>();
            int subitemType, subitemLength;
            while (remaining > 0) {
                if ((remaining -= 4) < 0)
                    throw AAbort.invalidPDUParameterValue();
                subitemType = dis.readUnsignedByte();
                if (dis.read() < 0)
                    throw new EOFException();
                subitemLength = dis.readUnsignedShort();
                if ((remaining -= subitemLength) < 0)
                    throw AAbort.invalidPDUParameterValue();
                switch (subitemType) {
                    case 0x30:
                        abstractSyntax = readASCII(dis, subitemLength, buf);
                        break;
                    case 0x40:
                        transferSyntaxes.add(readASCII(dis, subitemLength, buf));
                        break;
                    default:
                        throw AAbort.unexpectedOrUnrecognizedItem(subitemType);
                }
            }
            if (abstractSyntax == null) {
                throw AAbort.invalidPDUParameterValue();
            }
            if (transferSyntaxes.isEmpty()) {
                throw AAbort.invalidPDUParameterValue();
            }
            putPresentationContext(pcid, abstractSyntax, transferSyntaxes.toArray(EMPTY_STRINGS));
        }

        @Override
        void writeCommonExtendedNegotationTo(DataOutputStream dos, byte[] buf) throws IOException {
            for (Map.Entry<String, CommonExtendedNegotation> e : commonExtNegMap.entrySet()) {
                dos.writeShort(0x5700);
                dos.writeShort(2 + e.getKey().length() + e.getValue().length());
                writeLengthASCII(dos, e.getKey(), buf);
                e.getValue().writeTo(dos, buf);
            }
        }

        @Override
        void parseCommonExtendedNegotation(DataInputStream dis, int subitemLength, byte[] buf)
                throws IOException {
            int remaining = subitemLength - 6;
            if (remaining < 0)
                throw AAbort.invalidPDUParameterValue();
            int length = dis.readUnsignedShort();
            if ((remaining -= length) < 0)
                throw AAbort.invalidPDUParameterValue();
            String cuid = readASCII(dis, length, buf);
            length = dis.readUnsignedShort();
            if ((remaining -= length) < 0)
                throw AAbort.invalidPDUParameterValue();
            String serviceClass = readASCII(dis, length, buf);
            length = dis.readUnsignedShort();
            if (remaining != length)
                throw AAbort.invalidPDUParameterValue();
            List<String> relatedSOPClasses = new ArrayList<>();
            while (remaining > 0) {
                if ((remaining -= 2) < 0)
                    throw AAbort.invalidPDUParameterValue();
                length = dis.readUnsignedShort();
                if ((remaining -= length) < 0)
                    throw AAbort.invalidPDUParameterValue();
                relatedSOPClasses.add(readASCII(dis, length, buf));
            }
            putCommonExtendedNegotation(cuid, serviceClass, relatedSOPClasses.toArray(EMPTY_STRINGS));
        }

        @Override
        void writeUserIdentityTo(DataOutputStream dos, byte[] buf) throws IOException {
            if (userIdentity != null) {
                dos.writeShort(0x5800);
                dos.writeShort(userIdentity.itemLength());
                userIdentity.writeTo(dos);
            }
        }

        @Override
        void parseUserIdentityRQ(DataInputStream dis, int subitemLength, byte[] buf)
                throws IOException {
            int remaining = subitemLength - 6;
            if (remaining < 0)
                throw AAbort.invalidPDUParameterValue();
            int type = dis.readUnsignedByte();
            boolean responseRequested = dis.readBoolean();
            int length = dis.readUnsignedShort();
            if ((remaining -= length) < 0)
                throw AAbort.invalidPDUParameterValue();
            byte[] primaryField = readBytes(dis, length);
            length = dis.readUnsignedShort();
            if (remaining != length)
                throw AAbort.invalidPDUParameterValue();
            byte[] secondaryField = readBytes(dis, length);
            setUserIdentity(type, responseRequested, primaryField, secondaryField);
        }

        public static class PresentationContext {

            private String abstractSyntax;
            private final List<String> transferSyntaxList = new ArrayList<>();

            PresentationContext(String abstractSyntax, String... transferSyntaxes) {
                this.abstractSyntax = abstractSyntax;
                this.transferSyntaxList.addAll(List.of(transferSyntaxes));
            }

            public String abstractSyntax() {
                return abstractSyntax;
            }

            public String anyTransferSyntax() {
                return transferSyntaxList.get(0);
            }

            public String[] transferSyntax() {
                return transferSyntaxList.toArray(new String[0]);
            }

            public boolean equalsAbstractSyntax(String abstractSyntax) {
                return this.abstractSyntax.equals(abstractSyntax);
            }

            public boolean containsTransferSyntax(String transferSyntax) {
                return transferSyntaxList.contains(transferSyntax);
            }

            public boolean matches(String abstractSyntax, String transferSyntax) {
                return equalsAbstractSyntax(abstractSyntax) && containsTransferSyntax(transferSyntax);
            }

            int itemLength() {
                return 8 + abstractSyntax.length()
                        + transferSyntaxList.stream().mapToInt(s -> 4 + s.length()).sum();
            }

            void writeTo(DataOutputStream dos, byte[] buf) throws IOException {
                dos.writeInt(0x3000);
                writeLengthASCII(dos, abstractSyntax, buf);
                for (String uid : transferSyntaxList) {
                    dos.writeShort(0x4000);
                    writeLengthASCII(dos, uid, buf);
                }
            }

            void promptTo(Byte pcid, StringBuilder sb) {
                sb.append("  PresentationContext[pcid: ")
                        .append(pcid)
                        .append(System.lineSeparator())
                        .append("    abstract-syntax: ");
                UIDUtils.promptTo(abstractSyntax, sb)
                        .append(System.lineSeparator());
                transferSyntaxList.forEach(ts ->
                        UIDUtils.promptTo(ts, sb.append("    transfer-syntax: "))
                            .append(System.lineSeparator()));
                sb.append("  ]")
                        .append(System.lineSeparator());
            }
        }
    }

    public static class AC extends AAssociate {

        private final Map<Byte, PresentationContext> pcs = new LinkedHashMap<>();
        private byte[] userIdentityServerResponse;

        public AC() {}

        public static AC readFrom(DataInputStream dis, int pduLength) throws IOException {
            AC ac = new AC();
            ac.parse(dis, pduLength);
            return ac;
        }

        public void putPresentationContext(Byte id, Result result, String transferSyntax) {
            pcs.put(id, new PresentationContext(result, transferSyntax));
        }

        public PresentationContext getPresentationContext(Byte id) {
            return pcs.get(id);
        }

        public byte[] getUserIdentityServerResponse() {
            return clone(userIdentityServerResponse);
        }

        public void setUserIdentityServerResponse(byte[] userIdentityServerResponse) {
            this.userIdentityServerResponse = clone(userIdentityServerResponse);
        }

        public boolean acceptedTransferSyntax(Byte pcid, String transferSyntax) {
            PresentationContext pc = pcs.get(pcid);
            return pc != null && pc.result == Result.ACCEPTANCE && pc.transferSyntax.equals(transferSyntax);
        }

        boolean isAcceptance(Byte pcid) {
            PresentationContext pc = pcs.get(pcid);
            return pc != null && pc.result == Result.ACCEPTANCE;
        }

        @Override
        public String toString() {
            return toString("A-ASSOCIATE-AC[");
        }

        @Override
        void promptUserIdentityTo(StringBuilder sb) {
            if (userIdentityServerResponse != null) {
                sb.append("  UserIdentity[")
                        .append(System.lineSeparator())
                        .append("    server-response: byte[")
                        .append(userIdentityServerResponse.length)
                        .append("]")
                        .append(System.lineSeparator())
                        .append("  ]")
                        .append(System.lineSeparator());
            }
        }

        @Override
        void promptPresentationContextsTo(StringBuilder sb) {
            pcs.forEach((pcid, pc) -> pc.promptTo(pcid, sb));
        }

        @Override
        int presentationContextLength() {
            return pcs.values().stream().mapToInt(pc -> 4 + pc.itemLength()).sum();
        }

        @Override
        int userItemLength() {
            return super.userItemLength()
                    + (userIdentityServerResponse != null ? 6 + userIdentityServerResponse.length : 0);
        }

        @Override
        void writePresentationContextTo(DataOutputStream dos, byte[] buf) throws IOException {
            for (Map.Entry<Byte, PresentationContext> e : pcs.entrySet()) {
                dos.writeShort(0x2100);
                dos.writeShort(e.getValue().itemLength());
                dos.writeShort(e.getKey().intValue() << 8);
                e.getValue().writeTo(dos, buf);
            }
        }

        @Override
        void parsePresentationContextAC(DataInputStream dis, int itemLength, byte[] buf)
                throws IOException {
            int remaining = itemLength - 4;
            if (remaining < 0)
                throw AAbort.invalidPDUParameterValue();
            byte pcid = (byte) dis.readUnsignedByte();
            if (dis.read() < 0)
                throw new EOFException();
            Result result = switch (dis.readUnsignedByte()) {
                    case 0 -> Result.ACCEPTANCE;
                    case 1 -> Result.USER_REJECTION;
                    case 2 -> Result.NO_REASON;
                    case 3 -> Result.ABSTRACT_SYNTAX_NOT_SUPPORTED;
                    case 4 -> Result.TRANSFER_SYNTAXES_NOT_SUPPORTED;
                    default -> throw AAbort.invalidPDUParameterValue();
                };
            if (dis.read() < 0)
                throw new EOFException();
            int subitemType, subitemLength;
            String transferSyntax = null;
            while (remaining > 0) {
                if ((remaining -= 4) < 0)
                    throw AAbort.invalidPDUParameterValue();
                subitemType = dis.readUnsignedByte();
                if (dis.read() < 0)
                    throw new EOFException();
                subitemLength = dis.readUnsignedShort();
                if ((remaining -= subitemLength) < 0)
                    throw AAbort.invalidPDUParameterValue();
                switch (subitemType) {
                    case 0x40:
                        transferSyntax = readASCII(dis, subitemLength, buf);
                        break;
                    default:
                        throw AAbort.unexpectedOrUnrecognizedItem(subitemType);
                }
            }
            if (transferSyntax == null) {
                throw AAbort.invalidPDUParameterValue();
            }
            putPresentationContext(pcid, result, transferSyntax);
        }

        @Override
        void writeUserIdentityTo(DataOutputStream dos, byte[] buf) throws IOException {
            if (userIdentityServerResponse != null) {
                dos.writeShort(0x5900);
                dos.writeShort(2 + userIdentityServerResponse.length);
                dos.writeShort(userIdentityServerResponse.length);
                dos.write(userIdentityServerResponse);
            }
        }

        @Override
        void parseUserIdentityAC(DataInputStream dis, int subitemLength, byte[] buf)
                throws IOException {
            if (subitemLength < 2)
                throw AAbort.invalidPDUParameterValue();
            if (subitemLength != 2 + dis.readShort())
                throw AAbort.invalidPDUParameterValue();
            userIdentityServerResponse = readBytes(dis, subitemLength - 2);
        }

        public static class PresentationContext {

            public final Result result;
            public final String transferSyntax;

            PresentationContext(Result result, String transferSyntax) {
                this.result = result;
                this.transferSyntax = transferSyntax;
            }

            int itemLength() {
                return 8 + transferSyntax.length();
            }

            void promptTo(Byte pcid, StringBuilder sb) {
                sb.append("  PresentationContext[pcid: ")
                        .append(pcid)
                        .append(", result: ")
                        .append(result)
                        .append(System.lineSeparator())
                        .append("    transfer-syntax: ");
                UIDUtils.promptTo(transferSyntax, sb)
                        .append(System.lineSeparator())
                        .append("  ]")
                        .append(System.lineSeparator());
            }

            void writeTo(DataOutputStream dos, byte[] buf) throws IOException {
                dos.writeShort(result.code() << 8);
                dos.writeShort(0x4000);
                writeLengthASCII(dos, transferSyntax, buf);
            }
        }

        public enum Result {
            ACCEPTANCE("0 - acceptance"),
            USER_REJECTION("1 - user-rejection"),
            NO_REASON("2 - no-reason" ),
            ABSTRACT_SYNTAX_NOT_SUPPORTED("3 - abstract-syntax-not-supported"),
            TRANSFER_SYNTAXES_NOT_SUPPORTED("4 - transfer-syntaxes-not-supported");

            final String prompt;

            Result(String prompt) {
                this.prompt = prompt;
            }

            @Override
            public String toString() {
                return prompt;
            }

            public int code() {
                return ordinal();
            }
        }
    }

    public enum RoleSelection {
        NONE(false, false),
        SCU(true, false),
        SCP(false, true),
        BOTH(true, true);

        public final boolean scu;
        public final boolean scp;

        RoleSelection(boolean scu, boolean scp) {
            this.scu = scu;
            this.scp = scp;
        }

        static RoleSelection of(boolean scu, boolean scp) {
            return scu
                    ? (scp ? BOTH : SCU)
                    : (scp ? SCP : NONE);
        }
    }

    public static class CommonExtendedNegotation {
        public final String serviceClass;
        private final List<String> relatedSOPClassList = new ArrayList<>();

        CommonExtendedNegotation(String serviceClass, String... relatedSOPClasses) {
            this.serviceClass = serviceClass;
            this.relatedSOPClassList.addAll(List.of(relatedSOPClasses));
        }

        public String[] relatedSOPClasses() {
            return relatedSOPClassList.toArray(new String[0]);
        }

        public Stream<String> relatedSOPClassesStream() {
            return relatedSOPClassList.stream();
        }

        int length() {
            return 4 + serviceClass.length() + relatedSOPClassListLength();
        }

        private int relatedSOPClassListLength() {
            return relatedSOPClassList.stream().mapToInt(s -> 2 + s.length()).sum();
        }

        void writeTo(DataOutputStream dos, byte[] buf) throws IOException {
            writeLengthASCII(dos, serviceClass, buf);
            dos.writeShort(relatedSOPClassListLength());
            for (String uid : relatedSOPClassList) {
                writeLengthASCII(dos, uid, buf);
            }
        }

        public void promptTo(String cuid, StringBuilder sb) {
            sb.append("  CommonExtendedNegotation[")
                    .append(System.lineSeparator())
                    .append("    sop-class: ");
            UIDUtils.promptTo(cuid, sb)
                    .append(System.lineSeparator())
                    .append("    service-class: ");
            UIDUtils.promptTo(serviceClass, sb)
                    .append(System.lineSeparator());
            relatedSOPClassList.forEach(s ->
                    UIDUtils.promptTo(s, sb.append("    related-general-sop-class: "))
                            .append(System.lineSeparator()));
            sb.append("  ]").append(System.lineSeparator());
        }
    }

    public static class UserIdentity {
        public static final int USERNAME = 1;
        public static final int USERNAME_PASSCODE = 2;
        public static final int KERBEROS = 3;
        public static final int SAML = 4;
        public static final int JWT = 5;

        public final int type;
        public final boolean positiveResponseRequested;
        private final byte[] primaryField;
        private final byte[] secondaryField;

        UserIdentity(int type, boolean positiveResponseRequested, byte[] primaryField, byte[] secondaryField) {
            this.type = type;
            this.positiveResponseRequested = positiveResponseRequested;
            this.primaryField = primaryField.clone();
            this.secondaryField = secondaryField.clone();
        }

        public byte[] primaryField() {
            return primaryField.clone();
        }

        public byte[] secondaryField() {
            return secondaryField.clone();
        }

        public boolean hasUsername() {
            return type == USERNAME || type == USERNAME_PASSCODE;
        }

        public String username() {
            if (!hasUsername()) {
                throw new IllegalStateException("type: " + type);
            }
            return new String(primaryField, StandardCharsets.UTF_8);
        }

        int itemLength() {
            return 6 + primaryField.length + secondaryField.length;
        }

        void writeTo(DataOutputStream out) throws IOException {
            out.write(type);
            out.writeBoolean(positiveResponseRequested);
            out.writeShort(primaryField.length);
            out.write(primaryField);
            out.writeShort(secondaryField.length);
            out.write(secondaryField);
        }

        void promptTo(StringBuilder sb) {
            sb.append("  UserIdentity[")
                    .append(System.lineSeparator())
                    .append("    type: ")
                    .append(type)
                    .append(System.lineSeparator());
            if (hasUsername()) {
                sb.append("    username: ").append(username());
            } else {
                sb.append("    primaryField: byte[")
                        .append(primaryField.length)
                        .append(']');
            }
            if (secondaryField.length > 0) {
                sb.append(System.lineSeparator());
                if (type == UserIdentity.USERNAME_PASSCODE) {
                    sb.append("    passcode: ");
                    for (int i = secondaryField.length; --i >= 0; ) {
                        sb.append('*');
                    }
                } else {
                    sb.append("    secondaryField: byte[")
                            .append(secondaryField.length)
                            .append(']');
                }
            }
            sb.append(System.lineSeparator())
                    .append("  ]")
                    .append(System.lineSeparator());
        }
    }

}
