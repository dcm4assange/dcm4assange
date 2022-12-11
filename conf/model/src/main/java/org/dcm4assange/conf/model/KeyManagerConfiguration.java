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

import java.util.Objects;
import java.util.Optional;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jun 2019
 */
public class KeyManagerConfiguration {
    private String algorithm;
    private String provider;
    private KeyStoreConfiguration keyStoreConfiguration;
    private String password = "changeit";

    public Optional<String> getAlgorithm() {
        return Optional.ofNullable(algorithm);
    }

    public KeyManagerConfiguration setAlgorithm(String algorithm) {
        this.algorithm = StringUtils.trimAndNullifyEmpty(algorithm);
        return this;
    }

    public Optional<String> getProvider() {
        return Optional.ofNullable(provider);
    }

    public KeyManagerConfiguration setProvider(String provider) {
        this.provider = StringUtils.trimAndNullifyEmpty(provider);
        return this;
    }

    public KeyStoreConfiguration getKeyStoreConfiguration() {
        return Objects.requireNonNull(keyStoreConfiguration);
    }

    public KeyManagerConfiguration setKeyStoreConfiguration(KeyStoreConfiguration keyStoreConfiguration) {
        this.keyStoreConfiguration = Objects.requireNonNull(keyStoreConfiguration);
        return this;
    }

    public String getPassword() {
        return password;
    }

    public KeyManagerConfiguration setPassword(String password) {
        this.password = StringUtils.requireNonBlank(password);
        return this;
    }
}
