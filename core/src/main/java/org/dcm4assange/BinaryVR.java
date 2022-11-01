package org.dcm4assange;

import org.dcm4assange.MemoryCache.DicomInput;
import org.dcm4assange.util.OptionalFloat;
import org.dcm4assange.util.TagUtils;
import org.dcm4assange.util.ToggleEndian;

import java.math.BigInteger;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Mar 2021
 */
enum BinaryVR implements VRType {
    AT(4, ToggleEndian.SHORT){
        @Override
        int intAt(DicomInput input, long pos) {
            return input.tagAt(pos);
        }

        @Override
        public OptionalLong longValue(DicomObject dcmobj, int index) {
            return OptionalLong.empty();
        }

        @Override
        public OptionalFloat floatValue(DicomObject dcmobj, int index) {
            return OptionalFloat.empty();
        }

        @Override
        public OptionalDouble doubleValue(DicomObject dcmobj, int index) {
            return OptionalDouble.empty();
        }

        @Override
        String stringAt(DicomInput input, long pos) {
            return TagUtils.toHexString(input.tagAt(pos));
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return TagUtils.toHexString(ByteOrder.LITTLE_ENDIAN.bytesToTag(b, pos));
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.tagToBytes(val, b, off);
        }
    },
    FD(8, ToggleEndian.LONG){
        @Override
        int intAt(DicomInput input, long pos) {
            return (int) doubleAt(input, pos);
        }

        @Override
        long longAt(DicomInput input, long pos) {
            return (long) doubleAt(input, pos);
        }

        @Override
        float floatAt(DicomInput input, long pos) {
            return (float) doubleAt(input, pos);
        }

        @Override
        double doubleAt(DicomInput input, long pos) {
            return Double.longBitsToDouble(input.longAt(pos));
        }

        @Override
        String stringAt(DicomInput input, long pos) {
            return Double.toString(doubleAt(input, pos));
        }

        @Override
        String bytesToString(byte[] b, int off) {
            return Double.toString(Double.longBitsToDouble(ByteOrder.LITTLE_ENDIAN.bytesToLong(b, off)));
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.longToBytes(Double.doubleToRawLongBits(val), b, off);
        }
    },
    FL(4, ToggleEndian.INT){
        @Override
        int intAt(DicomInput input, long pos) {
            return (int) floatAt(input, pos);
        }

        @Override
        long longAt(DicomInput input, long pos) {
            return (long) floatAt(input, pos);
        }

        @Override
        float floatAt(DicomInput input, long pos) {
            return Float.intBitsToFloat(input.intAt(pos));
        }

        @Override
        String stringAt(DicomInput input, long pos) {
            return Float.toString(floatAt(input, pos));
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return Float.toString(Float.intBitsToFloat(ByteOrder.LITTLE_ENDIAN.bytesToInt(b, pos)));
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.intToBytes(Float.floatToRawIntBits(val), b, off);
        }
    },
    OB(1, null){
        @Override
        int intAt(DicomInput input, long pos) {
            return input.byteAt(pos);
        }
    },
    SL(4, ToggleEndian.INT),
    SS(2, ToggleEndian.SHORT){
        @Override
        int intAt(DicomInput input, long pos) {
            return input.shortAt(pos);
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return Short.toString(ByteOrder.LITTLE_ENDIAN.bytesToShort(b, pos));
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.shortToBytes(val, b, off);
        }
    },
    SV(8, ToggleEndian.LONG){
        @Override
        int intAt(DicomInput input, long pos) {
            return (int) input.longAt(pos);
        }

        @Override
        long longAt(DicomInput input, long pos) {
            return input.longAt(pos);
        }

        @Override
        float floatAt(DicomInput input, long pos) {
            return input.longAt(pos);
        }

        @Override
        double doubleAt(DicomInput input, long pos) {
            return input.longAt(pos);
        }

        @Override
        String stringAt(DicomInput input, long pos) {
            return Long.toString(input.longAt(pos));
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return Long.toString(ByteOrder.LITTLE_ENDIAN.bytesToLong(b, pos));
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.longToBytes(val, b, off);
        }
    },
    UL(4, ToggleEndian.INT){
        @Override
        long longAt(DicomInput input, long pos) {
            return input.intAt(pos) & 0xffffffffL;
        }

        @Override
        String stringAt(DicomInput input, long pos) {
            return Integer.toUnsignedString(input.intAt(pos));
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return Integer.toUnsignedString(ByteOrder.LITTLE_ENDIAN.bytesToInt(b, pos));
        }
    },
    US(2, ToggleEndian.SHORT) {
        @Override
        int intAt(DicomInput input, long pos) {
            return input.ushortAt(pos);
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return Integer.toString(ByteOrder.LITTLE_ENDIAN.bytesToShort(b, pos) & 0xffff);
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.shortToBytes(val, b, off);
        }
    },
    UV(8, ToggleEndian.LONG){
        @Override
        int intAt(DicomInput input, long pos) {
            return (int) input.longAt(pos);
        }

        @Override
        long longAt(DicomInput input, long pos) {
            return input.longAt(pos);
        }

        @Override
        float floatAt(DicomInput input, long pos) {
            long l = input.longAt(pos);
            return l < 0 ? toUnsignedBigInteger(l).floatValue() : l;
        }

        @Override
        double doubleAt(DicomInput input, long pos) {
            long l = input.longAt(pos);
            return l < 0 ? toUnsignedBigInteger(l).doubleValue() : l;
        }

        @Override
        String stringAt(DicomInput input, long pos) {
            return Long.toUnsignedString(input.longAt(pos));
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return Long.toUnsignedString(ByteOrder.LITTLE_ENDIAN.bytesToLong(b, pos));
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.longToBytes(val & 0xffffffffL, b, off);
        }
    };

    private static BigInteger toUnsignedBigInteger(long l) {
        return new BigInteger(1, new byte[]{
                (byte) (l >> 56),
                (byte) (l >> 48),
                (byte) (l >> 40),
                (byte) (l >> 32),
                (byte) (l >> 24),
                (byte) (l >> 16),
                (byte) (l >> 8),
                (byte) l});
    }

    final int bytes;
    final ToggleEndian toggleEndian;

    BinaryVR(int bytes, ToggleEndian toggleEndian) {
        this.bytes = bytes;
        this.toggleEndian = toggleEndian;
    }

    @Override
    public ToggleEndian toggleEndian() {
        return toggleEndian;
    }

    @Override
    public OptionalInt intValue(DicomObject dcmobj, int index) {
        long header = dcmobj.getHeader(index);
        return dcmobj.header2valueLength(header) >= bytes
                ? OptionalInt.of(intAt(dcmobj.dicomInput, DicomObject.header2valuePosition(header)))
                : OptionalInt.empty();
    }

    @Override
    public OptionalLong longValue(DicomObject dcmobj, int index) {
        long header = dcmobj.getHeader(index);
        return dcmobj.header2valueLength(header) >= bytes
                ? OptionalLong.of(longAt(dcmobj.dicomInput, DicomObject.header2valuePosition(header)))
                : OptionalLong.empty();
    }

    @Override
    public OptionalFloat floatValue(DicomObject dcmobj, int index) {
        long header = dcmobj.getHeader(index);
        return dcmobj.header2valueLength(header) >= bytes
                ? OptionalFloat.of(floatAt(dcmobj.dicomInput, DicomObject.header2valuePosition(header)))
                : OptionalFloat.empty();
    }

    @Override
    public OptionalDouble doubleValue(DicomObject dcmobj, int index) {
        long header = dcmobj.getHeader(index);
        return dcmobj.header2valueLength(header) >= bytes
                ? OptionalDouble.of(doubleAt(dcmobj.dicomInput, DicomObject.header2valuePosition(header)))
                : OptionalDouble.empty();
    }

    @Override
    public Object valueOf(int[] vals) {
        if (vals.length == 0) {
            return ByteOrder.EMPTY_BYTES;
        }
        byte[] b = new byte[vals.length * bytes];
        for (int i = 0; i < vals.length; i++) {
            intToBytes(vals[i], b, i * bytes);
        }
        return b;
    }

    int intAt(DicomInput input, long pos) {
        return input.intAt(pos);
    }

    long longAt(DicomInput input, long pos) {
        return intAt(input, pos);
    }

    float floatAt(DicomInput input, long pos) {
        return intAt(input, pos);
    }

    double doubleAt(DicomInput input, long pos) {
        return floatAt(input, pos);
    }

    String stringAt(DicomInput input, long pos) {
        return Integer.toString(intAt(input, pos));
    }

    String bytesToString(byte[] b, int pos) {
        return Integer.toString(ByteOrder.LITTLE_ENDIAN.bytesToInt(b, pos));
    }

    void intToBytes(int val, byte[] b, int off) {
        ByteOrder.LITTLE_ENDIAN.intToBytes(val, b, off);
    }

    @Override
    public StringBuilder promptValueTo(byte[] b, StringBuilder sb, int maxLength) {
        sb.append(" [");
        int n = b.length / bytes;
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append('\\');
            }
            sb.append(bytesToString(b, i * bytes));
            if (sb.length() >= maxLength) {
                sb.setLength(maxLength);
                return sb;
            }
        }
        return sb.append(']');
    }

    @Override
    public StringBuilder promptValueTo(DicomInput input, long valpos, int vallen, DicomObject dicomObject,
                                       StringBuilder appendTo, int maxLength) {
        appendTo.append(" [");
        int n = vallen / bytes;
        for (int i = 0; i < n; i++, valpos += bytes) {
            if (i > 0) {
                appendTo.append('\\');
            }
            appendTo.append(stringAt(input, valpos));
            if (appendTo.length() >= maxLength) {
                appendTo.setLength(maxLength);
                return appendTo;
            }
        }
        return appendTo.append(']');
    }
}
