package org.dcm4assange;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
class FileBulkDataElement extends BasicDicomElement {
    private final Path file;
    private final long offset;

    FileBulkDataElement(Path file, long offset, DicomElement dcmElm) {
        super(dcmElm.containedBy(), dcmElm.tag(), dcmElm.vr(), dcmElm.valueLength());
        this.file = file;
        this.offset = offset;
    }

    @Override
    public Optional<String> bulkDataURI() {
        StringBuilder sb = new StringBuilder(file.toUri().toString())
                .append("#offset=").append(offset);
        if (valueLength == -1)
            sb.append(",length=-1");
        else
            sb.append(",length=").append((valueLength & 0xffffffffL));
        return Optional.of(sb.toString());
    }

    @Override
    public void writeValueTo(DicomOutputStream dos) throws IOException {
        byte[] buffer = new byte[8192];
        try (InputStream in = Files.newInputStream(file)) {
            transferTo(in, offset, buffer, null);
            if (valueLength != -1) {
                transferTo(in, valueLength & 0xffffffffL, buffer, dos);
            } else {
                in.read(buffer, 0, 8);
                while (ByteOrder.LITTLE_ENDIAN.bytesToTag(buffer, 0) == Tag.Item) {
                    dos.write(buffer, 0, 8);
                    transferTo(in, ByteOrder.LITTLE_ENDIAN.bytesToInt(buffer, 4) & 0xffffffffL, buffer, dos);
                    in.read(buffer, 0, 8);
                }
            }
        }
    }

    private void transferTo(InputStream in, long length, byte[] buffer, OutputStream out) throws IOException {
        int read;
        while (length > 0 && (read = in.read(buffer, 0, (int) Math.min(length, buffer.length))) >= 0) {
            if (out != null) out.write(buffer, 0, read);
            length -= read;
        }
        if (length > 0)
            throw new EOFException("EOF on reading " + length + " bytes from " + file);
    }
}
