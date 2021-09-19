package org.dcm4assange;

import org.dcm4assange.util.IORuntimeException;
import org.dcm4assange.util.OptionalFloat;
import org.dcm4assange.util.StringUtils;
import org.dcm4assange.util.TagUtils;

import java.io.IOException;
import java.util.*;

public class DicomObject2 {
    private static final int DEFAULT_CAPACITY = 16;
    private static final int ITEM_DEFAULT_CAPACITY = 4;
    private static final long[] EMPTY_HEADERS = {};
    private static final Object[] EMPTY_VALUES = {};
    final MemoryCache.DicomInput dicomInput;
    final long header;
    private Sequence seq;
    private long[] headers;
    private Object[] values;
    private SpecificCharacterSet specificCharacterSet;

    private int size;

    DicomObject2(MemoryCache.DicomInput dicomInput) {
        this(dicomInput, 0, null, 0);
    }

    DicomObject2(MemoryCache.DicomInput dicomInput, long header, Sequence seq, int size) {
        this.dicomInput = dicomInput;
        this.header = header;
        this.headers = EMPTY_HEADERS;
        this.values = EMPTY_VALUES;
        this.seq = seq;
        this.size = size;
    }

    DicomObject2(DicomObject2 o) {
        this(o.dicomInput, o.header, o.seq, o.size);
        if (size > 0) {
            this.headers = Arrays.copyOf(o.headers, size);
            this.values = Arrays.copyOf(o.values, size);
            this.specificCharacterSet = o.specificCharacterSet;
            for (int i = 0; i < size; i++) {
                if (values[i] instanceof Sequence seq) {
                    values[i] = new Sequence(this, seq);
                } else if (values[i] instanceof Fragments frag) {
                    values[i] = new Fragments(this, frag);
                }
            }
        }
    }

    public DicomObject2() {
        this(null, 0, null, 0);
    }

    public SpecificCharacterSet specificCharacterSet() {
        return specificCharacterSet != null ? specificCharacterSet
                : seq != null ? seq.dcmobj.specificCharacterSet()
                : SpecificCharacterSet.getDefaultCharacterSet();
    }

    public int size() {
        if (size < 0) {
            size = 0;
            try {
                DicomInputStream2 dis = new DicomInputStream2(dicomInput);
                dis.seek(header2valuePosition(header));
                dis.parse(this, header2valueLength(header));
            } catch (IOException e) {
                throw new IORuntimeException("Failed to parse item", e);
            }
        }
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isItem() {
        return seq != null;
    }

    public DicomObject2 getParent() {
        return seq != null ? seq.dcmobj : null;
    }

    public Sequence getSequence() {
        return seq;
    }

    public boolean contains(int tag) {
        return indexOf(tag) >= 0;
    }

    public boolean containsValue(int tag) {
        int i = indexOf(tag);
        return i >= 0 && !isEmpty(headers[i], values[i]);
    }

    public Optional<String> privateCreatorOf(int tag) {
        return TagUtils.isPrivateTag(tag)
                ? getString(TagUtils.creatorTagOf(tag))
                : Optional.empty();
    }

    public OptionalInt getInt(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalInt.empty()
                : VR.fromHeader(headers[i]).type.intValue(this, i);
    }

    public OptionalLong getLong(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalLong.empty()
                : VR.fromHeader(headers[i]).type.longValue(this, i);
    }

    public OptionalFloat getFloat(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalFloat.empty()
                : VR.fromHeader(headers[i]).type.floatValue(this, i);
    }

    public OptionalDouble getDouble(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalDouble.empty()
                : VR.fromHeader(headers[i]).type.doubleValue(this, i);
    }

    public Optional<String> getString(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? Optional.empty()
                : VR.fromHeader(headers[i]).type.stringValue(this, i);
    }

    public String[] getStrings(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? StringUtils.EMPTY_STRINGS
                : VR.fromHeader(headers[i]).type.stringValues(this, i);
    }

    public Optional<String> getBulkDataURI(int tag) {
        int i = indexOf(tag);
        return (i >= 0 && values[i] instanceof String s)
                ? Optional.of(s)
                : Optional.empty();
    }

    public Optional<Sequence> getSequence(int tag) {
        int i = indexOf(tag);
        return (i >= 0 && values[i] instanceof Sequence sequence)
                ? Optional.of(sequence)
                : Optional.empty();
    }

    public Optional<Fragments> getFragments(int tag) {
        int i = indexOf(tag);
        return (i >= 0 && values[i] instanceof Fragments fragments)
                ? Optional.of(fragments)
                : Optional.empty();
    }

    public void setString(int tag, VR vr, String... vals) {
        add(tag, vr, vr.type.valueOf(vals));
    }

    public void setInt(int tag, VR vr, int... vals) {
        add(tag, vr, vr.type.valueOf(vals));
    }

    long getHeader(int index) {
        return headers[index];
    }

    Object getValue(int index) {
        return values[index];
    }

    void setValue(int index, Object value) {
        values[index] = value;
    }

    public int add(int tag, VR vr, Object value) {
        return add(vr.toHeader() | (tag & 0xffffffffL), value);
    }

    public int add(long header, Object value) {
        int index = indexOf(header2tag(header));
        int i;
        if (index < 0) {
            insertAt(i = -(index + 1), header, value);
        } else {
            headers[i = index] = header;
        }
        int tag = header2tag(header);
        if (tag == Tag.SpecificCharacterSet) {
            specificCharacterSet = SpecificCharacterSet.valueOf(StringVR.ASCII.stringValues(this, i));
        }
        return index;
    }

    private void insertAt(int index, long header, Object value) {
        int copy = size - index;
        int oldCapacity = headers.length;
        if (++size >= oldCapacity) {
            if (oldCapacity == 0) {
                int newCapacity = seq != null ? ITEM_DEFAULT_CAPACITY : DEFAULT_CAPACITY;
                headers = new long[newCapacity];
                values = new Object[newCapacity];
            } else {
                int newCapacity = oldCapacity << 1;
                headers = Arrays.copyOf(headers, newCapacity);
                values = Arrays.copyOf(values, newCapacity);
            }
        }
        if (copy > 0) {
            System.arraycopy(headers, index, headers, index + 1, copy);
            System.arraycopy(values, index, values, index + 1, copy);
        }
        headers[index] = header;
        values[index] = value;
    }

    private int indexOf(int tag) {
        int size = size();
        int high = size - 1;
        if (size > 0 && Integer.compareUnsigned(header2tag(headers[high]), tag) < 0)
            return -(size + 1);
        int low = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = Integer.compareUnsigned(header2tag(headers[mid]), tag);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // tag found
        }
        return -(low + 1);  // tag not found
    }

    int header2tag(long header) {
        return ((int)(header >>> 62) == 0) ? (int) header : dicomInput.tagAt(header & 0x00ffffffffffffffL);
    }

    static long header2valuePosition(long header) {
        return (header & 0x00ffffffffffffffL) + header2headerLength(header);
    }

    public static int header2headerLength(long header) {
        return ((int)(header >>> 62) == 3) ? 12 : 8;
    }

    int header2valueLength(long header) {
        return ((int)(header >>> 62) == 0) ? -1 : dicomInput.header2valueLength(header);
    }

    boolean isEmpty(long header, Object value) {
        return value instanceof byte[] b ? b.length == 0
                : value instanceof String[] ss ? ss.length == 0
                : value instanceof Sequence seq ? seq.size() == 0
                : value instanceof Fragments frags ? frags.size() == 0
                : dicomInput.header2valueLength(header) == 0;
    }

    public StringBuilder promptElementTo(long header, StringBuilder sb, int maxLength) {
        return promptTo(false, header, sb, maxLength);
    }

    public StringBuilder promptFragmentTo(long header, StringBuilder sb, int maxLength) {
        return promptTo(true, header, sb, maxLength);
    }

    private StringBuilder promptTo(boolean fragment, long header, StringBuilder sb, int maxLength) {
        int tag = header2tag(header);
        VR vr = VR.fromHeader(header);
        int valueLength = header2valueLength(header);
        promptLevelTo(sb).append(TagUtils.toCharArray(tag));
        if (vr != null) sb.append(' ').append(vr);
        else if (fragment) vr = VR.OB;
        sb.append(" #").append(valueLength);
        if (vr != null && vr != VR.SQ)
            vr.type.promptValueTo(dicomInput, header2valuePosition(header), header2valueLength(header), this, sb, maxLength);
        if (sb.length() < maxLength) {
            sb.append(" ").append(
                    ElementDictionary.keywordOf(privateCreatorOf(tag).orElse(null), tag));
            if (sb.length() > maxLength) {
                sb.setLength(maxLength);
            }
        }
        return sb;
    }

    public StringBuilder promptLevelTo(StringBuilder appendTo) {
        for (Sequence seq = this.seq; seq != null; seq = seq.dcmobj.seq) {
            appendTo.append('>');
        }
        return appendTo;
    }

    Encoder createEncoder(DicomEncoding encoding,
                          boolean includeGroupLength,
                          boolean undefSequenceLength,
                          boolean undefItemLength) {
        return new Encoder(encoding, includeGroupLength, undefSequenceLength, undefItemLength);
    }

    class Encoder {
        private static final int BUFFER_SIZE = 8192;
        private final DicomEncoding encoding;
        private final boolean includeGroupLength;
        private final boolean undefSequenceLength;
        private final boolean undefItemLength;
        private final int[] groupLengths;
        private final int itemLength;
        private byte[] swapBuffer;

        Encoder(DicomEncoding encoding,
                boolean includeGroupLength,
                boolean undefSequenceLength,
                boolean undefItemLength) {
            this.encoding = encoding;
            this.includeGroupLength = includeGroupLength;
            this.undefSequenceLength = undefSequenceLength;
            this.undefItemLength = undefItemLength;
            this.groupLengths = includeGroupLength && size != 0
                    ? new int[numberOfGroups()]
                    : ByteOrder.EMPTY_INTS;
            this.itemLength = calc();
        }

        private int calc() {
            int size = size();
            if (size == 0) return 0;
            int length = groupLengths.length * 12;
            int groupLengthsIndex = -1;
            int gggg0000 = -1;
            for (int i = 0; i < size; i++) {
                long header = headers[i];
                int l = totLength(header, i);
                length += l;
                if (includeGroupLength) {
                    int hhhh0000 = TagUtils.groupLengthTagOf(header2tag(header));
                    if (gggg0000 != hhhh0000) {
                        gggg0000 = hhhh0000;
                        groupLengthsIndex++;
                    }
                    groupLengths[groupLengthsIndex] += l;
                }
            }
            return length;
        }

        private int valueLength(long header, VR vr, int index) {
            Object value = values[index];
            if (value instanceof Sequence seq) {
                Encoder[] encoders = new Encoder[seq.size()];
                for (int i = 0; i < encoders.length; i++) {
                    encoders[i] = seq.getItem(i).createEncoder(
                                    encoding, includeGroupLength, undefSequenceLength, undefItemLength);
                }
                values[index] = value = encoders;
            }
            if (value instanceof Encoder[] encoders) {
                int length = encoders.length * 8;
                for (Encoder encoder : encoders) {
                    length += encoder.itemLength;
                    if (undefItemLength) length += 8;
                }
                return length;
            }
            if (value instanceof String[] ss) {
                values[index] = value = vr.type.toBytes(ss, DicomObject2.this);
            }
            if (value instanceof byte[] b) {
                return (b.length + 1) & ~1;
            }
            return header2valueLength(header);
        }

        private int totLength(long header, int index) {
            VR vr = VR.fromHeader(header);
            return (vr.evr8 || !encoding.explicitVR ? 8 : 12)
                    + valueLength(header, vr, index)
                    + (undefSequenceLength && vr == VR.SQ ? 8 : 0);
        }

        public void writeTo(DicomOutputStream2 out) throws IOException {
            int size = size();
            if (size == 0) return;
            byte[] b12 = new byte[12];
            int groupLengthsIndex = -1;
            int gggg0000 = -1;
            for (int index = 0; index < size; index++) {
                long header = headers[index];
                int tag = header2tag(header);
                if (includeGroupLength) {
                    int hhhh0000 = TagUtils.groupLengthTagOf(tag);
                    if (gggg0000 != hhhh0000) {
                        gggg0000 = hhhh0000;
                        groupLengthsIndex++;
                        fillHeader(gggg0000, VR.UL, 4, b12);
                        encoding.byteOrder.intToBytes(groupLengths[groupLengthsIndex], b12, 8);
                        out.write(b12, 0, 12);
                    }
                }
                Object value = values[index];
                VR vr = VR.fromHeader(header);
                int vlen = valueLength(header, vr, index);
                if (value instanceof Encoder[] encoders) {
                    out.write(b12, 0, fillHeader(tag, vr, undefSequenceLength ? -1 : vlen, b12));
                    for (Encoder encoder : encoders) {
                        out.write(b12, 0, fillHeader(Tag.Item, null, undefItemLength ? -1 : encoder.itemLength, b12));
                        encoder.writeTo(out);
                        if (undefItemLength)
                            out.write(b12, 0, fillHeader(Tag.ItemDelimitationItem, null, 0, b12));
                    }
                    if (undefSequenceLength)
                        out.write(b12, 0, fillHeader(Tag.SequenceDelimitationItem, null, 0, b12));
                } else {
                    out.write(b12, 0, fillHeader(tag, vr, vlen, b12));
                    if (value instanceof byte[] b) {
                        out.write(b);
                        if ((b.length & 1) != 0)
                            out.write(vr.paddingByte);
                    } else {
                        if (encoding.byteOrder == dicomInput.encoding.byteOrder || vr.type.toggleEndian() == null)
                            dicomInput.cache().writeBytesTo(header2valuePosition(header), vlen, out);
                        else
                            dicomInput.cache().writeSwappedBytesTo(header2valuePosition(header), vlen, out,
                                    vr.type.toggleEndian(), swapBuffer());
                    }
                }
            }
        }

        private byte[] swapBuffer() {
            byte[] swapBuffer = this.swapBuffer;
            if (swapBuffer == null)
                this.swapBuffer = swapBuffer = new byte[BUFFER_SIZE];
            return swapBuffer;
        }

        private int fillHeader(int tag, VR vr, int length, byte[] b12) {
            encoding.byteOrder.tagToBytes(tag, b12, 0);
            if (!encoding.explicitVR || vr == null) {
                encoding.byteOrder.intToBytes(length, b12, 4);
                return 8;
            }
            b12[4] = (byte) (vr.code >>> 8);
            b12[5] = (byte) vr.code;
            if (vr.evr8) {
                encoding.byteOrder.shortToBytes(length, b12, 6);
                return 8;
            }
            b12[6] = 0;
            b12[7] = 0;
            encoding.byteOrder.intToBytes(length, b12, 8);
            return 12;
        }
    }

    private int numberOfGroups() {
        int size = size();
        if (size == 0) return 0;
        int gggg0000 = TagUtils.groupLengthTagOf(header2tag(headers[0]));
        if (gggg0000 == TagUtils.groupLengthTagOf(header2tag(headers[size - 1]))) return 1;
        int n = 1;
        for (int i = 1; i < size; i++) {
            int hhhh0000 = TagUtils.groupLengthTagOf(header2tag(headers[i]));
            if (gggg0000 != hhhh0000) {
                gggg0000 = hhhh0000;
                n++;
            }
        }
        return n;
    }
}
