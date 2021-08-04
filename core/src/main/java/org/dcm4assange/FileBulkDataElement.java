package org.dcm4assange;

import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
class FileBulkDataElement extends BasicDicomElement {
    private final Path file;
    private final long valuePos;

    FileBulkDataElement(Path file, long valuePos, DicomElement dcmElm) {
        super(dcmElm.containedBy(), dcmElm.tag(), dcmElm.vr(), dcmElm.valueLength());
        this.file = file;
        this.valuePos = valuePos;
    }

    @Override
    public Optional<String> bulkDataURI() {
        StringBuilder sb = new StringBuilder(file.toUri().toString())
                .append("#offset=").append(valuePos);
        if (valueLength == -1)
            sb.append(",length=-1");
        else
            sb.append(",length=").append((valueLength & 0xffffffffL));
        return Optional.of(sb.toString());
    }
}
