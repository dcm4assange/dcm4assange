package org.dcm4assange.net;

import org.dcm4assange.DicomObject;
import org.dcm4assange.UID;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Nov 2019
 */
public class DicomServiceRegistry implements DimseHandler {
    private final Map<String, DimseHandler> map = new HashMap<>();
    private volatile DimseHandler defaultRQHandler = (as, pcid, dimse, commandSet, dataStream) -> {
        throw new DicomServiceException(dimse.noSuchSOPClass);
    };

    public DicomServiceRegistry() {
        map.put(UID.Verification, new AbstractDimseHandler(dimse -> dimse == Dimse.C_ECHO_RQ) {
            @Override
            protected void accept(Association as, Byte pcid, Dimse dimse, DicomObject commandSet, DicomObject dataSet)
                    throws IOException {
                as.writeDimse(pcid, Dimse.C_ECHO_RSP, dimse.mkRSP(commandSet));
            }
        });
    }

    public DicomServiceRegistry setDefaultRQHandler(DimseHandler defaultRQHandler) {
        this.defaultRQHandler = Objects.requireNonNull(defaultRQHandler);
        return this;
    }

    @Override
    public void accept(Association as, Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream)
            throws IOException {
        handlerOf(as, commandSet.getStringOrElseThrow(dimse.tagOfSOPClassUID))
                .accept(as, pcid, dimse, commandSet, dataStream);
    }

    private DimseHandler handlerOf(Association as, String cuid) {
        DimseHandler handler = map.get(cuid);
        if (handler != null) {
            return handler;
        }
        AAssociate.CommonExtendedNegotation commonExtendedNegotation = as.commonExtendedNegotationFor(cuid);
        if (commonExtendedNegotation != null) {
             handler = commonExtendedNegotation.selectDimseHandler(map);
            if (handler != null) {
                return handler;
            }
        }
        return defaultRQHandler;
    }
}
