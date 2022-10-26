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

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2021
 */
class Utils {
    static void skipByte(InputStream in) throws IOException {
        if (in.read() < 0)
            throw new EOFException();
    }

    static void skipNBytes(InputStream in, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            if (in.read() < 0)
                throw new EOFException();
        }
    }

    static int readUnsignedByte(InputStream in) throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    static boolean readBoolean(InputStream in) throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);
    }

    static int readUnsignedShort(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 8) + (ch2 << 0);
    }

    static int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    static String readASCII(InputStream in, int length) throws IOException {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) readUnsignedByte(in));
        }
        return sb.toString();
    }

    static void writeBoolean(OutputStream out,  boolean v) throws IOException {
        out.write(v ? 1 : 0);
    }

    static void writeShort(OutputStream out, int v) throws IOException {
        out.write(v >> 8);
        out.write(v);
    }

    static void writeInt(OutputStream out, int v) throws IOException {
        out.write(v >> 24);
        out.write(v >> 16);
        out.write(v >> 8);
        out.write(v);
    }

    static void writeASCII(OutputStream out, String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++)
            out.write((byte)s.charAt(i));
    }

    static void writeLengthASCII(OutputStream out, String s) throws IOException {
        writeShort(out, s.length());
        writeASCII(out, s);
    }

    static void writePaddedASCII(OutputStream out, String s, int padLen) throws IOException {
        writeASCII(out, s);
        writeNBytes(out, 0x20, padLen - s.length());
    }

    static void writeNBytes(OutputStream out, int b, int len) throws IOException {
        for (int i = 0 ; i < len ; i++)
            out.write(b);
    }


    static String checkLength(String s, int length) {
        if (s.length() > length)
            throw new IllegalArgumentException(s + " exceeds " + length + " characters");
        return s;
    }
}
