/*
 * Copyright 2021 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dcm4assange.net;

import org.dcm4assange.DicomObject;

import java.io.InputStream;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2021
 */
public class Association {
    private AAssociate.RQ aarq;
    private AAssociate.AC aaac;

    public String getTransferSyntax(Byte pcid) {
        return aaac.getPresentationContext(pcid).transferSyntax;
    }

    public void onDimseRQ(Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream) {
    }

    public void onCancelRQ(Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream) {
    }

    public void onDimseRSP(Byte pcid, Dimse dimse, DicomObject commandSet, DicomObject dataSet) {
    }
}
