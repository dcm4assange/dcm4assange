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
public class Sequence {
    private static final DicomObject2[] EMPTY_SEQUENCE = {};
    final DicomObject2 dcmobj;
    final int tag;
    private DicomObject2[] items = EMPTY_SEQUENCE;
    private int size;

    public Sequence(DicomObject2 dcmobj, int tag) {
        this.dcmobj = Objects.requireNonNull(dcmobj);
        this.tag = tag;
    }

    public void add(DicomObject2 dcmObj) {
        int index = size++;
        ensureCapacity(index);
        items[index] = dcmObj;
    }

    private void ensureCapacity(int index) {
        int oldCapacity = items.length;
        if (index < oldCapacity) return;
        if (oldCapacity == 0) {
            items = new DicomObject2[1];
        } else {
            items = Arrays.copyOf(items, oldCapacity == 1 ? 16 : oldCapacity << 1);
        }
    }

    public int size() {
        return size;
    }
}
