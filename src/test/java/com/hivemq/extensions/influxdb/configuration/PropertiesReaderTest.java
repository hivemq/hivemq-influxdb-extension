/*
 * Copyright 2018-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.influxdb.configuration;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertiesReaderTest {

    @Test
    void readPropertiesFromFile_file_null() {
        //noinspection DataFlowIssue
        assertThatThrownBy(() -> new PropertiesReader(null) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    void readPropertiesFromFile_file_does_not_exist(final @TempDir @NotNull Path tempDir) {
        final var propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertThat(propertiesReader.readPropertiesFromFile()).isFalse();
    }

    @Test
    void readPropertiesFromFile_file_does_exist(final @TempDir @NotNull Path tempDir) throws IOException {
        assertThat(tempDir.resolve("test").toFile().createNewFile()).isTrue();

        final var propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertThat(propertiesReader.readPropertiesFromFile()).isTrue();
    }

    @Test
    void getProperty_key_null(final @TempDir @NotNull Path tempDir) throws IOException {
        final var file = tempDir.resolve("test");
        assertThat(file.toFile().createNewFile()).isTrue();
        Files.write(file, List.of("key:value"));

        final var propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertThat(propertiesReader.readPropertiesFromFile()).isTrue();
        assertThat(propertiesReader.getProperty("key")).isEqualTo("value");

        //noinspection DataFlowIssue
        assertThatThrownBy(() -> propertiesReader.getProperty(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getProperty_key_doesnt_exist(final @TempDir @NotNull Path tempDir) throws IOException {
        final var file = tempDir.resolve("test");
        assertThat(file.toFile().createNewFile()).isTrue();
        Files.write(file, List.of("key:value"));

        final var propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertThat(propertiesReader.readPropertiesFromFile()).isTrue();
        assertThat(propertiesReader.getProperty("key")).isEqualTo("value");
        assertThat(propertiesReader.getProperty("unknown")).isNull();
    }

    @Test
    void getProperty_key_exists(final @TempDir @NotNull Path tempDir) throws IOException {
        final var file = tempDir.resolve("test");
        assertThat(file.toFile().createNewFile()).isTrue();
        Files.write(file, List.of("key:value"));

        final var propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertThat(propertiesReader.readPropertiesFromFile()).isTrue();
        assertThat(propertiesReader.getProperty("key")).isEqualTo("value");
    }

    @Test
    void getProperty_before_loading_properties(final @TempDir @NotNull Path tempDir) throws IOException {
        final var file = tempDir.resolve("test");
        assertThat(file.toFile().createNewFile()).isTrue();
        Files.write(file, List.of("key:value"));

        final var propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertThat(propertiesReader.getProperty("key")).isNull();
    }
}
