package org.dcm4assange;

import org.dcm4assange.MemoryCache.DicomInput;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2021
 */
public class DicomInputStream extends InputStream {
    @FunctionalInterface
    public interface PreambleHandler {
        void accept(DicomInputStream dis) throws IOException;
    }
    @FunctionalInterface
    public interface DicomElementHandler {
        boolean apply(DicomInputStream dis, long pos, DicomElement dcmElm) throws IOException;
    }
    @FunctionalInterface
    public interface ItemHandler {
        boolean apply(DicomInputStream dis, long pos, DicomElement dcmElm, DicomElement itemHeader) throws IOException;
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
    private DicomElementHandler onElement = DicomInputStream::onElement;
    private ItemHandler onItem = DicomInputStream::onItem;
    private ItemHandler onFragment = DicomInputStream::onFragment;
    private PreambleHandler preambleHandler = x -> {};
    private Predicate<DicomElement> bulkDataPredicate = x -> false;
    private Predicate<DicomElement> parseItemsPredicate = x -> true;
    private DicomObject fmi;

    public DicomInputStream(Path path, Predicate<DicomElement> bulkDataPredicate) throws IOException {
        this(Files.newInputStream(path));
        this.path = path;
        this.bulkDataPredicate = Objects.requireNonNull(bulkDataPredicate);
    }

    public DicomInputStream(InputStream in) {
        this.in = Objects.requireNonNull(in);
    }

    public static boolean bulkDataPredicate(DicomElement dcmElm) {
        switch (dcmElm.tag()) {
            case Tag.PixelData:
            case Tag.FloatPixelData:
            case Tag.DoubleFloatPixelData:
            case Tag.SpectroscopyData:
            case Tag.EncapsulatedDocument:
                return dcmElm.containedBy().isRoot();
            case Tag.WaveformData:
                return isWaveformSequence(dcmElm.containedBy().containedBy());
        }
        switch (dcmElm.tag() & 0xFF01FFFF) {
            case Tag.OverlayData:
            case Tag.CurveData:
            case Tag.AudioSampleData:
                return dcmElm.containedBy().isRoot();
        }
        return false;
    }

    private static boolean isWaveformSequence(DicomElement sequence) {
        return sequence != null
                && sequence.tag() == Tag.WaveformSequence
                && sequence.containedBy().isRoot();
    }

    public DicomEncoding encoding() {
        return input != null ? input.encoding : null;
    }

    public DicomInputStream withEncoding(DicomEncoding encoding) throws IOException {
        input = cache.dicomInput(encoding);
        if (encoding.deflated) {
            in = cache.inflate(in, pos);
        }
        return this;
    }

    public DicomInputStream withPreambleHandler(PreambleHandler handler) {
        this.preambleHandler = Objects.requireNonNull(handler);
        return this;
    }

    public DicomInputStream withDicomElementHandler(DicomElementHandler handler) {
        this.onElement = Objects.requireNonNull(handler);
        return this;
    }

    public DicomInputStream withoutBulkData() {
        return withoutBulkData(DicomInputStream::bulkDataPredicate);
    }

    public DicomInputStream withoutBulkData(Predicate<DicomElement> bulkDataPredicate) {
        this.bulkDataPredicate = Objects.requireNonNull(bulkDataPredicate);
        return this;
    }

    public DicomInputStream spoolBulkDataTo(Path bulkDataSpoolPath) {
        return spoolBulkDataTo(bulkDataSpoolPath, DicomInputStream::bulkDataPredicate);
    }

    public DicomInputStream spoolBulkDataTo(Path bulkDataSpoolPath, Predicate<DicomElement> bulkDataPredicate) {
        this.bulkDataSpoolPath = Objects.requireNonNull(bulkDataSpoolPath);
        this.bulkDataPredicate = Objects.requireNonNull(bulkDataPredicate);
        return this;
    }

    public DicomInputStream withItemHandler(ItemHandler handler) {
        this.onItem = Objects.requireNonNull(handler);
        return this;
    }

    public DicomInputStream withFragmentHandler(ItemHandler handler) {
        this.onFragment = Objects.requireNonNull(handler);
        return this;
    }

    public DicomInputStream withParseItems(Predicate<DicomElement> parseItemsPredicate) {
        this.parseItemsPredicate = Objects.requireNonNull(parseItemsPredicate);
        return this;
    }

    public DicomInputStream withParseItemsLazy(int seqTag) {
        return withParseItems(x -> x.tag() != seqTag);
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

    public DicomObject readCommandSet() throws IOException {
        if (pos != 0)
            throw new IllegalStateException("Stream position: " + pos);

        input = cache.dicomInput(DicomEncoding.IVR_LE);
        DicomObject dcmObj = new WriteableDicomObject();
        parse(dcmObj, -1);
        return dcmObj;
    }

    public DicomObject readDataSet() throws IOException {
        DicomObject dcmObj = new WriteableDicomObject();
        readDataSet(dcmObj);
        return dcmObj;
    }

    public boolean readDataSet(DicomObject dcmObj) throws IOException {
        Objects.requireNonNull(dcmObj);
        if (input == null) {
            guessEncoding();
        }
        return parse(dcmObj, -1);
    }

    public DicomObject fileMetaInformation() {
        return fmi;
    }

    public DicomObject readFileMetaInformation() throws IOException {
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
        DicomObject fmi = new WriteableDicomObject();
        DicomElement groupLengthElement  = parseHeader(fmi, input);
        if (groupLengthElement.tag() != Tag.FileMetaInformationGroupLength
                || groupLengthElement.vr() != VR.UL
                || groupLengthElement.valueLength() != 4) {
            throw new DicomParseException("Missing Group Length in File Meta Information");
        }
        int groupLength = input.intAt(pos);
        onElement.apply(this, 132, groupLengthElement);
        parse(fmi, groupLength);
        String tsuid = fmi.getString(Tag.TransferSyntaxUID).orElseThrow(
                () -> new DicomParseException("Missing Transfer Syntax UID in File Meta Information"));
        withEncoding(DicomEncoding.of(tsuid));
        this.fmi = fmi;
        return fmi;
    }

    public boolean onElement(long pos, DicomElement dcmElm) throws IOException {
        int valueLength = dcmElm.valueLength();
        long unsignedValueLength = valueLength & 0xffffffffL;
        if (dcmElm.vr() != VR.NONE) {
            boolean bulkData = bulkDataPredicate.test(dcmElm) && dcmElm.vr() != VR.SQ;
            if (bulkData) {
                if (bulkDataSpoolPath != null) {
                    if (bulkDataOutputStream == null) {
                        bulkDataOutputStream = Files.newOutputStream(bulkDataSpoolPath);
                    }
                    dcmElm.containedBy().add(dcmElm = new FileBulkDataElement(bulkDataSpoolPath, bulkDataPos, dcmElm));
                } else if (path != null) {
                    dcmElm.containedBy().add(dcmElm = new FileBulkDataElement(path, this.pos, dcmElm));
                }
            } else {
                dcmElm.containedBy().add(dcmElm);
            }
            if (dcmElm.vr() == VR.SQ) {
                return parseItems(dcmElm, valueLength);
            } else if (valueLength == -1) {
                if (!parseFragments(dcmElm)) return false;
                if (bulkData && bulkDataOutputStream != null) {
                    bulkDataOutputStream.write(END_OF_SEQ);
                    bulkDataPos += END_OF_SEQ.length;
                }
                return true;
            } else if (bulkData) {
                skip(pos, unsignedValueLength, bulkDataOutputStream);
                bulkDataPos += unsignedValueLength;
            }
        }
        this.pos += unsignedValueLength;
        return true;
    }

    public boolean onItem(long pos, DicomElement dcmElm, DicomElement itemHeader) throws IOException {
        int tag = itemHeader.tag();
        int itemLength = itemHeader.valueLength();
        if (tag == Tag.Item) {
            boolean parseItem = parseItemsPredicate.test(dcmElm);
            WriteableDicomObject dcmObj = new WriteableDicomObject(
                    this, dcmElm, dcmElm.containedBy().specificCharacterSet(),
                    parseItem ? new ArrayList<>() : null);
            dcmElm.addItem(dcmObj);
            if (parseItem) {
                if (!parse(dcmObj, itemLength))
                    return false;
            } else {
                skip(itemLength);
            }
        } else {
            this.pos += itemLength & 0xffffffffL;
        }
        return true;
    }

    public boolean onFragment(long pos, DicomElement dcmElm, DicomElement itemHeader) throws IOException {
        long itemLength = itemHeader.valueLength() & 0xffffffffL;
        if (itemHeader.tag() == Tag.Item) {
            if (bulkDataPredicate.test(dcmElm)) {
                skip(pos - 8, 8 + itemLength, bulkDataOutputStream);
                bulkDataPos += 8 + itemLength;
            }
            this.pos += itemLength;
        } else {
            this.pos += itemLength;
        }
        return true;
    }

    int itemLength(long itemPosition) {
        return input.intAt(itemPosition + 4);
    }

    boolean parseItem(long itemPosition, DicomObject dcmObj) throws IOException {
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
        return switch (cache.vrcode(pos)) {
            case 0x4145, 0x4153, 0x4154, 0x4353, 0x4441, 0x4453, 0x4454, 0x4644, 0x464c, 0x4953, 0x4c4f, 0x4c54,
                    0x4f42, 0x4f44, 0x4f46, 0x4f4c, 0x4f56, 0x4f57, 0x504e, 0x5348, 0x534c, 0x5351, 0x5353,
                    0x5354, 0x5356, 0x544d, 0x5543, 0x5549, 0x554c, 0x554e, 0x5552, 0x5553, 0x5554, 0x5556
                    -> true;
            default -> false;
        };
    }

    private DicomElement parseHeader(DicomObject dcmObj, DicomInput input) throws EOFException {
        if (pos + 8 > cache.limit()) {
            throw new EOFException();
        }
        int tag = input.tagAt(pos);
        switch (tag) {
            case Tag.Item:
            case Tag.ItemDelimitationItem:
            case Tag.SequenceDelimitationItem:
                return input.newDicomElement(dcmObj, tag, VR.NONE, input.intAt(pos + 4), pos += 8);
        }
        if (!input.encoding.explicitVR) {
            return input.newDicomElement(dcmObj, tag, lookupVR(tag, dcmObj), input.intAt(pos + 4), pos += 8);
        }
        int vrcode = cache.vrcode(pos + 4);
        boolean vrcodeUN = vrcode == VR.UN.code;
        VR vr = vrcodeUN ? VR.UN : vrOf(vrcode, tag, dcmObj);
        if (vr.evr8) {
            return input.newDicomElement(dcmObj, tag, vr, input.ushortAt(pos + 6), pos += 8);
        }
        if (pos + 12 > cache.limit()) {
            throw new EOFException();
        }
        return input.newDicomElement(dcmObj, tag, vrcodeUN ? lookupVR(tag, dcmObj) : vr,
                input.intAt(pos + 8), pos += 12);
    }

    private static VR vrOf(int vrCode, int tag, DicomObject dcmObj) {
        try {
            return VR.of(vrCode);
        } catch (IllegalArgumentException e) {
            return lookupVR(tag, dcmObj);
        }
    }

    private static VR lookupVR(int tag, DicomObject dcmObj) {
        return ElementDictionary.vrOf(dcmObj != null ? dcmObj.privateCreatorOf(tag).orElse(null) : null, tag
        );
    }

    public boolean parse(DicomObject dcmObj, int length) throws IOException {
        boolean undefinedLength = length == -1;
        boolean expectEOF = undefinedLength && dcmObj.isRoot();
        int tag = 0;
        long endPos = pos + length & 0xffffffffL;
        DicomInput input = this.input;
        if (length != 0 && input.encoding.explicitVR) {
            if (cache.fillFrom(in,  pos + 6) == pos + 6 && !probeExplicitVR(pos + 4)) {
                input = cache.dicomInput(DicomEncoding.IVR_LE);
            }
        }
        while (undefinedLength || pos < endPos) {
            long pos0 = pos;
            if (cache.fillFrom(in, pos0 + 12) == pos0 && expectEOF) break;
            DicomElement dcmElm = parseHeader(dcmObj, input);
            tag = dcmElm.tag();
            if (tag == Tag.SpecificCharacterSet) {
                cache.fillFrom(in, pos + dcmElm.valueLength());
            }
            if (!onElement.apply(this, pos0, dcmElm)) return false;
            if (tag == Tag.ItemDelimitationItem) break;
        }
        return true;
    }

    public boolean parseItems(DicomElement dcmElm, int length) throws IOException {
        if (length == 0) return true;
        boolean undefinedLength = length == -1;
        long endPos = pos + length;
        int tag = 0;
        while (undefinedLength || pos < endPos) {
            long pos0 = pos;
            cache.fillFrom(in, pos + 8);
            DicomElement itemHeader = parseHeader(dcmElm.containedBy(), input);
            tag = itemHeader.tag();
            if (!onItem.apply(this, pos0, dcmElm, itemHeader)) return false;
            if (tag == Tag.SequenceDelimitationItem) break;
        }
        return true;
    }

    public boolean parseFragments(DicomElement dcmElm) throws IOException {
        int tag;
        do {
            long pos0 = pos;
            cache.fillFrom(in, pos + 8);
            DicomElement itemHeader = parseHeader(dcmElm.containedBy(), input);
            tag = itemHeader.tag();
            if (!onFragment.apply(this, pos0, dcmElm, itemHeader)) return false;
        } while (tag != Tag.SequenceDelimitationItem);
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

    private void skip(int length) throws IOException {
        if (length != -1) {
            pos += length;
            return;
        }
        DicomElement dcmElm;
        do {
            cache.fillFrom(in, pos + 12);
            dcmElm = parseHeader(null, input);
            skipItems(dcmElm);
        } while (dcmElm.tag() != Tag.ItemDelimitationItem);
    }

    private void skipItems(DicomElement dcmElm)
            throws IOException {
        if (dcmElm.valueLength() != -1) {
            pos += dcmElm.valueLength();
            return;
        }
        DicomElement itemHeader;
        do {
            cache.fillFrom(in, pos + 8);
            itemHeader = parseHeader(null, input);
            skip(itemHeader.valueLength());
        } while (itemHeader.tag() != Tag.SequenceDelimitationItem);
    }
}
