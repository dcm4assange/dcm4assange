package org.dcm4assange.net;

import org.dcm4assange.ByteOrder;
import org.dcm4assange.Implementation;
import org.dcm4assange.UID;
import org.dcm4assange.conf.model.Connection;
import org.dcm4assange.util.ArrayUtils;
import org.dcm4assange.util.UIDUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.dcm4assange.net.Utils.*;
import static org.dcm4assange.util.StringUtils.EMPTY_STRINGS;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Nov 2019
 */
public abstract class AAssociate {

    private int protocolVersion = 1;
    private String calledAETitle;
    private String callingAETitle;
    private String applicationContextName;
    private int maxPDULength = -1;
    private String implClassUID;
    private String implVersionName;
    private int asyncOpsWindow = -1;
    private final Map<String, RoleSelection> roleSelectionMap = new LinkedHashMap<>();
    private final Map<String, byte[]> extNegMap = new LinkedHashMap<>();

    AAssociate() {
    }

    AAssociate(String callingAETitle, String calledAETitle) {
        setCalledAETitle(calledAETitle);
        setCallingAETitle(callingAETitle);
        applicationContextName = UID.DICOMApplicationContext;
        implClassUID = Implementation.CLASS_UID;
        implVersionName = Implementation.VERSION_NAME;
        maxPDULength = 0;
    }

    public String getCalledAETitle() {
        return calledAETitle;
    }

    public void setCalledAETitle(String calledAETitle) {
        this.calledAETitle = checkLength(calledAETitle, 16);
    }

    public String getCallingAETitle() {
        return callingAETitle;
    }

    public void setCallingAETitle(String callingAETitle) {
        this.callingAETitle = checkLength(callingAETitle, 16);
    }

    public String getApplicationContextName() {
        return applicationContextName;
    }

    public void setApplicationContextName(String applicationContextName) {
        Objects.requireNonNull(applicationContextName);
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
        Objects.requireNonNull(implClassUID);
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
        Objects.requireNonNull(cuid, "cuid");
        Objects.requireNonNull(roleSelection, "roleSelection");
        roleSelectionMap.put(cuid, roleSelection);
    }

    public RoleSelection getRoleSelection(String cuid) {
        return roleSelectionMap.get(cuid);
    }

    public void putExtendedNegotation(String cuid, byte[] extNeg) {
        Objects.requireNonNull(cuid, "cuid");
        Objects.requireNonNull(extNeg, "extNeg");
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
        int l = (asyncOpsWindow != -1 ? 24 : 16)
                + implClassUID.length()
                + implVersionName.length()
                + roleSelectionMap.size() * 8
                + extNegMap.size() * 6;
        for (String cuid : roleSelectionMap.keySet()) l += cuid.length();
        for (Map.Entry<String, byte[]> e : extNegMap.entrySet()) l += e.getKey().length() + e.getValue().length;
        return l;
    }

    abstract void writeTo(DataOutputStream out) throws IOException;

    void writeTo(DataOutputStream out, int pdutype) throws IOException {
        synchronized (out) {
            out.write(pdutype);
            out.write(0);
            out.writeInt(pduLength());
            out.writeShort(protocolVersion);
            out.write(0);
            out.write(0);
            writePaddedASCII(out, calledAETitle, 16);
            writePaddedASCII(out, callingAETitle, 16);
            writeNBytes(out, 0, 32);
            out.write(0x10);
            out.write(0);
            writeLengthASCII(out, applicationContextName);
            writePresentationContextTo(out);
            out.write(0x50);
            out.write(0);
            out.writeShort(userItemLength());
            out.write(0x51);
            out.write(0);
            out.write(0);
            out.write(4);
            out.writeInt(maxPDULength);
            out.write(0x52);
            out.write(0);
            writeLengthASCII(out, implClassUID);
            out.write(0x55);
            out.write(0);
            writeLengthASCII(out, implVersionName);
            if (asyncOpsWindow != -1) {
                out.write(0x53);
                out.write(0);
                out.write(0);
                out.write(4);
                out.writeInt(asyncOpsWindow);
            }
            for (Map.Entry<String, RoleSelection> e : roleSelectionMap.entrySet()) {
                out.write(0x54);
                out.write(0);
                out.writeShort(4 + e.getKey().length());
                writeLengthASCII(out,  e.getKey());
                out.writeBoolean(e.getValue().scu);
                out.writeBoolean(e.getValue().scp);
            }
            for (Map.Entry<String, byte[]> e : extNegMap.entrySet()) {
                out.write(0x56);
                out.write(0);
                out.writeShort(2 + e.getKey().length() + e.getValue().length);
                writeLengthASCII(out, e.getKey());
                out.write(e.getValue());
            }
            writeCommonExtendedNegotationTo(out);
            writeUserIdentityTo(out);
            out.flush();
        }
    }

    abstract void writePresentationContextTo(DataOutputStream out) throws IOException;

    void writeCommonExtendedNegotationTo(DataOutputStream out) throws IOException {
    }

    abstract void writeUserIdentityTo(DataOutputStream out) throws IOException;

    void parse(DataInputStream in, int pduLength) throws IOException {
        int remaining = pduLength - 68;
        if (remaining < 0)
            throw AAbort.invalidPDUParameterValue();
        protocolVersion = in.readUnsignedShort();
        in.readUnsignedByte();
        in.readUnsignedByte();
        calledAETitle = readASCII(in, 16).trim();
        callingAETitle = readASCII(in, 16).trim();
        in.skipNBytes(32);
        while (remaining > 0) {
            if ((remaining -= 4) < 0)
                throw AAbort.invalidPDUParameterValue();
            int itemType = in.readUnsignedByte();
            in.readUnsignedByte();
            int itemLength = in.readUnsignedShort();
            if ((remaining -= itemLength) < 0)
                throw AAbort.invalidPDUParameterValue();
            switch (itemType) {
                case 0x10 -> applicationContextName = readASCII(in, itemLength);
                case 0x20 -> parsePresentationContextRQ(in, itemLength);
                case 0x21 -> parsePresentationContextAC(in, itemLength);
                case 0x50 -> parseUserItems(in, itemLength);
                default -> throw AAbort.unexpectedOrUnrecognizedItem(itemType);
            }
        }
        if (applicationContextName == null) {
            throw AAbort.invalidPDUParameterValue();
        }
        if (maxPDULength < 0) {
            throw AAbort.invalidPDUParameterValue();
        }
        if (implClassUID == null) {
            throw AAbort.invalidPDUParameterValue();
        }
    }

    void parsePresentationContextRQ(DataInputStream in, int itemLength)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    void parsePresentationContextAC(DataInputStream in, int itemLength)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    private void parseUserItems(DataInputStream in, int itemLength)
            throws IOException {
        int itemRemaining = itemLength;
        while (itemRemaining > 0) {
            if ((itemRemaining -= 4) < 0)
                throw AAbort.invalidPDUParameterValue();
            int subitemType = in.readUnsignedByte();
            in.readUnsignedByte();
            int subitemLength = in.readUnsignedShort();
            if ((itemRemaining -= subitemLength) < 0)
                throw AAbort.invalidPDUParameterValue();
            switch (subitemType) {
                case 0x51 -> parseMaxPDULength(in, subitemLength);
                case 0x52 -> implClassUID = readASCII(in, subitemLength);
                case 0x53 -> parseOpsWindow(in, subitemLength);
                case 0x54 -> parseRoleSelections(in, subitemLength);
                case 0x55 -> implVersionName = readASCII(in, subitemLength);
                case 0x56 -> parseExtendedNegotations(in, subitemLength);
                case 0x57 -> parseCommonExtendedNegotations(in, subitemLength);
                case 0x58 -> parseUserIdentityRQ(in, subitemLength);
                case 0x59 -> parseUserIdentityAC(in, subitemLength);
                default -> throw AAbort.unexpectedOrUnrecognizedItem(subitemType);
            }
        }
    }

    private void parseMaxPDULength(DataInputStream in, int subitemLength)
            throws IOException {
        if (subitemLength != 4)
            throw AAbort.invalidPDUParameterValue();
        maxPDULength = in.readInt();
    }

    private void parseOpsWindow(DataInputStream in, int subitemLength)
            throws IOException {
        if (subitemLength != 4)
            throw AAbort.invalidPDUParameterValue();
        asyncOpsWindow = in.readInt();
    }

    private void parseRoleSelections(DataInputStream in, int subitemLength)
            throws IOException {
        if (subitemLength < 4)
            throw AAbort.invalidPDUParameterValue();
        int uidLen = in.readUnsignedShort();
        if (subitemLength != 4 + uidLen)
            throw AAbort.invalidPDUParameterValue();
        roleSelectionMap.put(
                readASCII(in, uidLen),
                RoleSelection.of(in.readBoolean(), in.readBoolean()));
    }

    private void parseExtendedNegotations(DataInputStream in, int subitemLength)
            throws IOException {
        if (subitemLength < 2)
            throw AAbort.invalidPDUParameterValue();
        int uidLen = in.readUnsignedShort();
        if (subitemLength < 2 + uidLen)
            throw AAbort.invalidPDUParameterValue();
        extNegMap.put(
                readASCII(in, uidLen),
                in.readNBytes(subitemLength - (2 + uidLen)));
    }

    void parseCommonExtendedNegotations(DataInputStream in, int subitemLength)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    void parseUserIdentityRQ(DataInputStream in, int subitemLength)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    void parseUserIdentityAC(DataInputStream in, int subitemLength)
            throws IOException {
        throw AAbort.unexpectedPDUParameter();
    }

    public void setMaxPDULength(Connection conn) {
        setMaxPDULength(conn.getReceivePDULength());
    }

    public static class RQ extends AAssociate {

        private final Map<Byte, PresentationContext> pcs = new LinkedHashMap<>();
        private final Map<String, CommonExtendedNegotation> commonExtNegMap = new LinkedHashMap<>();
        private UserIdentity userIdentity;

        private RQ() {}

        public RQ(String callingAETitle, String calledAETitle) {
            super(callingAETitle, calledAETitle);
        }

        public static RQ readFrom(DataInputStream in, int pduLength) throws IOException {
            RQ rq = new RQ();
            rq.parse(in, pduLength);
            return rq;
        }

        public void putPresentationContext(Byte id, String abstractSyntax, String... transferSyntaxes) {
            pcs.put(id, new PresentationContext(abstractSyntax, transferSyntaxes.clone()));
        }

        public Byte findOrAddPresentationContext(String abstractSyntax, String transferSyntax) {
            return pcidsFor(abstractSyntax, transferSyntax).findFirst().orElseGet(
                    () -> addPresentationContext(abstractSyntax, transferSyntax));
        }

        public Byte addPresentationContext(String abstractSyntax, String... transferSyntax) {
            int start = pcs.size() * 2 + 1;
            Byte pcid = IntStream.range(0, 128)
                    .mapToObj(i -> (byte) (start + i * 2))
                    .filter(((Predicate<Byte>) pcs.keySet()::contains).negate())
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalStateException("Maximal number (128) of Presentation Contexts reached"));
            pcs.put(pcid, new PresentationContext(abstractSyntax, transferSyntax.clone()));
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
            putCommonExtendedNegotation(cuid, serviceClass, relatedSOPClasses, new byte[0]);
        }

        public void putCommonExtendedNegotation(String cuid, String serviceClass, String[] relatedSOPClasses,
                                                byte[] reserved) {
            commonExtNegMap.put(cuid, new CommonExtendedNegotation(serviceClass, relatedSOPClasses, reserved));
        }

        public CommonExtendedNegotation getCommonExtendedNegotation(String cuid) {
            return commonExtNegMap.get(cuid);
        }

        public UserIdentity getUserIdentity() {
            return userIdentity;
        }

        public void setUserIdentity(int type, boolean positiveResponseRequested, byte[] primaryField) {
            this.userIdentity = new UserIdentity(type, positiveResponseRequested, primaryField.clone(), ByteOrder.EMPTY_BYTES);
        }

        public void setUserIdentity(int type, boolean positiveResponseRequested, byte[] primaryField,
                byte[] secondaryField) {
            this.userIdentity = new UserIdentity(type, positiveResponseRequested, primaryField.clone(), secondaryField.clone());
        }

        public void unsetUserIdentity() {
            this.userIdentity = null;
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
            int l = pcs.size() * 4;
            for (PresentationContext s : pcs.values()) l += s.itemLength();
            return l;
        }

        @Override
        int userItemLength() {
            int l = super.userItemLength() + commonExtNegMap.size() * 6;
            for (Map.Entry<String, CommonExtendedNegotation> e : commonExtNegMap.entrySet())
                l += e.getKey().length() + e.getValue().length();
            if (userIdentity != null) l += 4 + userIdentity.itemLength();
            return l;
        }

        @Override
        void writeTo(DataOutputStream out) throws IOException {
            writeTo(out, 0x01);
        }

        @Override
        void writePresentationContextTo(DataOutputStream out) throws IOException {
            for (Map.Entry<Byte, RQ.PresentationContext> e : pcs.entrySet()) {
                out.write(0x20);
                out.write(0);
                out.writeShort(e.getValue().itemLength());
                out.writeShort(e.getKey().intValue() << 8);
                e.getValue().writeTo(out);
            }
        }

        @Override
        void parsePresentationContextRQ(DataInputStream in, int itemLength)
                throws IOException {
            int remaining = itemLength - 4;
            if (remaining < 0)
                throw AAbort.invalidPDUParameterValue();
            byte pcid = (byte) in.readUnsignedByte();
            in.readUnsignedByte();
            in.readUnsignedByte();
            in.readUnsignedByte();
            String abstractSyntax = null;
            List<String> transferSyntaxes = new ArrayList<>();
            int subitemType, subitemLength;
            while (remaining > 0) {
                if ((remaining -= 4) < 0)
                    throw AAbort.invalidPDUParameterValue();
                subitemType = in.readUnsignedByte();
                in.readUnsignedByte();
                subitemLength = in.readUnsignedShort();
                if ((remaining -= subitemLength) < 0)
                    throw AAbort.invalidPDUParameterValue();
                switch (subitemType) {
                    case 0x30 -> abstractSyntax = readASCII(in, subitemLength);
                    case 0x40 -> transferSyntaxes.add(readASCII(in, subitemLength));
                    default -> throw AAbort.unexpectedOrUnrecognizedItem(subitemType);
                }
            }
            if (abstractSyntax == null) {
                throw AAbort.invalidPDUParameterValue();
            }
            if (transferSyntaxes.isEmpty()) {
                throw AAbort.invalidPDUParameterValue();
            }
            pcs.put(pcid, new PresentationContext(abstractSyntax, transferSyntaxes.toArray(EMPTY_STRINGS)));
        }

        @Override
        void writeCommonExtendedNegotationTo(DataOutputStream out) throws IOException {
            for (Map.Entry<String, CommonExtendedNegotation> e : commonExtNegMap.entrySet()) {
                out.write(0x57);
                out.write(0);
                out.writeShort(2 + e.getKey().length() + e.getValue().length());
                writeLengthASCII(out, e.getKey());
                e.getValue().writeTo(out);
            }
        }

        @Override
        void parseCommonExtendedNegotations(DataInputStream in, int subitemLength)
                throws IOException {
            int remaining = subitemLength - 6;
            if (remaining < 0)
                throw AAbort.invalidPDUParameterValue();
            int length = in.readUnsignedShort();
            if ((remaining -= length) < 0)
                throw AAbort.invalidPDUParameterValue();
            String cuid = readASCII(in, length);
            length = in.readUnsignedShort();
            if ((remaining -= length) < 0)
                throw AAbort.invalidPDUParameterValue();
            String serviceClass = readASCII(in, length);
            length = in.readUnsignedShort();
            int reservedLength = remaining - length;
            if (reservedLength < 0)
                throw AAbort.invalidPDUParameterValue();
            remaining -= reservedLength;
            List<String> relatedSOPClasses = new ArrayList<>();
            while (remaining > 0) {
                if ((remaining -= 2) < 0)
                    throw AAbort.invalidPDUParameterValue();
                length = in.readUnsignedShort();
                if ((remaining -= length) < 0)
                    throw AAbort.invalidPDUParameterValue();
                relatedSOPClasses.add(readASCII(in, length));
            }
            putCommonExtendedNegotation(cuid,
                    serviceClass,
                    relatedSOPClasses.toArray(EMPTY_STRINGS),
                    in.readNBytes(reservedLength));
        }

        @Override
        void writeUserIdentityTo(DataOutputStream out) throws IOException {
            if (userIdentity != null) {
                out.write(0x58);
                out.write(0);
                out.writeShort(userIdentity.itemLength());
                userIdentity.writeTo(out);
            }
        }

        @Override
        void parseUserIdentityRQ(DataInputStream in, int subitemLength)
                throws IOException {
            int remaining = subitemLength - 6;
            if (remaining < 0)
                throw AAbort.invalidPDUParameterValue();
            int type = in.readUnsignedByte();
            boolean responseRequested = in.readBoolean();
            int length = in.readUnsignedShort();
            if ((remaining -= length) < 0)
                throw AAbort.invalidPDUParameterValue();
            byte[] primaryField = in.readNBytes(length);
            length = in.readUnsignedShort();
            if (remaining != length)
                throw AAbort.invalidPDUParameterValue();
            byte[] secondaryField = in.readNBytes(length);
            try {
                this.userIdentity = new UserIdentity(type, responseRequested, primaryField, secondaryField);
            } catch (IllegalArgumentException e) {
                throw AAbort.invalidPDUParameterValue();
            }
        }

        public void setAsyncOpsWindow(Connection conn) {
            if (conn.isAsynchronousMode()) {
                setAsyncOpsWindow(conn.getMaxOpsInvoked(), conn.getMaxOpsPerformed());
            }
        }

        public static class PresentationContext {

            final String abstractSyntax;
            final String[] transferSyntaxes;

            PresentationContext(String abstractSyntax, String... transferSyntaxes) {
                this.abstractSyntax = abstractSyntax;
                this.transferSyntaxes = transferSyntaxes;
            }

            public String abstractSyntax() {
                return abstractSyntax;
            }

            public String[] transferSyntaxes() {
                return transferSyntaxes.clone();
            }

            public boolean equalsAbstractSyntax(String abstractSyntax) {
                return this.abstractSyntax.equals(abstractSyntax);
            }

            public boolean containsTransferSyntax(String transferSyntax) {
                return ArrayUtils.contains(transferSyntaxes, transferSyntax);
            }

            public boolean matches(String abstractSyntax, String transferSyntax) {
                return equalsAbstractSyntax(abstractSyntax) && containsTransferSyntax(transferSyntax);
            }

            int itemLength() {
                int l = 8 + abstractSyntax.length() + transferSyntaxes.length * 4;
                for (String s : transferSyntaxes) l += s.length();
                return l;
            }

            void writeTo(DataOutputStream out) throws IOException {
                out.write(0);
                out.write(0);
                out.write(0x30);
                out.write(0);
                writeLengthASCII(out, abstractSyntax);
                for (String uid : transferSyntaxes) {
                    out.write(0x40);
                    out.write(0);
                    writeLengthASCII(out, uid);
                }
            }

            void promptTo(Byte pcid, StringBuilder sb) {
                sb.append("  PresentationContext[pcid: ")
                        .append(pcid)
                        .append(System.lineSeparator())
                        .append("    abstract-syntax: ");
                UIDUtils.promptTo(abstractSyntax, sb)
                        .append(System.lineSeparator());
                for (String ts : transferSyntaxes) {
                    UIDUtils.promptTo(ts, sb.append("    transfer-syntax: "))
                            .append(System.lineSeparator());
                }
                sb.append("  ]").append(System.lineSeparator());
            }
        }
    }

    public static class AC extends AAssociate {

        private final Map<Byte, PresentationContext> pcs = new LinkedHashMap<>();
        private byte[] userIdentityServerResponse;

        private AC() {}

        public AC(String callingAETitle, String calledAETitle) {
            super(callingAETitle, calledAETitle);
        }

        public static AC readFrom(DataInputStream in, int pduLength) throws IOException {
            AC ac = new AC();
            ac.parse(in, pduLength);
            return ac;
        }

        public void putPresentationContext(Byte id, Result result, String transferSyntax) {
            pcs.put(id, new PresentationContext(result, transferSyntax));
        }

        public PresentationContext getPresentationContext(Byte id) {
            return pcs.get(id);
        }

        public Optional<byte[]> getUserIdentityServerResponse() {
            return Optional.ofNullable(clone(userIdentityServerResponse));
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
            int l = pcs.size() * 4;
            for (PresentationContext pc : pcs.values()) l += pc.itemLength();
            return l;
        }

        @Override
        int userItemLength() {
            int l = super.userItemLength();
            if (userIdentityServerResponse != null) l += 6 + userIdentityServerResponse.length;
            return l;
        }

        @Override
        void writeTo(DataOutputStream out) throws IOException {
            writeTo(out, 0x02);
        }

        @Override
        void writePresentationContextTo(DataOutputStream out) throws IOException {
            for (Map.Entry<Byte, PresentationContext> e : pcs.entrySet()) {
                out.write(0x21);
                out.write(0);
                out.writeShort(e.getValue().itemLength());
                out.write(e.getKey());
                out.write(0);
                e.getValue().writeTo(out);
            }
        }

        @Override
        void parsePresentationContextAC(DataInputStream in, int itemLength)
                throws IOException {
            int remaining = itemLength - 4;
            if (remaining < 0)
                throw AAbort.invalidPDUParameterValue();
            byte pcid = (byte) in.readUnsignedByte();
            in.readUnsignedByte();
            Result result = switch (in.readUnsignedByte()) {
                    case 0 -> Result.ACCEPTANCE;
                    case 1 -> Result.USER_REJECTION;
                    case 2 -> Result.NO_REASON;
                    case 3 -> Result.ABSTRACT_SYNTAX_NOT_SUPPORTED;
                    case 4 -> Result.TRANSFER_SYNTAXES_NOT_SUPPORTED;
                    default -> throw AAbort.invalidPDUParameterValue();
                };
            in.readUnsignedByte();
            int subitemType, subitemLength;
            String transferSyntax = null;
            while (remaining > 0) {
                if ((remaining -= 4) < 0)
                    throw AAbort.invalidPDUParameterValue();
                subitemType = in.readUnsignedByte();
                in.readUnsignedByte();
                subitemLength = in.readUnsignedShort();
                if ((remaining -= subitemLength) < 0)
                    throw AAbort.invalidPDUParameterValue();
                transferSyntax = switch (subitemType) {
                    case 0x40 -> readASCII(in, subitemLength);
                    default -> throw AAbort.unexpectedOrUnrecognizedItem(subitemType);
                };
            }
            if (transferSyntax == null) {
                throw AAbort.invalidPDUParameterValue();
            }
            putPresentationContext(pcid, result, transferSyntax);
        }

        @Override
        void writeUserIdentityTo(DataOutputStream out) throws IOException {
            if (userIdentityServerResponse != null) {
                out.write(0x59);
                out.write(0);
                out.writeShort(2 + userIdentityServerResponse.length);
                out.writeShort(userIdentityServerResponse.length);
                out.write(userIdentityServerResponse);
            }
        }

        @Override
        void parseUserIdentityAC(DataInputStream in, int subitemLength)
                throws IOException {
            if (subitemLength < 2)
                throw AAbort.invalidPDUParameterValue();
            if (subitemLength != 2 + in.readUnsignedShort())
                throw AAbort.invalidPDUParameterValue();
            userIdentityServerResponse = in.readNBytes(subitemLength - 2);
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

            void writeTo(DataOutputStream out) throws IOException {
                out.write(result.code());
                out.write(0);
                out.write(0x40);
                out.write(0);
                writeLengthASCII(out, transferSyntax);
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
        final String serviceClass;
        final String[] relatedSOPClasses;
        final byte[] reserved;

        CommonExtendedNegotation(String serviceClass, String[] relatedSOPClasses, byte[] reserved) {
            this.serviceClass = serviceClass;
            this.relatedSOPClasses = relatedSOPClasses;
            this.reserved = reserved.clone();
        }

        int length() {
            return 4 + serviceClass.length() + relatedSOPClassesLength() + reserved.length;
        }

        private int relatedSOPClassesLength() {
            int l = relatedSOPClasses.length * 2;
            for (String s : relatedSOPClasses) l += s.length();
            return l;
        }

        DimseRQHandler selectDimseRQHandler(Map<String, DimseRQHandler> map) {
            for (String relatedSOPClass : relatedSOPClasses) {
                DimseRQHandler handler = map.get(relatedSOPClass);
                if (handler != null)
                    return handler;
            }
            return map.get(serviceClass);
        }

        void writeTo(DataOutputStream out) throws IOException {
            writeLengthASCII(out, serviceClass);
            out.writeShort(relatedSOPClassesLength());
            for (String uid : relatedSOPClasses) {
                writeLengthASCII(out, uid);
            }
            out.write(reserved);
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
            for (String s : relatedSOPClasses)
                UIDUtils.promptTo(s, sb.append("    related-general-sop-class: "))
                        .append(System.lineSeparator());
            if (reserved.length > 0)
                sb.append("    reserved: byte[")
                        .append(reserved.length)
                        .append("]")
                        .append(System.lineSeparator());
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
            if (type <= 0 || type > 5) {
                throw new IllegalArgumentException("type: " + type);
            }
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
