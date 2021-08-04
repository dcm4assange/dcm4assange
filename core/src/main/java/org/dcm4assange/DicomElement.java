package org.dcm4assange;

import org.dcm4assange.util.OptionalFloat;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2021
 */
public interface DicomElement {
    DicomObject containedBy();
    int tag();
    VR vr();
    int valueLength();
    boolean isEmpty();
    OptionalInt intValue();
    OptionalLong longValue();
    OptionalFloat floatValue();
    OptionalDouble doubleValue();
    Optional<String> stringValue();
    Optional<String> bulkDataURI();
    String[] stringValues();
    int numberOfItems();
    DicomObject addItem();
    void addItem(DicomObject item);
    DicomObject getItem(int index) throws IOException;
    StringBuilder promptTo(StringBuilder appendTo, int maxLength);
    int promptItemsTo(StringBuilder appendTo, int maxColumns, int maxLines);
    StringBuilder promptLevelTo(StringBuilder appendTo);
    int elementLength(DicomOutputStream dos);
    int valueLength(DicomOutputStream dos);
    void writeValueTo(DicomOutputStream dos) throws IOException;
}
