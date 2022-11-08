package org.dcm4assange;

import org.dcm4assange.util.IORuntimeException;
import org.dcm4assange.util.OptionalFloat;
import org.dcm4assange.util.StringUtils;
import org.dcm4assange.util.TagUtils;

import java.io.*;
import java.util.*;
import java.util.function.ToIntBiFunction;

public class DicomObject implements Serializable {
    private static final int TO_STRING_LENGTH = 78;
    private static final int TO_STRING_LINES = 50;
    private static final int DEFAULT_CAPACITY = 16;
    private static final int ITEM_DEFAULT_CAPACITY = 4;
    final MemoryCache.DicomInput dicomInput;
    final long position;
    final Sequence seq;
    volatile long[] headers = {};
    volatile Object[] values = {};
    volatile SpecificCharacterSet specificCharacterSet;
    volatile int size;
    volatile int length = -1;

    DicomObject(MemoryCache.DicomInput dicomInput, long position, int length, Sequence seq, int size) {
        this.dicomInput = dicomInput;
        this.position = position;
        this.length = length;
        this.seq = seq;
        this.size = size;
    }

    DicomObject(DicomObject o) {
        this(o.dicomInput, o.position, o.length, o.seq, o.size);
        if (size > 0) {
            int capacity = size + 1; // reserve space to include Group Length
            this.headers = Arrays.copyOf(o.headers, capacity);
            this.values = Arrays.copyOf(o.values, capacity);
            this.specificCharacterSet = o.specificCharacterSet;
            for (int i = 0; i < size; i++) {
                if (values[i] instanceof Sequence seq) {
                    values[i] = new Sequence(seq);
                } else if (values[i] instanceof Fragments frag) {
                    values[i] = new Fragments(frag);
                }
            }
        }
    }

    public DicomObject() {
        this(null, -1L, -1, null, 0);
    }

    public static DicomObject createFileMetaInformation(String cuid, String iuid, String tsuid,
                                                        boolean implementationVersionName) {
        if (iuid == null || iuid.isEmpty())
            throw new IllegalArgumentException("Missing SOP Instance UID");
        if (cuid == null || cuid.isEmpty())
            throw new IllegalArgumentException("Missing SOP Class UID");
        if (tsuid == null || tsuid.isEmpty())
            throw new IllegalArgumentException("Missing Transfer Syntax UID");

        DicomObject fmi = new DicomObject();
        fmi.setBytes(Tag.FileMetaInformationVersion, VR.OB, new byte[]{0, 1});
        fmi.setString(Tag.MediaStorageSOPClassUID, VR.UI, cuid);
        fmi.setString(Tag.MediaStorageSOPInstanceUID, VR.UI, iuid);
        fmi.setString(Tag.TransferSyntaxUID, VR.UI, tsuid);
        fmi.setString(Tag.ImplementationClassUID, VR.UI, Implementation.CLASS_UID);
        if (implementationVersionName) {
            fmi.setString(Tag.ImplementationVersionName, VR.SH, Implementation.VERSION_NAME);
        }
        return fmi;
    }

    public SpecificCharacterSet specificCharacterSet() {
        return specificCharacterSet != null ? specificCharacterSet
                : seq != null ? seq.containedBy().specificCharacterSet()
                : SpecificCharacterSet.getDefaultCharacterSet();
    }

    public int size() {
        if (size < 0) {
            size = 0;
            try {
                new DicomInputStream(dicomInput).parse(this);
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

    public DicomObject getParent() {
        return seq != null ? seq.containedBy() : null;
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

    public OptionalInt getInt(int tag, int index) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalInt.empty()
                : VR.fromHeader(headers[i]).type.intValue(this, i, index);
    }

    public OptionalInt getInt(int tag) {
        return getInt(tag, 0);
    }

    public int getIntOrElseThrow(int tag) {
        return getInt(tag).orElseThrow(() -> missing(tag));
    }

    public OptionalLong getLong(int tag, int index) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalLong.empty()
                : VR.fromHeader(headers[i]).type.longValue(this, i, index);
    }

    public OptionalLong getLong(int tag) {
        return getLong(tag, 0);
    }

    public long getLongOrElseThrow(int tag) {
        return getLong(tag).orElseThrow(() -> missing(tag));
    }

    public OptionalFloat getFloat(int tag, int index) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalFloat.empty()
                : VR.fromHeader(headers[i]).type.floatValue(this, i, index);
    }

    public OptionalFloat getFloat(int tag) {
        return getFloat(tag, 0);
    }

    public float getFloatOrElseThrow(int tag) {
        return getFloat(tag).orElseThrow(() -> missing(tag));
    }

    public OptionalDouble getDouble(int tag, int index) {
        int i = indexOf(tag);
        return i < 0
                ? OptionalDouble.empty()
                : VR.fromHeader(headers[i]).type.doubleValue(this, i, index);
    }

    public OptionalDouble getDouble(int tag) {
        return getDouble(tag, 0);
    }

    public double getDoubleOrElseThrow(int tag) {
        return getDouble(tag).orElseThrow(() -> missing(tag));
    }

    public Optional<String> getString(int tag, int index) {
        int i = indexOf(tag);
        return i < 0
                ? Optional.empty()
                : VR.fromHeader(headers[i]).type.stringValue(this, i, index);
    }

    public Optional<String> getString(int tag) {
        return getString(tag, 0);
    }

    public String getStringOrElseThrow(int tag) {
        return getString(tag).orElseThrow(() -> missing(tag));
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

    public Optional<DicomObject> getItem(int tag) {
        int i = indexOf(tag);
        return (i >= 0 && values[i] instanceof Sequence sequence && !sequence.isEmpty())
                ? Optional.of(sequence.getItem(0))
                : Optional.empty();
    }

    public Sequence newSequence(int tag) {
        Sequence sequence = new Sequence(tag);
        add(tag, VR.SQ, sequence);
        return sequence;
    }

    public Optional<Fragments> getFragments(int tag) {
        int i = indexOf(tag);
        return (i >= 0 && values[i] instanceof Fragments fragments)
                ? Optional.of(fragments)
                : Optional.empty();
    }

    public Fragments newFragments(int tag, long header) {
        Fragments fragments = new Fragments(tag);
        add(header, fragments);
        return fragments;
    }

    public void setBytes(int tag, VR vr, byte[] val) {
        add(tag, vr, val);
    }

    public void setString(int tag, VR vr, String... vals) {
        add(tag, vr, vr.type.valueOf(vals));
    }

    public void setBulkDataURI(int tag, VR vr, String bulkdataURI) {
        add(tag, vr, Objects.requireNonNull(bulkdataURI));
    }

    public void setInt(int tag, VR vr, int... vals) {
        add(tag, vr, vr.type.valueOf(vals));
    }

    public void setLong(int tag, VR vr, long... vals) {
        add(tag, vr, vr.type.valueOf(vals));
    }

    public void setFloat(int tag, VR vr, float... vals) {
        add(tag, vr, vr.type.valueOf(vals));
    }

    public void setDouble(int tag, VR vr, double... vals) {
        add(tag, vr, vr.type.valueOf(vals));
    }

    private int add(int tag, VR vr, Object value) {
        return add(vr.toHeader() | (tag & 0xffffffffL), value);
    }

    public int add(long header, Object value) {
        int tag = header2tag(header);
        int index = indexOf(tag);
        int i;
        if (index < 0) {
            insertAt(i = -(index + 1), header, value);
        } else {
            headers[i = index] = header;
            values[i] = value;
        }
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
        return ((int)(header >>> 62) == 0) ? (int) header : dicomInput.tagAt(header & 0x007fffffffffffffL);
    }

    static long header2valuePosition(long header) {
        return (header & 0x007fffffffffffffL) + header2headerLength(header);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(512);
        if (promptTo(sb, TO_STRING_LENGTH, TO_STRING_LINES) < 0) {
            sb.append(System.lineSeparator()).append("...");
        }
        return sb.toString();
    }

    public int promptTo(StringBuilder sb, int maxColumns, int maxLines) {
        if (size < 0) {
            if (--maxLines > 0) {
                promptLevelTo(sb).append(" not parsed");
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (--maxLines < 0) break;
                if (i > 0) sb.append(System.lineSeparator());
                promptElementTo(headers[i], values[i], sb, sb.length() + maxColumns);
                if (values[i] instanceof Sequence seq) {
                    maxColumns = seq.promptItemsTo(sb, maxColumns, maxLines);
                }
            }
        }
        return maxLines;
    }

    public StringBuilder promptElementTo(long header, Object value, StringBuilder sb, int maxLength) {
        int tag = header2tag(header);
        VR vr = VR.fromHeader(header);
        promptLevelTo(sb).append(TagUtils.toCharArray(tag));
        if (vr != null) sb.append(' ').append(vr);
        if (value instanceof byte[] b) {
            sb.append(" #").append((b.length + 1) & ~1);
            vr.type.promptValueTo(b, sb, maxLength);
        } else if (value instanceof String[] ss) {
            int valueLength = ss.length;
            for (String s: ss) valueLength += s.length();
            sb.append(" #").append(valueLength & ~1);
            vr.type.promptValueTo(ss, sb, maxLength);
        } else if (value instanceof String s) {
            sb.append(" {").append(s).append('}');
        } else {
            int valueLength = header2valueLength(header);
            sb.append(" #").append(valueLength);
            if (vr != null && vr != VR.SQ && valueLength != -1)
                vr.type.promptValueTo(dicomInput, header2valuePosition(header), valueLength, this, sb, maxLength);
        }
        if (sb.length() < maxLength) {
            sb.append(" ").append(
                    ElementDictionary.keywordOf(privateCreatorOf(tag).orElse(null), tag));
            if (sb.length() > maxLength) {
                sb.setLength(maxLength);
            }
        }
        return sb;
    }

    public StringBuilder promptFragmentTo(long header, Object value, StringBuilder sb, int maxLength) {
        int valueLength = header2valueLength(header);
        promptLevelTo(sb).append("(FFFE,E000) #").append(valueLength);
        BinaryVR.OB.promptValueTo(dicomInput, header2valuePosition(header), valueLength, this, sb, maxLength);
        if (sb.length() < maxLength) {
            sb.append(" Fragment");
            if (sb.length() > maxLength) {
                sb.setLength(maxLength);
            }
        }
        return sb;
    }

    public StringBuilder promptLevelTo(StringBuilder appendTo) {
        for (Sequence seq = this.seq; seq != null; seq = seq.containedBy().seq) {
            appendTo.append('>');
        }
        return appendTo;
    }

    int calculateLength(DicomOutputStream dos) {
        if (size == -1 && dicomInput.encoding == dos.encoding()) {
            return length;
        }
        int size = size();
        int length = 0;
        for (int index = 0; index < size; index++) {
            long header = headers[index];
            if (!TagUtils.isGroupLength(header2tag(header))) {
                VR vr = VR.fromHeader(header);
                int l = (vr.evr8 || !dos.encoding().explicitVR ? 8 : 12)
                        + valueLength(dos, header, vr, index, DicomObject::calculateLength)
                        + (dos.undefSequenceLength() && vr == VR.SQ ? 8 : 0);
                length += l;
            }
        }
        this.length = length;
        return length;
    }

    int calculateLengthWithGroupLength(DicomOutputStream dos) {
        int size = size();
        int length = 0;
        int[] groupLengthTags = groupLengthTags();
        int[] groupLengths = new int[groupLengthTags.length];
        for (int index = 0, gi = 0; index < size; index++) {
            long header = headers[index];
            int tag = header2tag(header);
            if (!TagUtils.isGroupLength(tag)) {
                VR vr = VR.fromHeader(header);
                int l = (vr.evr8 || !dos.encoding().explicitVR ? 8 : 12)
                        + valueLength(dos, header, vr, index, DicomObject::calculateLengthWithGroupLength)
                        + (dos.undefSequenceLength() && vr == VR.SQ ? 8 : 0);
                length += l;
                if (groupLengthTags[gi] != TagUtils.groupLengthTagOf(tag)) gi++;
                groupLengths[gi] += l;
            }
        }
        for (int i = 0; i < groupLengthTags.length; i++) {
            setInt(groupLengthTags[i], VR.UL, groupLengths[i]);
            length += 12;
        }
        this.length = length;
        return length;
    }

    private int valueLength(DicomOutputStream dos, long header, VR vr, int index,
                            ToIntBiFunction<DicomObject, DicomOutputStream> calc) {
        Object value = values[index];
        if (value instanceof String bulkDataURI) {
            if (dos.encoding() == DicomEncoding.SERIALIZE) {
                final int strlen = bulkDataURI.length();
                int utflen = bulkDataURI.length();
                for (int i = 0; i < strlen; i++) {
                    int c = bulkDataURI.charAt(i);
                    if (c >= 0x80 || c == 0)
                        utflen += (c >= 0x800) ? 2 : 1;
                }
                return utflen;
            }
        } else {
            if (value instanceof Sequence seq) {
                int size = seq.size();
                int length = (dos.undefItemLength() ? 16 : 8) * size;
                for (int i = 0; i < size; i++) {
                    DicomObject item = seq.getItem(i);
                    length += calc.applyAsInt(item, dos);
                }
                return length;
            }
            if (value instanceof String[] ss) {
                values[index] = value = vr.type.toBytes(ss, DicomObject.this);
            }
            if (value instanceof byte[] b) {
                return (b.length + 1) & ~1;
            }
        }
        return header2valueLength(header);
    }

    void writeTo(DicomOutputStream out, boolean includeGroupLength) throws IOException {
        if (size == -1) {
            dicomInput.cache().writeBytesTo(position, length, out);
            return;
        }
        for (int index = 0, n = size; index < n; index++) {
            long header = headers[index];
            Object value = values[index];
            if (value instanceof Sequence seq) {
                out.write(seq, includeGroupLength);
            } else {
                int tag = header2tag(header);
                if (includeGroupLength || !TagUtils.isGroupLength(tag)){
                    if (value instanceof byte[] b) {
                        out.write(tag, VR.fromHeader(header), b);
                    } else if (value instanceof String bulkDataURI) {
                        out.write(tag, VR.fromHeader(header), bulkDataURI);
                    } else {
                        out.write(header, dicomInput);
                    }
                }
            }
        }
    }

    int[] groupLengthTags() {
        int size = size();
        if (size == 0) return ByteOrder.EMPTY_INTS;
        int gggg0000 = TagUtils.groupLengthTagOf(header2tag(headers[0]));
        int[] tags = { gggg0000 };
        if (gggg0000 != TagUtils.groupLengthTagOf(header2tag(headers[size - 1]))) {
            int n = 1;
            for (int i = 1; i < size; i++) {
                int hhhh0000 = TagUtils.groupLengthTagOf(header2tag(headers[i]));
                if (gggg0000 != hhhh0000) {
                    tags = Arrays.copyOf(tags, n + 1);
                    tags[n++] = gggg0000 = hhhh0000;
                }
            }
        }
        return tags;
    }

    private static class SerializationProxy implements Serializable {
        private transient DicomObject dcmobj;

        public SerializationProxy(DicomObject dcmobj) {
            this.dcmobj = dcmobj;
        }

        @Serial
        private Object readResolve() throws ObjectStreamException {
            return dcmobj;
        }

        @Serial
        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            dcmobj = new DicomInputStream(ois).withEncoding(DicomEncoding.SERIALIZE).readDataSet();
        }

        @Serial
        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
            new DicomOutputStream(oos).withEncoding(DicomEncoding.SERIALIZE).writeDataSet(dcmobj);
        }

        @Serial
        private static final long serialVersionUID = 3614827395326873440L;

    }

    @Serial
    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required.");
    }

    @Serial
    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    private static NoSuchElementException missing(int tag) {
        return new NoSuchElementException("Missing "
                + ElementDictionary.keywordOf(tag) + ' '
                + TagUtils.toString(tag));
    }

    public class Sequence {
        private static final DicomObject[] EMPTY_SEQUENCE = {};
        final int tag;
        private DicomObject[] items = EMPTY_SEQUENCE;
        private int size;

        private Sequence(int tag) {
            this.tag = tag;
        }

        private Sequence(Sequence o) {
            this(o.tag);
            this.items = new DicomObject[o.size];
            this.size = o.size;
            for (int i = 0; i < o.size; i++) {
                items[i] = new DicomObject(o.items[i]);
            }
        }

        public DicomObject containedBy() {
            return DicomObject.this;
        }

        public void add(DicomObject dcmObj) {
            int index = size++;
            ensureCapacity(index);
            items[index] = dcmObj;
        }

        private void ensureCapacity(int index) {
            int oldCapacity = items.length;
            if (index < oldCapacity) return;
            if (oldCapacity == 0) {
                items = new DicomObject[1];
            } else {
                items = Arrays.copyOf(items, oldCapacity == 1 ? 16 : oldCapacity << 1);
            }
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public int size() {
            return size;
        }

        public DicomObject getItem(int index) {
            Objects.checkIndex(index, size);
            return items[index];
        }

        int promptItemsTo(StringBuilder sb, int maxColumns, int maxLines) {
            for (int i = 0; i < size; i++) {
                if (--maxLines < 0) break;
                sb.append(System.lineSeparator());
                DicomObject item = items[i];
                item.promptLevelTo(sb)
                        .append("(FFFE,E000) #")
                        .append(item.length)
                        .append(" Item #").append(i + 1)
                        .append(System.lineSeparator());
                maxLines = item.promptTo(sb, maxColumns, maxLines);
            }
            return maxLines;
        }
    }

    public class Fragments {
        private static final long[] EMPTY = {};
        final int tag;
        private long[] headers = EMPTY;
        private int size;

        private Fragments(int tag) {
            this.tag = tag;
        }

        private Fragments(Fragments o) {
            this(o.tag);
            this.headers = Arrays.copyOf(o.headers, o.size);
            this.size = o.size;
        }

        public void add(long header) {
            int index = size++;
            ensureCapacity(index);
            headers[index] = header;
        }

        private void ensureCapacity(int index) {
            int oldCapacity = headers.length;
            if (index < oldCapacity) return;
            if (oldCapacity == 0) {
                headers = new long[2];
            } else {
                headers = Arrays.copyOf(headers, oldCapacity == 2 ? 16 : oldCapacity << 1);
            }
        }

        public int size() {
            return size;
        }

        public DicomObject containedBy() {
            return DicomObject.this;
        }
    }
}
