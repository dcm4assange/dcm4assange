package org.dcm4assange;

import org.dcm4assange.MemoryCache.DicomInput;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2021
 */
public class DicomInputStream2 extends InputStream {
    @FunctionalInterface
    public interface PreambleHandler {
        void accept(DicomInputStream2 dis) throws IOException;
    }
    @FunctionalInterface
    public interface DicomElementHandler {
        boolean apply(DicomInputStream2 dis, DicomObject2 dcmobj, long header)
                throws IOException;
    }
    @FunctionalInterface
    public interface DicomElementPredicate {
        boolean test(DicomObject2 dcmobj, int tag, VR vr, int vallen);
    }
    @FunctionalInterface
    public interface ItemHandler {
        boolean apply(DicomInputStream2 dis, List<DicomObject2> items, DicomObject2 parent, int seqtag, long header)
                throws IOException;
    }
    @FunctionalInterface
    public interface FragmentHandler {
        boolean apply(DicomInputStream2 dis, List<Long> fragments, DicomObject2 parent, int seqtag, long header)
                throws IOException;
    }
    private static final byte[] END_OF_SEQ = {
            (byte) 0xFE,
            (byte) 0xFF,
            (byte) 0xDD,
            (byte) 0xE0,
            0, 0, 0, 0 };
    private Path path;
    private InputStream in;
    private DicomInput input;
    private long pos;
    private Path bulkDataSpoolPath;
    private OutputStream bulkDataOutputStream;
    private long bulkDataPos;
    private final MemoryCache cache = new MemoryCache();
    private DicomElementHandler onElement = DicomInputStream2::onElement;
    private ItemHandler onItem = DicomInputStream2::onItem;
    private FragmentHandler onFragment = DicomInputStream2::onFragment;
    private PreambleHandler preambleHandler = x -> {};
    private DicomElementPredicate bulkDataPredicate = (dcmObj, tag, vr, valueLength) -> false;
    private DicomObject2 fmi;

    public DicomInputStream2(Path path, DicomElementPredicate bulkDataPredicate)
            throws IOException {
        this(Files.newInputStream(path));
        this.path = path;
        this.bulkDataPredicate = Objects.requireNonNull(bulkDataPredicate);
    }

    public DicomInputStream2(InputStream in) {
        this.in = Objects.requireNonNull(in);
    }

    public static boolean bulkDataPredicate(DicomObject2 dcmobj, int tag, VR vr, int valueLength) {
        switch (tag) {
            case Tag.PixelData:
            case Tag.FloatPixelData:
            case Tag.DoubleFloatPixelData:
            case Tag.SpectroscopyData:
            case Tag.EncapsulatedDocument:
                return dcmobj.isRoot();
            case Tag.WaveformData:
                return isWaveformSequenceItem(dcmobj);
        }
        switch (tag & 0xFF01FFFF) {
            case Tag.OverlayData:
            case Tag.CurveData:
            case Tag.AudioSampleData:
                return dcmobj.isRoot();
        }
        return false;
    }

    private static boolean isWaveformSequenceItem(DicomObject2 item) {
        DicomObject2 parent = item.getParent();
        return parent != null
                && item.getSequenceTag() == Tag.WaveformSequence
                && parent.isRoot();
    }

    public DicomEncoding encoding() {
        return input != null ? input.encoding : null;
    }

    public DicomInputStream2 withEncoding(DicomEncoding encoding) throws IOException {
        input = cache.dicomInput(encoding);
        if (encoding.deflated) {
            in = cache.inflate(in, pos);
        }
        return this;
    }

    public DicomInputStream2 withPreambleHandler(PreambleHandler handler) {
        this.preambleHandler = Objects.requireNonNull(handler);
        return this;
    }

    public DicomInputStream2 withDicomElementHandler(DicomElementHandler handler) {
        this.onElement = Objects.requireNonNull(handler);
        return this;
    }

    public DicomInputStream2 withoutBulkData() {
        return withoutBulkData(DicomInputStream2::bulkDataPredicate);
    }

    public DicomInputStream2 withoutBulkData(DicomElementPredicate bulkDataPredicate) {
        this.bulkDataPredicate = Objects.requireNonNull(bulkDataPredicate);
        return this;
    }

    public DicomInputStream2 spoolBulkDataTo(Path bulkDataSpoolPath) {
        return spoolBulkDataTo(bulkDataSpoolPath, DicomInputStream2::bulkDataPredicate);
    }

    public DicomInputStream2 spoolBulkDataTo(Path bulkDataSpoolPath, DicomElementPredicate bulkDataPredicate) {
        this.bulkDataSpoolPath = Objects.requireNonNull(bulkDataSpoolPath);
        this.bulkDataPredicate = Objects.requireNonNull(bulkDataPredicate);
        return this;
    }

    public DicomInputStream2 withItemHandler(ItemHandler handler) {
        this.onItem = Objects.requireNonNull(handler);
        return this;
    }

    public DicomInputStream2 withFragmentHandler(FragmentHandler handler) {
        this.onFragment = Objects.requireNonNull(handler);
        return this;
    }

    public long streamPosition() {
        return pos;
    }

    public void seek(long pos) {
        this.pos = pos;
    }

    @Override
    public int read() throws IOException {
        if (cache.fillFrom(in, pos + 1) == pos)
            return -1;

        return cache.byteAt(pos++);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0)
            return 0;

        int read = (int) (cache.fillFrom(in, pos + len) - pos);
        if (read == 0)
            return -1;

        cache.copyBytesTo(pos, b, off, read);
        pos += read;
        return read;
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } finally {
            if (bulkDataOutputStream != null) bulkDataOutputStream.close();
        }
    }

    public long fillCache(long length) throws IOException {
        return cache.fillFrom(in, length);
    }

    public void skip(long pos, long length, OutputStream out) throws IOException {
        cache.skipFrom(in, pos, length, out);
    }

    public DicomObject2 readCommandSet() throws IOException {
        if (pos != 0)
            throw new IllegalStateException("Stream position: " + pos);

        input = cache.dicomInput(DicomEncoding.IVR_LE);
        DicomObject2 dcmObj = new DicomObject2(input);
        parse(dcmObj, -1);
        return dcmObj;
    }

    public DicomObject2 readDataSet() throws IOException {
        if (input == null) {
            guessEncoding();
        }
        DicomObject2 dcmObj = new DicomObject2(input);
        parse(dcmObj, -1);
        return dcmObj;
    }

    public DicomObject2 fileMetaInformation() {
        return fmi;
    }

    public DicomObject2 readFileMetaInformation() throws IOException {
        if (pos != 0)
            throw new IllegalStateException("Stream position: " + pos);

        long read = cache.fillFrom(in, 144);
        byte[] b = cache.block(0);
        if (read != 144
                || b[128] != 'D'
                || b[129] != 'I'
                || b[130] != 'C'
                || b[131] != 'M'
                || b[132] != 2
                || b[133] != 0)
            return null;

        preambleHandler.accept(this);
        pos = 132;
        input = cache.dicomInput(DicomEncoding.EVR_LE);
        DicomObject2 fmi = new DicomObject2(input);
        long header  = parseHeader(fmi, input);
        VR vr = HeaderType.vr(header);
        HeaderType headerType = HeaderType.of(header);
        int tag = headerType.tag(header, input);
        int vallen = headerType.valueLength(header, input);
        if (tag != Tag.FileMetaInformationGroupLength || vr != VR.UL || vallen != 4) {
            throw new DicomParseException("Missing Group Length in File Meta Information");
        }
        int groupLength = input.intAt(pos);
        onElement.apply(this, fmi, header);
        parse(fmi, groupLength);
        String tsuid = fmi.getString(Tag.TransferSyntaxUID).orElseThrow(
                () -> new DicomParseException("Missing Transfer Syntax UID in File Meta Information"));
        withEncoding(DicomEncoding.of(tsuid));
        this.fmi = fmi;
        return fmi;
    }

    public boolean onElement(DicomObject2 dcmObj, long header)
            throws IOException {
        VR vr = HeaderType.vr(header);
        HeaderType headerType = HeaderType.of(header);
        int tag = headerType.tag(header, input);
        int vallen = headerType.valueLength(header, input);
        long unsignedValueLength = vallen & 0xffffffffL;
        if (vr != null) {
            if (vr == VR.SQ) {
                List<DicomObject2> items = new ArrayList<>();
                dcmObj.add(header, items);
                return parseItems(items, dcmObj, tag, vallen);
            }
            boolean bulkData = bulkDataPredicate.test(dcmObj, tag, vr, vallen);
            if (bulkData) {
                if (bulkDataSpoolPath != null) {
                    if (bulkDataOutputStream == null) {
                        bulkDataOutputStream = Files.newOutputStream(bulkDataSpoolPath);
                    }
                    dcmObj.add(header, bulkDataURI(bulkDataSpoolPath, bulkDataPos, vallen));
                } else if (path != null) {
                    dcmObj.add(header, bulkDataURI(path, pos, vallen));
                }
                if (vallen == -1) {
                    if (!parseFragments(null, dcmObj, tag)) return false;
                    if (bulkDataOutputStream != null) {
                        bulkDataOutputStream.write(END_OF_SEQ);
                        bulkDataPos += END_OF_SEQ.length;
                    }
                    return true;
                }
                skip(pos, unsignedValueLength, bulkDataOutputStream);
                bulkDataPos += unsignedValueLength;
            } else if (vallen == -1) {
                List<Long> fragments = new ArrayList<>();
                dcmObj.add(header, fragments);
                return parseFragments(fragments, dcmObj, tag);
            } else {
                dcmObj.add(header, null);
            }
        }
        this.pos += unsignedValueLength;
        return true;
    }

    private static String bulkDataURI(Path path, long offset, int length) {
        StringBuilder sb = new StringBuilder(path.toUri().toString())
                .append("#offset=").append(offset);
        if (length == -1)
            sb.append(",length=-1");
        else
            sb.append(",length=").append((length & 0xffffffffL));
        return sb.toString();
    }

    public boolean onItem(List<DicomObject2> items, DicomObject2 parent, int seqtag, long header)
            throws IOException {
        HeaderType headerType = HeaderType.of(header);
        int itemtag = headerType.tag(header, input);
        int itemlen = headerType.valueLength(header, input);
        if (itemtag == Tag.Item) {
            DicomObject2 dcmObj = new DicomObject2(input, parent, seqtag);
            items.add(dcmObj);
            if (!parse(dcmObj, itemlen))
                return false;
        } else {
            this.pos += itemlen & 0xffffffffL;
        }
        return true;
    }

    public boolean onFragment(List<Long> fragments, DicomObject2 parent, int seqtag, long header)
            throws IOException {
        HeaderType headerType = HeaderType.of(header);
        int itemtag = headerType.tag(header, input);
        int itemlen = headerType.valueLength(header, input);
        long uitemlen = itemlen & 0xffffffffL;
        if (itemtag == Tag.Item) {
            if (fragments != null) {
                fragments.add(uitemlen);
            } else {
                skip(pos - 8, 8 + uitemlen, bulkDataOutputStream);
                bulkDataPos += 8 + uitemlen;
            }
        }
        this.pos += uitemlen;
        return true;
    }

    int itemLength(long itemPosition) {
        return input.intAt(itemPosition + 4);
    }

    boolean parseItem(long itemPosition, DicomObject2 dcmObj) throws IOException {
        long prevPos = this.pos;
        this.pos = itemPosition + 8;
        try {
            return parse(dcmObj, input.intAt(itemPosition + 4));
        } finally {
            this.pos = prevPos;
        }
    }

    private void guessEncoding() throws IOException {
        if (readFileMetaInformation() == null) {
            DicomEncoding encoding = !probeExplicitVR(4) ? DicomEncoding.IVR_LE
                    : (cache.byteAt(1) == 0 ? DicomEncoding.EVR_LE : DicomEncoding.EVR_BE);
            input = cache.dicomInput(encoding);
            int valueLength = !encoding.explicitVR ? input.intAt(pos + 4)
                    : VR.of(cache.vrcode(pos + 4)).evr8 ? input.ushortAt(pos + 6)
                    : input.intAt(pos + 8);
            if (valueLength > 64)
                throw new DicomParseException("Not a DICOM stream");
        }
    }

    private boolean probeExplicitVR(long pos) {
        return VR.of(cache.vrcode(pos)) != null;
    }

    private long parseHeader(DicomObject2 dcmObj, DicomInput input) throws IOException {
        long pos0 = pos;
        if (cache.fillFrom(in, pos0 + 8) < pos0 + 8) {
            throw new EOFException();
        }
        pos += 8;
        int tag = input.tagAt(pos0);
        switch (tag) {
            case Tag.Item:
            case Tag.ItemDelimitationItem:
            case Tag.SequenceDelimitationItem:
                return HeaderType.IVR_POS.encode(pos0);
        }
        if (!input.encoding.explicitVR) {
            return HeaderType.IVR_POS.encode(pos0) | HeaderType.encode(lookupVR(tag, dcmObj));
        }
        int vrcode = cache.vrcode(pos0 + 4);
        boolean vrcodeUN = vrcode == VR.UN.code;
        VR vr = vrcodeUN ? VR.UN : vrOf(vrcode, tag, dcmObj);
        if (vr.evr8) {
            return HeaderType.EVR_8_POS.encode(pos0) | HeaderType.encode(vr);
        }
        if (pos0 + 12 > cache.limit()) {
            throw new EOFException();
        }
        pos += 4;
        return HeaderType.EVR_12_POS.encode(pos0) | HeaderType.encode(vr);
    }

    private static VR vrOf(int vrCode, int tag, DicomObject2 dcmObj) {
        try {
            return VR.of(vrCode);
        } catch (IllegalArgumentException e) {
            return lookupVR(tag, dcmObj);
        }
    }

    private static VR lookupVR(int tag, DicomObject2 dcmObj) {
        return ElementDictionary.vrOf(
                dcmObj != null ? dcmObj.privateCreatorOf(tag).orElse(null) : null,
                tag
        );
    }

    public boolean parse(DicomObject2 dcmObj, int length) throws IOException {
        boolean undefinedLength = length == -1;
        long endPos = pos + length & 0xffffffffL;
        DicomInput input = this.input;
        if (length != 0 && input.encoding.explicitVR) {
            if (cache.fillFrom(in,  pos + 6) == pos + 6 && !probeExplicitVR(pos + 4)) {
                input = cache.dicomInput(DicomEncoding.IVR_LE);
            }
        }
        while (undefinedLength || pos < endPos) {
            long pos0 = pos;
            long header;
            try {
                header = parseHeader(dcmObj, input);
            } catch (EOFException e) {
                if (undefinedLength && dcmObj.isRoot() && pos0 == cache.limit()) break;
                throw e;
            }
            HeaderType headerType = HeaderType.of(header);
            int tag = headerType.tag(header, input);
            if (tag == Tag.SpecificCharacterSet) {
                cache.fillFrom(in, pos + headerType.valueLength(header, input));
            }
            if (!onElement.apply(this, dcmObj, header)) return false;
            if (tag == Tag.ItemDelimitationItem) break;
        }
        return true;
    }

    public boolean parseItems(List<DicomObject2> items, DicomObject2 parent, int seqtag, int length)
            throws IOException {
        if (length == 0) return true;
        boolean undefinedLength = length == -1;
        long endPos = pos + length & 0xffffffffL;
        while (undefinedLength || pos < endPos) {
            long header = parseHeader(parent, input);
            int itemtag = HeaderType.IVR_POS.tag(header, input);
            if (!onItem.apply(this, items, parent, seqtag, header)) return false;
            if (itemtag == Tag.SequenceDelimitationItem) break;
        }
        return true;
    }

    public boolean parseFragments(List<Long> fragments, DicomObject2 parent, int seqtag)
            throws IOException {
        for (;;) {
            long header = parseHeader(parent, input);
            int itemtag = HeaderType.IVR_POS.tag(header, input);
            if (!onFragment.apply(this, fragments, parent, seqtag, header)) return false;
            if (itemtag == Tag.SequenceDelimitationItem) break;
        }
        return true;
    }

    public StringBuilder promptPreambleTo(StringBuilder sb, int cols) {
        sb.append('[');
        for (int i = 0; i < 128; i++) {
            if (i > 0) {
                sb.append('\\');
            }
            sb.append(cache.byteAt(i));
            if (sb.length() >= cols) {
                sb.setLength(cols);
                return sb;
            }
        }
        return sb.append(']');
    }

    enum HeaderType {
        VR_TAG(0) {
            @Override
            int tag(long header, DicomInput dicomInput) {
                return (int) header;
            }

            @Override
            int valueLength(long header, DicomInput dicomInput) {
                throw new UnsupportedOperationException();
            }
        },
        IVR_POS(8) {
            @Override
            int valueLength(long header, DicomInput dicomInput) {
                return dicomInput.intAt((header & POS_BITS) + 4);
            }
        },
        EVR_8_POS(8) {
            @Override
            int valueLength(long header, DicomInput dicomInput) {
                return dicomInput.ushortAt((header & POS_BITS) + 6);
            }
        },
        EVR_12_POS(12) {
            @Override
            int valueLength(long header, DicomInput dicomInput) {
                return dicomInput.intAt((header & POS_BITS) + 8);
            }
        };

        static final long POS_BITS = 0x00ffffffffffffffL;
        final int headerLength;

        HeaderType(int headerLength) {
            this.headerLength = headerLength;
        }

        static HeaderType of(long header) {
            return HeaderType.values()[(int) (header >>> 62)];
        }

        long encode(long pos) {
            return (long) ordinal() << 62 | pos;
        }

        static long encode(VR vr) {
            return ((long) (vr.ordinal() + 1) << 56);
        }

        static VR vr(long header) {
            int i = ((int) (header >> 56)) & 0x3f;
            return i > 0 ? VR.values()[i-1] : null;
        }

        static int tagOf(long header, DicomInput dicomInput) {
            return of(header).tag(header, dicomInput);
        }

        int tag(long header, DicomInput dicomInput) {
            return dicomInput.tagAt(header & POS_BITS);
        }

        long valuePosition(long header) {
            return (header & POS_BITS) + headerLength;
        }

        int valueLength(long header, DicomInput dicomInput) {
            throw new UnsupportedOperationException();
        }
    }
}
