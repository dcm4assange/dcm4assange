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

import org.dcm4assange.util.StringUtils;

import java.io.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2021
 */
public class AAbort extends IOException {
    private static final String[] SOURCES = {
            "0 - service-user",
            "1",
            "2 - service-provider",
    };

    private static final String[] SERVICE_USER_REASONS = {
            "0",
    };

    private static final String[] SERVICE_PROVIDER_REASONS = {
            "0 - reason-not-specified",
            "1 - unrecognized-PDU",
            "2 - unexpected-PDU",
            "3",
            "4 - unrecognized-PDU-parameter",
            "5 - unexpected-PDU-parameter",
            "6 - invalid-PDU-parameter-value"
    };

    private static final String[][] REASONS = {
            SERVICE_USER_REASONS,
            StringUtils.EMPTY_STRINGS,
            SERVICE_PROVIDER_REASONS
    };

    private final int sourceReason;

    private AAbort(int sourceReason) {
        super(toString(sourceReason));
        this.sourceReason = sourceReason;
    }

    public static AAbort userInitiated() {
        return new AAbort(0);
    }

    public static AAbort reasonNotSpecified() {
        return new AAbort(0x200);
    }

    public static AAbort unrecognizedPDU() {
        return new AAbort(0x201);
    }

    public static AAbort unexpectedPDU() {
        return new AAbort(0x202);
    }

    public static AAbort unrecognizedPDUParameter() {
        return new AAbort(0x204);
    }

    public static AAbort unexpectedPDUParameter() {
        return new AAbort(0x205);
    }

    public static AAbort invalidPDUParameterValue() {
        return new AAbort(0x206);
    }

    public static AAbort unexpectedOrUnrecognizedItem(int itemType) {
        return switch (itemType) {
            case 0x10, 0x20, 0x21, 0x30, 0x40, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59
                    -> unexpectedPDUParameter();
            default -> unrecognizedPDUParameter();
        };
    }

    public static AAbort readFrom(InputStream in, int pduLength) throws IOException {
        if (pduLength != 4)
            throw invalidPDUParameterValue();
        return new AAbort(Utils.readInt(in));
    }

    public void writeTo(OutputStream out) throws IOException {
        Utils.writeInt(out, sourceReason);
    }

    private static String toString(int sourceReason) {
        int source = (sourceReason >> 8) & 0xff;
        return "A-ABORT[source: " + toString(SOURCES, source)
                + ", reason: " + toReason(source, sourceReason & 0xff)
                + ']';
    }

    private static String toString(String[] ss, int i) {
        try {
            return ss[i];
        } catch (IndexOutOfBoundsException e) {
            return Integer.toString(i);
        }
    }

    private static String toReason(int source, int reason) {
        try {
            return toString(REASONS[source], reason);
        } catch (IndexOutOfBoundsException e) {
            return Integer.toString(reason);
        }
    }

    public final int getReason() {
        return sourceReason & 0xff;
    }

    public final int getSource() {
        return (sourceReason >> 8) & 0xff;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
