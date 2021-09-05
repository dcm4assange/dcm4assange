package org.dcm4assange;

import org.dcm4assange.MemoryCache.DicomInput;
import org.dcm4assange.util.OptionalFloat;
import org.dcm4assange.util.TagUtils;
import org.dcm4assange.util.ToggleEndian;

import java.math.BigInteger;
import java.util.Optional;
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
        public OptionalLong longValue(DicomInput input, long valpos, int vallen) {
            return OptionalLong.empty();
        }

        @Override
        public OptionalFloat floatValue(DicomInput input, long valpos, int vallen) {
            return OptionalFloat.empty();
        }

        @Override
        public OptionalDouble doubleValue(DicomInput input, long valpos, int vallen) {
            return OptionalDouble.empty();
        }

        @Override
        String stringAt(DicomInput input, long pos) {
            return TagUtils.toHexString(input.tagAt(pos));
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
    public OptionalInt intValue(DicomInput input, long valpos, int vallen) {
        return vallen >= bytes
                ? OptionalInt.of(intAt(input, valpos))
                : OptionalInt.empty();
    }

    @Override
    public OptionalInt intValue(DicomObject2 dcmobj, int index) {
        long header = dcmobj.getHeader(index);
        return dcmobj.vallen(header) >= bytes
                ? OptionalInt.of(intAt(dcmobj.dicomInput, DicomObject2.valpos(header)))
                : OptionalInt.empty();
    }

    @Override
    public OptionalLong longValue(DicomInput input, long valpos, int vallen) {
        return vallen >= bytes
                ? OptionalLong.of(longAt(input, valpos))
                : OptionalLong.empty();
    }

    @Override
    public OptionalLong longValue(DicomObject2 dcmobj, int index) {
        long header = dcmobj.getHeader(index);
        return dcmobj.vallen(header) >= bytes
                ? OptionalLong.of(longAt(dcmobj.dicomInput, DicomObject2.valpos(header)))
                : OptionalLong.empty();
    }

    @Override
    public OptionalFloat floatValue(DicomInput input, long valpos, int vallen) {
        return vallen >= bytes
                ? OptionalFloat.of(floatAt(input, valpos))
                : OptionalFloat.empty();
    }

    @Override
    public OptionalFloat floatValue(DicomObject2 dcmobj, int index) {
        long header = dcmobj.getHeader(index);
        return dcmobj.vallen(header) >= bytes
                ? OptionalFloat.of(floatAt(dcmobj.dicomInput, DicomObject2.valpos(header)))
                : OptionalFloat.empty();
    }

    @Override
    public OptionalDouble doubleValue(DicomInput input, long valpos, int vallen) {
        return vallen >= bytes
                ? OptionalDouble.of(doubleAt(input, valpos))
                : OptionalDouble.empty();
    }

    @Override
    public OptionalDouble doubleValue(DicomObject2 dcmobj, int index) {
        long header = dcmobj.getHeader(index);
        return dcmobj.vallen(header) >= bytes
                ? OptionalDouble.of(doubleAt(dcmobj.dicomInput, DicomObject2.valpos(header)))
                : OptionalDouble.empty();
    }

    @Override
    public Optional<String> stringValue(DicomInput input, long valpos, int vallen,
            DicomObject dicomObject) {
        OptionalInt i = intValue(input, valpos, vallen);
        return i.isPresent() ? Optional.of(Integer.toString(i.getAsInt())) : Optional.empty();
    }

    @Override
    public DicomElement elementOf(DicomObject dcmObj, int tag, VR vr, int val) {
        byte[] b = new byte[bytes];
        intToBytes(val, b, 0);
        return new ByteArrayElement(dcmObj, tag, vr, b);
    }

    @Override
    public DicomElement elementOf(DicomObject dcmObj, int tag, VR vr, int... vals) {
        if (vals.length == 0) {
            return new BasicDicomElement(dcmObj, tag, vr, 0);
        }
        byte[] b = new byte[vals.length * bytes];
        for (int i = 0; i < vals.length; i++) {
            intToBytes(vals[i], b, i * bytes);
        }
        return new ByteArrayElement(dcmObj, tag, vr, b);
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
