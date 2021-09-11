package org.dcm4assange;

import org.dcm4assange.util.OptionalFloat;
import org.dcm4assange.util.StringUtils;
import org.dcm4assange.util.TagUtils;

import java.util.*;

public class DicomObject2 {
    private static final int DEFAULT_CAPACITY = 16;
    private static final int ITEM_DEFAULT_CAPACITY = 4;
    private static final long[] EMPTY_HEADERS = {};
    private static final Object[] EMPTY_VALUES = {};
    final MemoryCache.DicomInput dicomInput;
    private long[] headers;
    private Sequence dcmseq;
    private Object[] values;
    private SpecificCharacterSet specificCharacterSet;

    private int size;

    DicomObject2(MemoryCache.DicomInput dicomInput) {
        this(dicomInput, null);
    }

    DicomObject2(MemoryCache.DicomInput dicomInput, Sequence dcmseq) {
        this.dicomInput = dicomInput;
        this.headers = EMPTY_HEADERS;
        this.values = EMPTY_VALUES;
        this.dcmseq = dcmseq;
    }

    public SpecificCharacterSet specificCharacterSet() {
        return specificCharacterSet != null ? specificCharacterSet
                : dcmseq != null ? dcmseq.dcmobj.specificCharacterSet()
                : SpecificCharacterSet.getDefaultCharacterSet();
    }

    public int size() {
        return size;
    }

    public boolean isRoot() {
        return dcmseq == null;
    }

    public DicomObject2 getParent() {
        return dcmseq != null ? dcmseq.dcmobj : null;
    }

    public int getSequenceTag() {
        return dcmseq != null ? dcmseq.tag : 0;
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
                : header2vr(headers[i]).type.intValue(this, i);
    }

    public OptionalLong getLong(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalLong.empty()
                : header2vr(headers[i]).type.longValue(this, i);
    }

    public OptionalFloat getFloat(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalFloat.empty()
                : header2vr(headers[i]).type.floatValue(this, i);
    }

    public OptionalDouble getDouble(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalDouble.empty()
                : header2vr(headers[i]).type.doubleValue(this, i);
    }

    public Optional<String> getString(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? Optional.empty()
                : header2vr(headers[i]).type.stringValue(this, i);
    }

    public String[] getStrings(int tag) {
        int i = indexOf(tag);
        return i < 0
                ? StringUtils.EMPTY_STRINGS
                : header2vr(headers[i]).type.stringValues(this, i);
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
        if (index < 0) {
            insertAt(-(index + 1), header, value);
        } else {
            headers[index] = header;
        }
        return index;
    }

    private void insertAt(int index, long header, Object value) {
        int copy = size - index;
        int oldCapacity = headers.length;
        if (++size >= oldCapacity) {
            if (oldCapacity == 0) {
                int newCapacity = dcmseq != null ? ITEM_DEFAULT_CAPACITY : DEFAULT_CAPACITY;
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

    static VR header2vr(long header) {
        int index = ((int) (header >>> 56)) & 0x3f;
        return index > 0 ? VR.values()[index - 1] : null;
    }

    int header2tag(long header) {
        return ((int)(header >>> 62) == 0) ? (int) header : dicomInput.tagAt(header & 0x00ffffffffffffffL);
    }

    static long valpos(long header) {
        return (header & 0x00ffffffffffffffL) + headerlen(header);
    }

    static int headerlen(long header) {
        return ((int)(header >>> 62) == 3) ? 12 : 8;
    }

    int vallen(long header) {
        return ((int)(header >>> 62) == 0) ? -1 : dicomInput.vallen(header);
    }
}
