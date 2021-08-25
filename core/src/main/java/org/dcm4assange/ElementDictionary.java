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

import org.dcm4assange.util.TagUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
public class ElementDictionary {
    private static final ElementDictionary DICOM = new ElementDictionary(null, Tag::of,
            new Elements(ElementDictionary.class.getResource("ElementDictionary.properties")));

    private static final ServiceLoader<ElementDictionary> loader = ServiceLoader.load(ElementDictionary.class);
    private static final Map<String, ElementDictionary> privateDictionaries = new HashMap<>();
    private static final Object[] GroupLength = { VR.UL, "GroupLength" };
    private static final Object[] PrivateCreatorID = { VR.LO, "PrivateCreatorID" };
    private static final Object[] ZonalMap = { VR.US, "ZonalMap" };
    private static final Object[] SourceImageIDs = { VR.CS, "SourceImageIDs" };
    private static final Object[] Unknown = { VR.UN, "" };

    private final String privateCreator;
    private final ToIntFunction<String> tagOfKeyword;
    private final IntFunction<Object[]> elementOfTag;

    public ElementDictionary(String privateCreator, ToIntFunction<String> tagOfKeyword, URL resource) {
        this(Objects.requireNonNull(privateCreator), tagOfKeyword, new PrivateElements(resource));
    }

    private ElementDictionary(String privateCreator, ToIntFunction<String> tagOfKeyword,
                              IntFunction<Object[]> elementOfTag) {
        this.privateCreator = privateCreator;
        this.tagOfKeyword = tagOfKeyword;
        this.elementOfTag = elementOfTag;
    }

    public final String getPrivateCreator() {
        return privateCreator;
    }

    public static ElementDictionary privateDictionary(String privateCreator) {
        if (privateCreator != null) {
            ElementDictionary dict1 = privateDictionaries.get(Objects.requireNonNull(privateCreator));
            if (dict1 != null) return dict1;
            if (!privateDictionaries.containsKey(privateCreator)) {
                synchronized (loader) {
                    for (ElementDictionary dict : loader) {
                        privateDictionaries.putIfAbsent(dict.getPrivateCreator(), dict);
                        if (privateCreator.equals(dict.getPrivateCreator()))
                            return dict;
                    }
                    privateDictionaries.put(privateCreator, null);
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

    public static VR vrOf(int tag) {
        return (VR) DICOM.elementOfTag.apply(tag)[0];
    }

    public static String keywordOf(int tag) {
        return (String) DICOM.elementOfTag.apply(tag)[1];
    }

    public static int tagForKeyword(String keyword) {
        return DICOM.tagOfKeyword.applyAsInt(keyword);
    }

    public static VR vrOf(String privateCreator, int tag) {
        return (VR) (TagUtils.isPrivateTag(tag) ? privateDictionary(privateCreator) : DICOM)
                .elementOfTag.apply(tag)[0];
    }

    public static String keywordOf(String privateCreator, int tag) {
        return (String) (TagUtils.isPrivateTag(tag) ? privateDictionary(privateCreator) : DICOM)
                .elementOfTag.apply(tag)[1];
    }

    public static int tagForKeyword(String privateCreatorID, String keyword) {
        return privateDictionary(privateCreatorID).tagOfKeyword.applyAsInt(keyword);
    }

    private static void parse(URL resource, BiConsumer<String, Object[]> action) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
            reader.lines()
                    .filter(line -> line.length() >= 12 && line.charAt(0) != '#')
                    .forEach(line -> action.accept(
                        line.substring(0,8), line.charAt(11) == ':'
                                ? new Object[]{VR.valueOf(line.substring(9,11)), line.substring(12)}
                                : new Object[]{null, line.substring(10)}));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Elements implements IntFunction<Object[]>, BiConsumer<String, Object[]> {
        private final Map<Integer, Object[]> map1 = new HashMap<>();
        private final Map<Integer, Object[]> map2 = new HashMap<>();
        private final Map<Integer, Object[]> map3 = new HashMap<>();
        private final Map<Integer, Object[]> map4 = new HashMap<>();

        public Elements(URL resource) {
            parse(resource, this);
        }

        @Override
        public void accept(String tagHexString, Object[] element) {
            Integer tag = (int) Long.parseLong(tagHexString.replace('x', '0'), 16);
            switch (tagHexString.lastIndexOf('x')) {
                case -1 -> map1.put(tag, element);
                case 3 -> map2.put(tag, element);
                case 6 -> (tagHexString.indexOf('x') == 6 ? map3 : map4).put(tag, element);
            }
        }

        @Override
        public Object[] apply(int tag) {
            return (tag & 0x0000FFFF) == 0 && tag != 0x00000000 && tag != 0x00020000 ? GroupLength
                : (tag & 0x00010000) != 0 ?
                    ((tag & 0x0000FF00) == 0 && (tag & 0x000000F0) != 0 ? PrivateCreatorID : Unknown)
                : map1.compute(tag, (tag1, entry1) -> entry1 != null ? entry1
                : map2.compute(tag & 0xFF00FFFF, (tag2, entry2) -> entry2 != null ? entry2
                : map3.compute(tag & 0xFFFFFF0F, (tag3, entry3) -> entry3 != null ? entry3
                : map4.compute(tag & 0xFFFF000F, (tag4, entry4) -> entry4 != null ? entry4
                : (tag & 0xFFFF0000) == 0x10100000 ? ZonalMap
                : (tag & 0xFFFFFF00) == 0x00203100 ? SourceImageIDs
                : Unknown))));
        }
    }

    private static class PrivateElements implements IntFunction<Object[]>, BiConsumer<String, Object[]> {
        private final Map<Integer, Object[]> map = new HashMap<>();

        public PrivateElements(URL resource) {
            parse(resource, this);
        }

        @Override
        public void accept(String tagHexString, Object[] element) {
            Integer tag = (int) Long.parseLong(tagHexString.replace('x', '0'), 16);
            map.put(tag, element);
        }

        @Override
        public Object[] apply(int tag) {
            return map.getOrDefault(tag & 0xFFFF00FF, Unknown);
        }
    }
}
