package org.dcm4assange;

import java.io.IOException;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Mar 2021
 */
public class DicomParseException extends IOException {
    public DicomParseException(String message) {
        super(message);
    }
}
