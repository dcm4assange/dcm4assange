package org.dcm4assange.net;

import org.dcm4assange.DicomEncoding;
import org.dcm4assange.DicomInputStream;
import org.dcm4assange.DicomObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Nov 2019
 */
public abstract class AbstractDimseHandler implements DimseHandler {
    final Predicate<Dimse> recognizedOperation;

    public AbstractDimseHandler(Predicate<Dimse> recognizedOperation) {
        this.recognizedOperation = recognizedOperation;
    }

    @Override
    public void accept(Association as, Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream)
            throws IOException {
        if (!recognizedOperation.test(dimse)) {
            throw new DicomServiceException(Status.UnrecognizedOperation);
        }
        accept(as, pcid, dimse, commandSet, dataStream != null
                ? new DicomInputStream(dataStream)
                    .withEncoding(DicomEncoding.of(as.getTransferSyntax(pcid)))
                    .readDataSet()
                : null);
    }

    protected abstract void accept(Association as, Byte pcid, Dimse dimse, DicomObject commandSet, DicomObject dataSet)
        throws IOException;

    static final DimseHandler onDimseRSP = new AbstractDimseHandler(dimse -> true) {
        @Override
        protected void accept(Association as, Byte pcid, Dimse dimse, DicomObject commandSet, DicomObject dataSet) {
            as.onDimseRSP(pcid, dimse, commandSet, dataSet);
        }
    };
}
