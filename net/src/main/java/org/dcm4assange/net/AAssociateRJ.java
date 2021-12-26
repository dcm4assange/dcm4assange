package org.dcm4assange.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2019
 */
public class AAssociateRJ extends Exception {
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

    public static AAssociateRJ readFrom(DataInputStream dis, int pduLength) throws IOException {
        return new AAssociateRJ(dis.readInt());
    }

    public void writeTo(DataOutputStream dos) throws IOException {
        dos.writeInt(resultSourceReason);
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
