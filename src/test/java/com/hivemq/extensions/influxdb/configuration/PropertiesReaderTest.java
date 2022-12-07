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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesReaderTest {

    @Test
    void readPropertiesFromFile_file_null() {
        assertThrows(NullPointerException.class, () -> new PropertiesReader(null) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        });
    }

    @Test
    void readPropertiesFromFile_file_does_not_exist(final @TempDir @NotNull Path tempDir) {
        final PropertiesReader propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertFalse(propertiesReader.readPropertiesFromFile());
    }

    @Test
    void readPropertiesFromFile_file_does_exist(final @TempDir @NotNull Path tempDir) throws IOException {
        assertTrue(tempDir.resolve("test").toFile().createNewFile());

        final PropertiesReader propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertTrue(propertiesReader.readPropertiesFromFile());
    }

    @Test
    void getProperty_key_null(final @TempDir @NotNull Path tempDir) throws IOException {
        final Path file = tempDir.resolve("test");
        assertTrue(file.toFile().createNewFile());
        Files.write(file, List.of("key:value"));

        final PropertiesReader propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertTrue(propertiesReader.readPropertiesFromFile());
        assertEquals("value", propertiesReader.getProperty("key"));

        assertThrows(NullPointerException.class, () -> propertiesReader.getProperty(null));
    }

    @Test
    void getProperty_key_doesnt_exist(final @TempDir @NotNull Path tempDir) throws IOException {
        final Path file = tempDir.resolve("test");
        assertTrue(file.toFile().createNewFile());
        Files.write(file, List.of("key:value"));

        final PropertiesReader propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertTrue(propertiesReader.readPropertiesFromFile());
        assertEquals("value", propertiesReader.getProperty("key"));

        assertNull(propertiesReader.getProperty("unknown"));
    }

    @Test
    void getProperty_key_exists(final @TempDir @NotNull Path tempDir) throws IOException {
        final Path file = tempDir.resolve("test");
        assertTrue(file.toFile().createNewFile());
        Files.write(file, List.of("key:value"));

        final PropertiesReader propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertTrue(propertiesReader.readPropertiesFromFile());
        assertEquals("value", propertiesReader.getProperty("key"));
    }

    @Test
    void getProperty_before_loading_properties(final @TempDir @NotNull Path tempDir) throws IOException {
        final Path file = tempDir.resolve("test");
        assertTrue(file.toFile().createNewFile());
        Files.write(file, List.of("key:value"));

        final PropertiesReader propertiesReader = new PropertiesReader(tempDir.toFile()) {
            @Override
            public @NotNull String getFilename() {
                return "test";
            }
        };
        assertNull(propertiesReader.getProperty("key"));
    }
}