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

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2021
 */
class Utils {
    static void skipByte(InputStream in) throws IOException {
        if (in.read() < 0)
            throw new EOFException();
    }

    static String readASCII(DataInputStream in, int length) throws IOException {
        byte[] b = new byte[length];
        in.readFully(b, 0, length);
        return new String(b, 0, length, StandardCharsets.US_ASCII);
    }

    static void writeASCII(DataOutputStream out, String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++)
            out.write((byte)s.charAt(i));
    }

    static void writeLengthASCII(DataOutputStream out, String s) throws IOException {
        out.writeShort(s.length());
        writeASCII(out, s);
    }

    static void writePaddedASCII(DataOutputStream out, String s, int padLen) throws IOException {
        writeASCII(out, s);
        writeNBytes(out, 0x20, padLen - s.length());
    }

    static void writeNBytes(DataOutputStream out, int b, int len) throws IOException {
        for (int i = 0 ; i < len ; i++)
            out.write(b);
    }


    static String checkLength(String s, int length) {
        if (s.length() > length)
            throw new IllegalArgumentException(s + " exceeds " + length + " characters");
        return s;
    }
}
