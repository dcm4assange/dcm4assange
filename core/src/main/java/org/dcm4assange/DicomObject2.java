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
    private long[] headers;
    private Sequence seq;
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

    long getHeader(int index) {
        return headers[index];
    }

    Object getValue(int index) {
        return values[index];
    }

    void setValue(int index, Object value) {
        values[index] = value;
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
}
