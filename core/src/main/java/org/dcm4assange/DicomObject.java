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
public interface DicomObject {
    static DicomObject newDicomObject() {
        return new WriteableDicomObject();
    }

    SpecificCharacterSet specificCharacterSet();

    int size();

    boolean isEmpty();

    boolean isRoot();

    DicomElement sequence();

    Optional<String> privateCreatorOf(int tag);

    Optional<DicomElement> get(int tag);

    Optional<String> getString(int tag);

    String[] getStrings(int tag);

    OptionalInt getInt(int tag);

    OptionalLong getLong(int tag);

    OptionalFloat getFloat(int tag);

    OptionalDouble getDouble(int tag);

    DicomElement setEmpty(int tag, VR vr);

    DicomElement setString(int tag, VR vr, String value);

    DicomElement setStrings(int tag, VR vr, String... values);

    DicomElement setInt(int tag, VR vr, int val);

    DicomElement setInt(int tag, VR vr, int... vals);

    DicomElement add(DicomElement dcmElm);

    int promptTo(StringBuilder appendTo, int maxColumns, int maxLines);

    int itemLength();

    int calculateItemLength(DicomOutputStream dicomOutputStream);

    int calculatedItemLength();

    void writeTo(DicomOutputStream dicomOutputStream) throws IOException;
}
