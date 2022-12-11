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

package org.dcm4assange.conf.model;

import org.dcm4assange.util.StringUtils;

import java.util.Optional;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jun 2019
 */
public class KeyStoreConfiguration {
    private String name = "default";
    private String keyStoreType;
    private String provider;
    private String path;
    private String url;
    private String password = "changeit";

    public String getName() {
        return name;
    }

    public KeyStoreConfiguration setName(String name) {
        this.name = StringUtils.requireNonBlank(name);
        return this;
    }

    public Optional<String> getKeyStoreType() {
        return Optional.ofNullable(keyStoreType);
    }

    public KeyStoreConfiguration setKeyStoreType(String keyStoreType) {
        this.keyStoreType = StringUtils.trimAndNullifyEmpty(keyStoreType);
        return this;
    }

    public Optional<String> getProvider() {
        return Optional.ofNullable(provider);
    }

    public KeyStoreConfiguration setProvider(String provider) {
        this.provider = StringUtils.trimAndNullifyEmpty(provider);
        return this;
    }

    public Optional<String> getPath() {
        return Optional.ofNullable(path);
    }

    public KeyStoreConfiguration setPath(String path) {
        this.path = StringUtils.trimAndNullifyEmpty(path);
        return this;
    }

    public Optional<String> getURL() {
        return Optional.ofNullable(url);
    }

    public KeyStoreConfiguration setURL(String url) {
        this.url = StringUtils.trimAndNullifyEmpty(url);
        return this;
    }

    public String getPassword() {
        return password;
    }

    public KeyStoreConfiguration setPassword(String password) {
        this.password = StringUtils.requireNonBlank(password);
        return this;
    }
}
