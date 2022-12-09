package org.dcm4assange.net;

import org.dcm4assange.DicomObject;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Nov 2019
 */
public class DicomServiceException extends Exception {
    private final int status;
    private final DicomObject dataSet;

    public DicomServiceException(int status) {
        this(status, null);
    }
    public DicomServiceException(int status, DicomObject dataSet) {
        this.status = status;
        this.dataSet = dataSet;
    }

    public DicomObject getDataSet() {
        return dataSet;
    }

    public int getStatus() {
        return status;
    }
}
