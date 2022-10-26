package org.dcm4assange.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2019
 */
public class AAssociateRJ extends IOException {
    private static final String[] RESULTS = {
            "0",
            "1 - rejected-permanent",
            "2 - rejected-transient"
    };
    private static final String[] SOURCES = {
            "0",
            "1 - DICOM UL service-user",
            "2 - DICOM UL service-provider (ACSE related function)",
            "3 - DICOM UL service-provider (Presentation related function)"
    };
    private static final String[] USER_REASONS = {
            "0",
            "1 - no-reason-given",
            "2 - application-context-name-not-supported",
            "3 - calling-AE-title-not-recognized",
            "4",
            "5",
            "6",
            "7 - called-AE-title-not-recognized"
    };
    private static final String[] ACSE_REASONS = {
            "0",
            "1 - no-reason-given",
            "2 - protocol-version-not-supported"
    };
    private static final String[] PRES_REASONS = {
            "0",
            "1 - temporary-congestion",
            "2 - local-limit-exceeded"
    };
    private static final String[][] REASONS = {
            USER_REASONS,
            ACSE_REASONS,
            PRES_REASONS
    };
    public final int resultSourceReason;

    private AAssociateRJ(int resultSourceReason) {
        super(toString(resultSourceReason));
        this.resultSourceReason = resultSourceReason;
    }

    public static AAssociateRJ protocolVersionNotSupported() {
        return new AAssociateRJ(0x010202);
    }
    public static AAssociateRJ applicationContextNameNotSupported() {
        return new AAssociateRJ(0x010102);
    }

    public static AAssociateRJ callingAETitleNotRecognized() {
        return new AAssociateRJ(0x010103);
    }

    public static AAssociateRJ calledAETitleNotRecognized() {
        return new AAssociateRJ(0x010107);
    }

    public static AAssociateRJ readFrom(InputStream is, int pduLength) throws IOException {
        if (pduLength != 4)
            throw AAbort.invalidPDUParameterValue();
        return new AAssociateRJ(Utils.readInt(is));
    }

    public void writeTo(OutputStream out) throws IOException {
        Utils.writeInt(out, resultSourceReason);
    }

    static String toString(int resultSourceReason) {
        final int source = (resultSourceReason >> 8) & 0xff;
        return "A-ASSOCIATE-RJ[" + System.lineSeparator()
                + "  result: " + itoa((resultSourceReason >> 16) & 0xff, RESULTS)
                + System.lineSeparator()
                + "  source: " + itoa(source, SOURCES)
                + System.lineSeparator()
                + "  reason: " + reasonAsString(source, resultSourceReason & 0xff)
                + System.lineSeparator()
                + ']';
    }

    private static String reasonAsString(int source, int reason) {
        try {
            return REASONS[source - 1][reason];
        } catch (IndexOutOfBoundsException e) {
            return Integer.toString(reason);
        }
    }

    private static String itoa(int i, String[] values) {
        try {
            return values[i];
        } catch (IndexOutOfBoundsException e) {
            return Integer.toString(i);
        }
    }

}
