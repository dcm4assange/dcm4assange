package org.dcm4assange;

import org.dcm4assange.util.OptionalFloat;
import org.dcm4assange.util.StringUtils;
import org.dcm4assange.util.TagUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2021
 */
class BasicDicomElement implements DicomElement {
    static final int TO_STRING_LENGTH = 78;
    protected final DicomObject dicomObject;
    protected final int tag;
    protected final VR vr;
    protected final int valueLength;

    BasicDicomElement(DicomObject dicomObject, int tag, VR vr, int valueLength) {
        this.dicomObject = dicomObject;
        this.tag = tag;
        this.vr = Objects.requireNonNull(vr);
        this.valueLength = valueLength;
    }

    @Override
    public DicomObject dicomObject() {
        return dicomObject;
    }

    @Override
    public int tag() {
        return tag;
    }

    @Override
    public VR vr() {
        return vr;
    }

    @Override
    public int valueLength() {
        return valueLength;
    }

    @Override
    public boolean isEmpty() {
        return valueLength == 0;
    }

    @Override
    public OptionalInt intValue() {
        return OptionalInt.empty();
    }

    @Override
    public OptionalLong longValue() {
        return OptionalLong.empty();
    }

    @Override
    public OptionalFloat floatValue() {
        return OptionalFloat.empty();
    }

    @Override
    public OptionalDouble doubleValue() {
        return OptionalDouble.empty();
    }

    @Override
    public Optional<String> stringValue() {
        return Optional.empty();
    }

    @Override
    public Optional<String> bulkDataURI() {
        return Optional.empty();
    }

    @Override
    public String[] stringValues() {
        return StringUtils.EMPTY_STRINGS;
    }

    @Override
    public int numberOfItems() {
        return 0;
    }

    @Override
    public void addItem(DicomObject item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DicomObject addItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DicomObject getItem(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return promptTo(new StringBuilder(TO_STRING_LENGTH), TO_STRING_LENGTH).toString();
    }

    @Override
    public StringBuilder promptTo(StringBuilder appendTo, int maxLength) {
        promptLevelTo(appendTo).append(TagUtils.toCharArray(tag));
        if (vr != VR.NONE) appendTo.append(' ').append(vr);
        appendTo.append(" #").append(valueLength);
        promptValueTo(appendTo, maxLength);
        if (appendTo.length() < maxLength) {
            appendTo.append(" ").append(
                    ElementDictionary.keywordOf(tag, dicomObject.privateCreatorOf(tag).orElse(null)));
            if (appendTo.length() > maxLength) {
                appendTo.setLength(maxLength);
            }
        }
        return appendTo;
    }

    @Override
    public int promptItemsTo(StringBuilder appendTo, int maxColumns, int maxLines) {
        return maxLines;
    }

    protected StringBuilder promptValueTo(StringBuilder appendTo, int maxLength) {
        return appendTo.append(" []");
    }

    @Override
    public StringBuilder promptLevelTo(StringBuilder appendTo) {
        for (DicomElement seq = dicomObject.sequence(); seq != null; seq = seq.dicomObject().sequence()) {
            appendTo.append('>');
        }
        return appendTo;
    }

    @Override
    public int elementLength(DicomOutputStream dos) {
        return (dos.encoding().explicitVR && !vr.evr8 ? 12 : 8) + valueLength();

    }

    @Override
    public int valueLength(DicomOutputStream dos) {
        return valueLength();
    }

    @Override
    public void writeValueTo(DicomOutputStream dos) throws IOException {
    }
}
