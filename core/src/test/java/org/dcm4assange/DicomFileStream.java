package org.dcm4assange;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Aug 2021
 */
class DicomFileStream extends InputStream {
    private static final byte[] FMI_IVR_LE_PX_DATA= {'D', 'I', 'C', 'M',
            2, 0, 0, 0, 'U', 'L', 4, 0, 26, 0, 0, 0,
            2, 0, 16, 0, 'U', 'I', 18, 0, 49, 46, 50, 46, 56, 52, 48, 46, 49, 48, 48, 48, 56, 46, 49, 46, 50, 00,
            -32, 0x7F, 0x10, 0, 0, 0, 0, 0
    };
    private static final byte[] POST_IVR_LE_PX_DATA = {
            -4, -1, -4, -1, 0, 0, 0, 0
    };
    private static final byte[] FMI_ENCAPS_PX_DATA = {'D', 'I', 'C', 'M',
            2, 0, 0, 0, 'U', 'L', 4, 0, 30, 0, 0, 0,
            2, 0, 16, 0, 'U', 'I', 22, 0, 49, 46, 50, 46, 56, 52, 48, 46, 49, 48, 48, 48, 56, 46, 49, 46, 50, 46, 49, 46, 57, 56,
            -32, 0x7F, 0x10, 0, 'O', 'B', 0, 0, -1, -1, -1, -1,
            -2, -1, 0, -32, 0, 0, 0, 0,
            -2, -1, 0, -32, 0, 0, 0, 0,
    };
    private static final byte[] POST_ENCAPS_PX_DATA = {
            -2, -1, -35, -32, 0, 0, 0, 0,
            -4, -1, -4, -1, 'O', 'B', 0, 0, 0, 0, 0, 0
    };
    final byte[] prefix;
    final long pixelDataLength;
    final byte[] suffix;
    long position;
    long available;

    DicomFileStream(byte[] prefix, long pixelDataLength, byte[] suffix) {
        this.prefix = prefix.clone();
        ByteOrder.LITTLE_ENDIAN.intToBytes((int) pixelDataLength, this.prefix, prefix.length - 4);
        this.pixelDataLength = pixelDataLength;
        this.suffix = suffix;
        this.available = 128 + prefix.length + suffix.length + pixelDataLength;
    }

    static DicomFileStream ivrPxData(long pixelDataLength) {
        return new DicomFileStream(FMI_IVR_LE_PX_DATA, pixelDataLength, POST_IVR_LE_PX_DATA);
    }

    static DicomFileStream encapsPxData(long pixelDataLength) {
        return new DicomFileStream(FMI_ENCAPS_PX_DATA, pixelDataLength, POST_ENCAPS_PX_DATA);
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(0, Math.max(available, Integer.MAX_VALUE));
    }

    @Override
    public int read() throws IOException {
        if (--available < 0)
            return -1;
        long index = position++ - 128;
        if (index >= 0) {
            if (index < prefix.length && index >= 0)
                return prefix[(int) index] & 0xff;
            if ((index -= prefix.length + pixelDataLength) >= 0)
                return suffix[(int) index] & 0xff;
        }
        return 0;
    }

    @Override
    public long skip(long n) throws IOException {
        if (available < 0)
            return 0;

        long skip = Math.min(available, n);
        position += skip;
        available -= skip;
        return skip;
    }
}
