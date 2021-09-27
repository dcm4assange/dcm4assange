package org.dcm4assange;

import org.dcm4assange.MemoryCache.DicomInput;
import org.dcm4assange.util.StringUtils;
import org.dcm4assange.util.StringUtils.Trim;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Mar 2021
 */
enum StringVR implements VRType {
    ASCII("\\", VM.MULTI, Trim.LEADING_AND_TRAILING, StringVR::ascii2),
    STRING("\\", VM.MULTI, Trim.LEADING_AND_TRAILING, DicomObject::specificCharacterSet),
    TEXT("\n\t\f", VM.SINGLE, Trim.TRAILING, DicomObject::specificCharacterSet),
    DS("\\", VM.MULTI, Trim.LEADING_AND_TRAILING, StringVR::ascii2) {
        @Override
        public OptionalInt intValue(DicomObject dcmobj, int index) {
            Optional<String> s = stringValue(dcmobj, index);
            return s.isPresent() ? OptionalInt.of((int) Double.parseDouble(s.get())) : OptionalInt.empty();
        }
    },
    IS("\\", VM.MULTI, Trim.LEADING_AND_TRAILING, StringVR::ascii2) {
        @Override
        public OptionalInt intValue(DicomObject dcmobj, int index) {
            Optional<String> s = stringValue(dcmobj, index);
            return s.isPresent() ? OptionalInt.of(Integer.parseInt(s.get())) : OptionalInt.empty();
        }
    },
    PN("\\^=", VM.MULTI, Trim.LEADING_AND_TRAILING, DicomObject::specificCharacterSet),
    UC("\\", VM.MULTI, Trim.TRAILING, StringVR::ascii2),
    UR("", VM.SINGLE, Trim.LEADING_AND_TRAILING, StringVR::ascii2),
    UI("\\", VM.MULTI, Trim.LEADING_AND_TRAILING, StringVR::ascii2);

    private final String delimiters;
    private final VM vm;
    private final StringUtils.Trim trim;
    private final Function<DicomObject, SpecificCharacterSet> asciiOrCS2;

    StringVR(String delimiters, VM vm, Trim trim,
             Function<DicomObject, SpecificCharacterSet> asciiOrCS2) {
        this.delimiters = delimiters;
        this.vm = vm;
        this.trim = trim;
        this.asciiOrCS2 = asciiOrCS2;
    }

    @Override
    public String delimiters() {
        return delimiters;
    }

    @Override
    public byte[] toBytes(String[] ss, DicomObject dcmobj) {
        return asciiOrCS2.apply(dcmobj).encode(StringUtils.join(ss, 0, ss.length, '\\'), delimiters);
    }

    @Override
    public Object valueOf(String[] ss) {
        return ss;
    }

    @Override
    public Optional<String> stringValue(DicomObject dcmobj, int index) {
        String[] ss = stringValues(dcmobj, index);
        return ss.length > 0 ? Optional.of(ss[0]) : Optional.empty();
    }

    @Override
    public String[] stringValues(DicomObject dcmobj, int index) {
        if (dcmobj.getValue(index) instanceof String[] ss)
            return ss;
        long header = dcmobj.getHeader(index);
        String[] ss = stringValues(dcmobj.dicomInput.stringAt(
                DicomObject.header2valuePosition(header),
                dcmobj.header2valueLength(header),
                asciiOrCS2.apply(dcmobj)));
        dcmobj.setValue(index, ss);
        return ss;
    }

    @Override
    public Optional<String> stringValue(String value) {
        return vm.first(value, trim);
    }

    @Override
    public String[] stringValues(String value) {
        return vm.split(value, trim);
    }

    @Override
    public StringBuilder promptValueTo(String[] ss, StringBuilder sb, int maxLength) {
        sb.append(" [");
        for (int i = 0; i < ss.length && sb.length() < maxLength; i++) {
            if (i > 0) sb.append('\\');
            sb.append(ss[i]);
        }
        if (sb.length() < maxLength)
            sb.append("]");
        else
            sb.setLength(maxLength);
        return sb;
    }

    @Override
    public StringBuilder promptValueTo(DicomInput input, long valuePos, int valueLen, DicomObject dcmobj,
                                       StringBuilder sb, int maxLength) {
        sb.append(" [");
        int limitValueLen = (maxLength - sb.length() - 1) * 2; // assume max 2 bytes by char
        if (limitValueLen < valueLen) {
            sb.append(input.stringAt(valuePos, limitValueLen, asciiOrCS2.apply(dcmobj)));
        } else {
            sb.append(StringUtils.trim(input.stringAt(valuePos, valueLen, asciiOrCS2.apply(dcmobj)), trim));
            sb.append(']');
        }
        if (sb.length() > maxLength) {
            sb.setLength(maxLength);
        }
        return sb;
    }

    private static SpecificCharacterSet ascii2(DicomObject dicomObject) {
        return SpecificCharacterSet.ASCII;
    }

    enum VM {
        SINGLE {
            @Override
            Optional<String> first(String s, StringUtils.Trim trim) {
                return StringUtils.optionalOf(StringUtils.trim(s, trim));
            }

            @Override
            String[] split(String s, StringUtils.Trim trim) {
                return s.isEmpty()
                        ? StringUtils.EMPTY_STRINGS
                        : new String[]{StringUtils.trim(s, trim)};
            }

            @Override
            String join(VR vr, String[] s) {
                if (s.length == 1) {
                    return s[0];
                }
                throw new IllegalArgumentException(String.format("VR: %s does not allow multiple values", vr));
            }
        },
        MULTI {
            @Override
            Optional<String> first(String s, StringUtils.Trim trim) {
                return StringUtils.optionalOf(StringUtils.cut(s, s.length(), '\\', 0, trim));
            }

            @Override
            String[] split(String s, StringUtils.Trim trim) {
                return StringUtils.split(s, s.length(), '\\', trim);
            }

            @Override
            String join(VR vr, String[] ss) {
                return StringUtils.join(ss, 0, ss.length, '\\');
            }
        };

        abstract Optional<String> first(String s, StringUtils.Trim trim);

        abstract String[] split(String s, StringUtils.Trim trim);

        abstract String join(VR vr, String[] s);
    }
}
