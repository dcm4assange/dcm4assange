package org.dcm4assange.net;

import org.dcm4assange.DicomObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Nov 2019
 */
@FunctionalInterface
public interface DimseHandler {
    void accept(Association as, Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream)
            throws IOException;

}
