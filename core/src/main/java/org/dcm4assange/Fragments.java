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

package org.dcm4assange;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Sep 2021
 */
public class Fragments {
    private static final long[] EMPTY = {};
    final DicomObject2 dcmobj;
    final int tag;
    private long[] headers = EMPTY;
    private int size;

    public Fragments(DicomObject2 dcmobj, int tag) {
        this.dcmobj = Objects.requireNonNull(dcmobj);
        this.tag = tag;
    }

    Fragments(DicomObject2 dcmobj, Fragments o) {
        this(dcmobj, o.tag);
        this.headers = Arrays.copyOf(o.headers, o.size);
        this.size = o.size;
    }

    public void add(long header) {
        int index = size++;
        ensureCapacity(index);
        headers[index] = header;
    }

    private void ensureCapacity(int index) {
        int oldCapacity = headers.length;
        if (index < oldCapacity) return;
        if (oldCapacity == 0) {
            headers = new long[2];
        } else {
            headers = Arrays.copyOf(headers, oldCapacity == 2 ? 16 : oldCapacity << 1);
        }
    }

    public int size() {
        return size;
    }

    public DicomObject2 getDicomObject() {
        return dcmobj;
    }
}
