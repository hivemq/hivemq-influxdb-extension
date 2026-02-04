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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfluxDbConfigurationTest {

    private @NotNull InfluxDbConfiguration influxDbConfiguration;
    private @NotNull Path file;

    @TempDir
    private @NotNull Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        final var confDir = tempDir.resolve("conf");
        Files.createDirectories(confDir);
        file = confDir.resolve("config.properties");
        influxDbConfiguration = new InfluxDbConfiguration(file.toFile());
    }

    @Test
    void validateConfiguration_ok() throws Exception {
        Files.write(file, List.of("host:localhost", "port:3000"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isTrue();
    }

    @Test
    void validateConfiguration_wrong_port() throws Exception {
        Files.write(file, List.of("host:localhost", "port:-3000"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();
    }

    @Test
    void validateConfiguration_host_missing() throws Exception {
        Files.write(file, List.of("port:3000"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();
    }

    @Test
    void validateConfiguration_port_missing() throws Exception {
        Files.write(file, List.of("host:localhost"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();
    }

    @Test
    void validateConfiguration_port_null() throws Exception {
        Files.write(file, List.of("host:localhost", "port:"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();

        assertThat(influxDbConfiguration.getProperty("port")).isNull();
    }

    @Test
    void validateConfiguration_host_null() throws Exception {
        Files.write(file, List.of("host:", "port:3000"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();

        assertThat(influxDbConfiguration.getProperty("host")).isNull();
    }

    @Test
    void all_properties_empty() throws Exception {
        Files.write(file,
                List.of("mode:",
                        "host:",
                        "port:",
                        "tags:",
                        "prefix:",
                        "protocol:",
                        "database:",
                        "connectTimeout:",
                        "reportingInterval:",
                        "auth:"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();

        assertThat(influxDbConfiguration.getMode()).isEqualTo("http");
        assertThat(influxDbConfiguration.getTags()).isEmpty();
        assertThat(influxDbConfiguration.getPrefix()).isEmpty();
        assertThat(influxDbConfiguration.getProtocolOrDefault("http")).isEqualTo("http");
        assertThat(influxDbConfiguration.getDatabase()).isEqualTo("hivemq");
        assertThat(influxDbConfiguration.getConnectTimeout()).isEqualTo(5000);
        assertThat(influxDbConfiguration.getReportingInterval()).isEqualTo(1);
        assertThat(influxDbConfiguration.getAuth()).isNull();
        assertThat(influxDbConfiguration.getHost()).isNull();
        assertThat(influxDbConfiguration.getPort()).isNull();
    }

    @Test
    void all_properties_null() throws Exception {
        Files.write(file, List.of());
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();

        assertThat(influxDbConfiguration.getMode()).isEqualTo("http");
        assertThat(influxDbConfiguration.getTags()).isEmpty();
        assertThat(influxDbConfiguration.getPrefix()).isEmpty();
        assertThat(influxDbConfiguration.getProtocolOrDefault("http")).isEqualTo("http");
        assertThat(influxDbConfiguration.getDatabase()).isEqualTo("hivemq");
        assertThat(influxDbConfiguration.getConnectTimeout()).isEqualTo(5000);
        assertThat(influxDbConfiguration.getReportingInterval()).isEqualTo(1);
        assertThat(influxDbConfiguration.getAuth()).isNull();
        assertThat(influxDbConfiguration.getHost()).isNull();
        assertThat(influxDbConfiguration.getPort()).isNull();
    }

    @Test
    void all_properties_have_correct_values() throws Exception {
        Files.write(file,
                List.of("mode:tcp",
                        "host:hivemq.monitoring.com",
                        "port:3000",
                        "tags:host=hivemq1;version=3.4.1",
                        "prefix:node1",
                        "protocol:tcp",
                        "database:test-hivemq",
                        "connectTimeout:10000",
                        "reportingInterval:5",
                        "auth:username:password"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isTrue();

        assertThat(influxDbConfiguration.getMode()).isEqualTo("tcp");
        assertThat(influxDbConfiguration.getTags()).containsOnly(entry("host", "hivemq1"), entry("version", "3.4.1"));
        assertThat(influxDbConfiguration.getPrefix()).isEqualTo("node1");
        assertThat(influxDbConfiguration.getProtocolOrDefault("http")).isEqualTo("tcp");
        assertThat(influxDbConfiguration.getDatabase()).isEqualTo("test-hivemq");
        assertThat(influxDbConfiguration.getConnectTimeout()).isEqualTo(10000);
        assertThat(influxDbConfiguration.getReportingInterval()).isEqualTo(5);
        assertThat(influxDbConfiguration.getAuth()).isEqualTo("username:password");
        assertThat(influxDbConfiguration.getHost()).isEqualTo("hivemq.monitoring.com");
        assertThat(influxDbConfiguration.getPort()).isEqualTo(3000);
    }

    @Test
    void tags_invalid_configured() throws Exception {
        Files.write(file, List.of("tags:host=hivemq1;version=;use=monitoring"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();

        assertThat(influxDbConfiguration.getTags()).containsOnly(entry("host", "hivemq1"), entry("use", "monitoring"));
    }

    @Test
    void tags_has_only_semicolons() throws Exception {
        Files.write(file, List.of("tags:;;;;;;"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();

        assertThat(influxDbConfiguration.getTags()).isEmpty();
    }

    @Test
    void tags_has_only_a_key() throws Exception {
        Files.write(file, List.of("tags:okay"));
        assertThat(influxDbConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(influxDbConfiguration.validateConfiguration()).isFalse();

        assertThat(influxDbConfiguration.getTags()).isEmpty();
    }

    @Test
    void tags_has_correct_tag_but_missing_semicolon() throws Exception {
        Files.write(file, List.of("tags:key=value"));
        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());

        final Map<String, String> tags = influxDbConfiguration.getTags();
        assertEquals(1, tags.size());
    }

    @Test
    void properties_that_are_numbers_have_invalid_string() throws Exception {
        Files.write(file, List.of("host:test", "port:800000", "reportingInterval:0", "connectTimeout:-1"));
        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        // false because port is out of range
        assertFalse(influxDbConfiguration.validateConfiguration());

        // default values because values in file are no valid (zero or negative number)
        assertThat(influxDbConfiguration.getConnectTimeout()).isEqualTo(5000);
        assertThat(influxDbConfiguration.getReportingInterval()).isEqualTo(1);
    }

    @Test
    void validateConfiguration_cloud_token_missing() throws Exception {
        Files.write(file, List.of("mode:cloud", "host:localhost", "bucket:mybucket"));
        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_cloud_bucket_missing() throws Exception {
        Files.write(file, List.of("mode:cloud", "host:localhost", "token:mytoken"));
        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_cloud_ok() throws Exception {
        Files.write(file, List.of("mode:cloud", "host:localhost", "token:mytoken", "bucket:mybucket"));
        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());
    }
}
