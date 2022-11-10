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
        public OptionalLong longValue(DicomObject dcmobj, int i, int index) {
            return OptionalLong.empty();
        }

        @Override
        public OptionalFloat floatValue(DicomObject dcmobj, int i, int index) {
            return OptionalFloat.empty();
        }

        @Override
        public OptionalDouble doubleValue(DicomObject dcmobj, int i, int index) {
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

        @Override
        int bytesToInt(byte[] b, int pos) {
            return ByteOrder.LITTLE_ENDIAN.bytesToTag(b, pos);
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
        int bytesToInt(byte[] b, int pos) {
            return (int) bytesToDouble(b, pos);
        }

        @Override
        long bytesToLong(byte[] b, int pos) {
            return (long) bytesToDouble(b, pos);
        }

        @Override
        float bytesToFloat(byte[] b, int pos) {
            return (float) bytesToDouble(b, pos);
        }

        @Override
        double bytesToDouble(byte[] b, int pos) {
            return Double.longBitsToDouble(ByteOrder.LITTLE_ENDIAN.bytesToLong(b, pos));
        }

        @Override
        String bytesToString(byte[] b, int off) {
            return Double.toString(Double.longBitsToDouble(ByteOrder.LITTLE_ENDIAN.bytesToLong(b, off)));
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            doubleToBytes(val, b, off);
        }

        void longToBytes(long val, byte[] b, int off) {
            doubleToBytes(val, b, off);
        }

        void floatToBytes(float val, byte[] b, int off) {
            doubleToBytes(val, b, off);
        }

        void doubleToBytes(double val, byte[] b, int off) {
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
        int bytesToInt(byte[] b, int pos) {
            return (int) bytesToFloat(b, pos);
        }

        @Override
        long bytesToLong(byte[] b, int pos) {
            return (long) bytesToFloat(b, pos);
        }

        @Override
        float bytesToFloat(byte[] b, int pos) {
            return Float.intBitsToFloat(ByteOrder.LITTLE_ENDIAN.bytesToInt(b, pos));
        }

        @Override
        double bytesToDouble(byte[] b, int pos) {
            return bytesToFloat(b, pos);
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return Float.toString(Float.intBitsToFloat(ByteOrder.LITTLE_ENDIAN.bytesToInt(b, pos)));
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            floatToBytes(val, b, off);
        }

        void longToBytes(long val, byte[] b, int off) {
            floatToBytes(val, b, off);
        }

        @Override
        void floatToBytes(float val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.intToBytes(Float.floatToRawIntBits(val), b, off);
        }
    },
    OB(1, null){
        @Override
        int intAt(DicomInput input, long pos) {
            return input.byteAt(pos);
        }

        @Override
        int bytesToInt(byte[] b, int pos) {
            return b[pos];
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            b[off] = (byte) val;
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

        @Override
        int bytesToInt(byte[] b, int pos) {
            return ByteOrder.LITTLE_ENDIAN.bytesToShort(b, pos);
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
        void intToBytes(int val, byte[] b, int off) {
            longToBytes(val, b, off);
        }

        @Override
        void longToBytes(long val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.longToBytes(val, b, off);
        }

        @Override
        int bytesToInt(byte[] b, int pos) {
            return (int) bytesToLong(b, pos);
        }

        @Override
        long bytesToLong(byte[] b, int pos) {
            return ByteOrder.LITTLE_ENDIAN.bytesToLong(b, pos);
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return Long.toString(bytesToLong(b, pos));
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
            return Integer.toUnsignedString(bytesToInt(b, pos));
        }

        long bytesToLong(byte[] b, int pos) {
            return bytesToInt(b, pos) & 0xffffffffL;
        }
    },
    US(2, ToggleEndian.SHORT) {
        @Override
        int intAt(DicomInput input, long pos) {
            return input.ushortAt(pos);
        }

        @Override
        void intToBytes(int val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.shortToBytes(val, b, off);
        }

        @Override
        int bytesToInt(byte[] b, int pos) {
            return ByteOrder.LITTLE_ENDIAN.bytesToShort(b, pos) & 0xffff;
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
        void intToBytes(int val, byte[] b, int off) {
            longToBytes(val, b, off);
        }

        @Override
        void longToBytes(long val, byte[] b, int off) {
            ByteOrder.LITTLE_ENDIAN.longToBytes(val, b, off);
        }

        @Override
        int bytesToInt(byte[] b, int pos) {
            return (int) bytesToLong(b, pos);
        }

        @Override
        String bytesToString(byte[] b, int pos) {
            return Long.toUnsignedString(ByteOrder.LITTLE_ENDIAN.bytesToLong(b, pos));
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
    public OptionalInt intValue(DicomObject dcmobj, int i, int index) {
        long header = dcmobj.headers[i];
        if ((int)(header >>> 62) == 0) {
            if (dcmobj.values[i] instanceof byte[] b && b.length / bytes > index) {
                return OptionalInt.of(bytesToInt(b, bytes * index));
            }
        } else if (dcmobj.header2valueLength(header) / bytes > index) {
            return OptionalInt.of(
                    intAt(dcmobj.dicomInput, DicomObject.header2valuePosition(header) + bytes * index));
        }
        return OptionalInt.empty();
    }

    @Override
    public OptionalLong longValue(DicomObject dcmobj, int i, int index) {
        long header = dcmobj.headers[i];
        if ((int)(header >>> 62) == 0) {
            if (dcmobj.values[i] instanceof byte[] b && b.length / bytes > index) {
                return OptionalLong.of(bytesToLong(b, bytes * index));
            }
        } else if (dcmobj.header2valueLength(header) / bytes > index) {
            return OptionalLong.of(
                    longAt(dcmobj.dicomInput, DicomObject.header2valuePosition(header) + bytes * index));
        }
        return OptionalLong.empty();
    }

    @Override
    public OptionalFloat floatValue(DicomObject dcmobj, int i, int index) {
        long header = dcmobj.headers[i];
        if ((int)(header >>> 62) == 0) {
            if (dcmobj.values[i] instanceof byte[] b && b.length / bytes > index) {
                return OptionalFloat.of(bytesToFloat(b, bytes * index));
            }
        } else if (dcmobj.header2valueLength(header) / bytes > index) {
            return OptionalFloat.of(
                    floatAt(dcmobj.dicomInput, DicomObject.header2valuePosition(header) + bytes * index));
        }
        return OptionalFloat.empty();
    }

    @Override
    public OptionalDouble doubleValue(DicomObject dcmobj, int i, int index) {
        long header = dcmobj.headers[i];
        if ((int)(header >>> 62) == 0) {
            if (dcmobj.values[i] instanceof byte[] b && b.length / bytes > index) {
                return OptionalDouble.of(bytesToDouble(b, bytes * index));
            }
        } else if (dcmobj.header2valueLength(header) / bytes > index) {
            return OptionalDouble.of(
                    doubleAt(dcmobj.dicomInput, DicomObject.header2valuePosition(header) + bytes * index));
        }
        return OptionalDouble.empty();
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

    @Override
    public Object valueOf(long[] vals) {
        if (vals.length == 0) {
            return ByteOrder.EMPTY_BYTES;
        }
        byte[] b = new byte[vals.length * bytes];
        for (int i = 0; i < vals.length; i++) {
            longToBytes(vals[i], b, i * bytes);
        }
        return b;
    }

    @Override
    public Object valueOf(float[] vals) {
        if (vals.length == 0) {
            return ByteOrder.EMPTY_BYTES;
        }
        byte[] b = new byte[vals.length * bytes];
        for (int i = 0; i < vals.length; i++) {
            floatToBytes(vals[i], b, i * bytes);
        }
        return b;
    }

    @Override
    public Object valueOf(double[] vals) {
        if (vals.length == 0) {
            return ByteOrder.EMPTY_BYTES;
        }
        byte[] b = new byte[vals.length * bytes];
        for (int i = 0; i < vals.length; i++) {
            doubleToBytes(vals[i], b, i * bytes);
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

    void intToBytes(int val, byte[] b, int off) {
        ByteOrder.LITTLE_ENDIAN.intToBytes(val, b, off);
    }

    void longToBytes(long val, byte[] b, int off) {
        intToBytes((int) val, b, off);
    }

    void floatToBytes(float val, byte[] b, int off) {
        intToBytes((int) val, b, off);
    }

    void doubleToBytes(double val, byte[] b, int off) {
        floatToBytes((float) val, b, off);
    }

    int bytesToInt(byte[] b, int pos) {
        return ByteOrder.LITTLE_ENDIAN.bytesToInt(b, pos);
    }

    long bytesToLong(byte[] b, int pos) {
        return bytesToInt(b, pos);
    }

    float bytesToFloat(byte[] b, int pos) {
        return bytesToInt(b, pos);
    }

    double bytesToDouble(byte[] b, int pos) {
        return bytesToFloat(b, pos);
    }

    String bytesToString(byte[] b, int pos) {
        return Integer.toString(bytesToInt(b, pos));
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
