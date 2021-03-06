package org.dcm4assange;

import org.dcm4assange.MemoryCache.DicomInput;
import org.dcm4assange.util.OptionalFloat;
import org.dcm4assange.util.StringUtils;
import org.dcm4assange.util.ToggleEndian;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2021
 */
interface VRType {
    default OptionalInt intValue(DicomObject dcmobj, int index) {
        return OptionalInt.empty();
    }

    default OptionalLong longValue(DicomObject dcmobj, int index) {
        return OptionalLong.empty();
    }

    default OptionalFloat floatValue(DicomObject dcmobj, int index) {
        return OptionalFloat.empty();
    }

    default OptionalDouble doubleValue(DicomObject dcmobj, int index) {
        return OptionalDouble.empty();
    }

    default Optional<String> stringValue(DicomObject dcmobj, int index) {
        return Optional.empty();
    }

    default String[] stringValues(DicomObject dcmobj, int index) {
        return StringUtils.EMPTY_STRINGS;
    }

    default Optional<String> stringValue(String value) {
        return Optional.empty();
    }

    default String[] stringValues(String value) {
        return StringUtils.EMPTY_STRINGS;
    }

    VRType SQ = new VRType(){};

    VRType UN = new VRType(){
        @Override
        public Optional<String> stringValue(DicomObject dcmobj, int index) {
            long header = dcmobj.getHeader(index);
            return Optional.of(
                            promptValueTo(
                                    dcmobj.dicomInput,
                                    DicomObject.header2valuePosition(header),
                                    dcmobj.header2valueLength(header),
                                    new StringBuilder())
                                    .toString());
        }

        private StringBuilder promptValueTo(DicomInput input, long valpos, int vallen, StringBuilder sb) {
            for (int i = 0; i < vallen; i++) {
                int c = input.byteAt(valpos + i);
                if (c < ' ' || c == '\\' || c == 127) {
                    sb.append('\\');
                    sb.append((char) ('0' + ((c >> 6) & 7)));
                    sb.append((char) ('0' + ((c >> 3) & 7)));
                    sb.append((char) ('0' + (c & 7)));
                } else {
                    sb.append((char) c);
                }
            }
            return sb;
        }

        @Override
        public StringBuilder promptValueTo(DicomInput dicomInput, long valuePos, int valueLength,
                                           DicomObject dicomObject, StringBuilder sb, int maxLength) {
            sb.append(" [");
            int truncate = Math.max(0, valueLength - sb.length() - maxLength);
            promptValueTo(dicomInput, valuePos, valueLength - truncate, sb);
            if (truncate < 0) {
                sb.append(']');
            }
            if (sb.length() > maxLength) {
                sb.setLength(maxLength);
            }
            return sb;
        }
    };

    default StringBuilder promptValueTo(String[] ss, StringBuilder sb, int maxLength) {
        throw new UnsupportedOperationException();
    }

    default StringBuilder promptValueTo(byte[] b, StringBuilder sb, int maxLength) {
        throw new UnsupportedOperationException();
    }

    default StringBuilder promptValueTo(DicomInput dicomInput, long valuePos, int valueLength,
                                        DicomObject dicomObject, StringBuilder sb, int maxLength) {
        throw new UnsupportedOperationException();
    }

    default ToggleEndian toggleEndian() {
        return null;
    }

    default String delimiters() {
        throw new UnsupportedOperationException();
    }

    default int paddingByte() {
        return 0;
    }

    default byte[] toBytes(String[] ss, DicomObject dcmobj) {
        throw new UnsupportedOperationException();
    }

    default Object valueOf(String[] ss) {
        throw new UnsupportedOperationException();
    }

    default Object valueOf(int[] vals) {
        throw new UnsupportedOperationException();
    }
}
