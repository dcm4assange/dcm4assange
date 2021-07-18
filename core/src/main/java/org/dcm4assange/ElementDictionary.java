/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dcm4assange;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
public class ElementDictionary {
    private static final ElementDictionary DICOM = new ElementDictionary(
            null, Tag::of, Tag::keywordOf, Tag::vrOf);
    private static final ServiceLoader<ElementDictionary> loader =
            ServiceLoader.load(ElementDictionary.class);
    private static final Map<String, ElementDictionary> map = new HashMap<>();
    private final String privateCreator;
    private final ToIntFunction<String> tagOfKeyword;
    private final IntFunction<String> keywordOfTag;
    private final IntFunction<VR> vrOfTag;

    public ElementDictionary(String privateCreator,
                             ToIntFunction<String> tagOfKeyword,
                             IntFunction<String> keywordOfTag,
                             IntFunction<VR> vrOfTag) {
        this.privateCreator = privateCreator;
        this.tagOfKeyword = tagOfKeyword;
        this.keywordOfTag = keywordOfTag;
        this.vrOfTag = vrOfTag;
    }

    public final String getPrivateCreator() {
        return privateCreator;
    }

    public static ElementDictionary getElementDictionary() {
        return ElementDictionary.DICOM;
    }

    public static ElementDictionary getElementDictionary(String privateCreator) {
        if (privateCreator != null) {
            ElementDictionary dict1 = map.get(Objects.requireNonNull(privateCreator));
            if (dict1 != null) return dict1;
            if (!map.containsKey(privateCreator)) {
                synchronized (loader) {
                    for (ElementDictionary dict : loader) {
                        map.putIfAbsent(dict.getPrivateCreator(), dict);
                        if (privateCreator.equals(dict.getPrivateCreator()))
                            return dict;
                    }
                    map.put(privateCreator, null);
                }
            }
        }
        return ElementDictionary.DICOM;
    }

    public static void reload() {
        synchronized (loader) {
            loader.reload();
        }
    }

    public static VR vrOf(int tag, String privateCreator) {
        return getElementDictionary(privateCreator).vrOfTag.apply(tag);
    }

    public static String keywordOf(int tag, String privateCreator) {
        return getElementDictionary(privateCreator).keywordOfTag.apply(tag);
    }

    public static int tagForKeyword(String keyword, String privateCreatorID) {
        return getElementDictionary(privateCreatorID).tagOfKeyword.applyAsInt(keyword);
    }

}
