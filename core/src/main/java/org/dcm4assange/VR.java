package org.dcm4assange;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2021
 */
public enum VR {
    AE(0x4145, true, StringVR.ASCII, 0x20),
    AS(0x4153, true, StringVR.ASCII, 0x20),
    AT(0x4154, true, BinaryVR.AT, 0),
    CS(0x4353, true, StringVR.ASCII, 0x20),
    DA(0x4441, true, StringVR.ASCII, 0x20),
    DS(0x4453, true, StringVR.DS, 0x20),
    DT(0x4454, true, StringVR.ASCII, 0x20),
    FD(0x4644, true, BinaryVR.FD, 0),
    FL(0x464c, true, BinaryVR.FL, 0),
    IS(0x4953, true, StringVR.IS, 0x20),
    LO(0x4c4f, true, StringVR.STRING, 0x20),
    LT(0x4c54, true, StringVR.TEXT, 0x20),
    OB(0x4f42, false, BinaryVR.OB, 0),
    OD(0x4f44, false, BinaryVR.FD, 0),
    OF(0x4f46, false, BinaryVR.FL, 0),
    OL(0x4f4c, false, BinaryVR.SL, 0),
    OV(0x4f56, false, BinaryVR.SV, 0),
    OW(0x4f57, false, BinaryVR.SS, 0),
    PN(0x504e, true, StringVR.PN, 0x20),
    SH(0x5348, true, StringVR.STRING, 0x20),
    SL(0x534c, true, BinaryVR.SL, 0),
    SQ(0x5351, false, VRType.SQ, 0),
    SS(0x5353, true, BinaryVR.SS, 0),
    ST(0x5354, true, StringVR.TEXT, 0x20),
    SV(0x5356, false, BinaryVR.SV, 0),
    TM(0x544d, true, StringVR.ASCII, 0x20),
    UC(0x5543, false, StringVR.UC, 0x20),
    UI(0x5549, true, StringVR.UI, 0),
    UL(0x554c, true, BinaryVR.UL, 0),
    UN(0x554e, false, VRType.UN, 0),
    UR(0x5552, false, StringVR.UR, 0x20),
    US(0x5553, true, BinaryVR.US, 0),
    UT(0x5554, false, StringVR.TEXT, 0x20),
    UV(0x5556, false, BinaryVR.UV, 0);

    final int code;
    final boolean evr8;
    final VRType type;
    final int paddingByte;
    private static final VR[] VALUE_OF = new VR[1024];
    static {
        for (VR vr : VR.values())
            VALUE_OF[indexOf(vr.code)] = vr;
    }

    VR(int code, boolean evr8, VRType type, int paddingByte) {
        this.code = code;
        this.evr8 = evr8;
        this.type = type;
        this.paddingByte = paddingByte;
    }

    static VR of(int code) {
        return ((code ^ 0x4040) & 0xffffe0e0) == 0 ? VALUE_OF[indexOf(code)] : null;
    }

    private static int indexOf(int code) {
        return ((code & 0x1f00) >> 3) | (code & 0x1f);
    }

    public static VR fromHeader(long header) {
        int index = ((int) (header >>> 56)) & 0x3f;
        return index > 0 ? values()[index - 1] : null;
    }

    long toHeader() {
        return (long)(ordinal() + 1) << 56;
    }
}
