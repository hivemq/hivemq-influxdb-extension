package com.hivemq.extensions.influxdb.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InfluxDbConfigurationTest {

    private @NotNull InfluxDbConfiguration influxDbConfiguration;
    private @NotNull Path file;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        influxDbConfiguration = new InfluxDbConfiguration(tempDir.toFile());
        file = tempDir.resolve("influxdb.properties");
    }

    @Test
    void validateConfiguration_ok() throws IOException {
        Files.write(file, List.of("host:localhost", "port:3000"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertTrue(influxDbConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_wrong_port() throws IOException {
        Files.write(file, List.of("host:localhost", "port:-3000"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_host_missing() throws IOException {
        Files.write(file, List.of("port:3000"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_port_missing() throws IOException {
        Files.write(file, List.of("host:localhost"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_port_null() throws IOException {
        Files.write(file, List.of("host:localhost", "port:"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());

        assertNull(influxDbConfiguration.getProperty("port"));
    }

    @Test
    void validateConfiguration_host_null() throws IOException {
        Files.write(file, List.of("host:", "port:3000"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());

        assertNull(influxDbConfiguration.getProperty("host"));
    }

    @Test
    void all_properties_empty() throws IOException {
        Files.write(file, List.of(
                "mode:",
                "host:",
                "port:",
                "tags:",
                "prefix:",
                "protocol:",
                "database:",
                "connectTimeout:",
                "reportingInterval:",
                "auth:"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());

        assertEquals("http", influxDbConfiguration.getMode());
        assertTrue(influxDbConfiguration.getTags().isEmpty());
        assertEquals("", influxDbConfiguration.getPrefix());
        assertEquals("http", influxDbConfiguration.getProtocol());
        assertEquals("hivemq", influxDbConfiguration.getDatabase());
        assertEquals(5000, influxDbConfiguration.getConnectTimeout());
        assertEquals(1, influxDbConfiguration.getReportingInterval());
        assertNull(influxDbConfiguration.getAuth());
        assertNull(influxDbConfiguration.getHost());
        assertNull(influxDbConfiguration.getPort());
    }

    @Test
    void all_properties_null() throws IOException {
        Files.write(file, List.of());

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());

        assertEquals("http", influxDbConfiguration.getMode());
        assertTrue(influxDbConfiguration.getTags().isEmpty());
        assertEquals("", influxDbConfiguration.getPrefix());
        assertEquals("http", influxDbConfiguration.getProtocol());
        assertEquals("hivemq", influxDbConfiguration.getDatabase());
        assertEquals(5000, influxDbConfiguration.getConnectTimeout());
        assertEquals(1, influxDbConfiguration.getReportingInterval());
        assertNull(influxDbConfiguration.getAuth());
        assertNull(influxDbConfiguration.getHost());
        assertNull(influxDbConfiguration.getPort());
    }

    @Test
    void all_properties_have_correct_values() throws IOException {
        Files.write(file, List.of(
                "mode:tcp",
                "host:hivemq.monitoring.com",
                "port:3000",
                "tags:host=hivemq1;version=3.4.1",
                "prefix:node1",
                "protocol:tcp",
                "database:test-hivemq",
                "connectTimeout:10000",
                "reportingInterval:5",
                "auth:username:password"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertTrue(influxDbConfiguration.validateConfiguration());

        final Map<String, String> tags = influxDbConfiguration.getTags();
        assertEquals("tcp", influxDbConfiguration.getMode());
        assertEquals(2, tags.size());
        assertEquals("hivemq1", tags.get("host"));
        assertEquals("3.4.1", tags.get("version"));
        assertEquals("node1", influxDbConfiguration.getPrefix());
        assertEquals("tcp", influxDbConfiguration.getProtocol());
        assertEquals("test-hivemq", influxDbConfiguration.getDatabase());
        assertEquals(10000, influxDbConfiguration.getConnectTimeout());
        assertEquals(5, influxDbConfiguration.getReportingInterval());
        assertEquals("username:password", influxDbConfiguration.getAuth());
        assertEquals("hivemq.monitoring.com", influxDbConfiguration.getHost());
        assertEquals(3000, influxDbConfiguration.getPort());
    }

    @Test
    void tags_invalid_configured() throws IOException {
        Files.write(file, List.of("tags:host=hivemq1;version=;use=monitoring"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());

        final Map<String, String> tags = influxDbConfiguration.getTags();
        assertEquals(2, tags.size());
        assertEquals("hivemq1", tags.get("host"));
        assertEquals("monitoring", tags.get("use"));
        assertNull(tags.get("version"));
    }

    @Test
    void tags_has_only_semicolons() throws IOException {
        Files.write(file, List.of("tags:;;;;;;"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());

        final Map<String, String> tags = influxDbConfiguration.getTags();
        assertEquals(0, tags.size());
    }

    @Test
    void tags_has_only_a_key() throws IOException {
        Files.write(file, List.of("tags:okay"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());

        final Map<String, String> tags = influxDbConfiguration.getTags();
        assertEquals(0, tags.size());
    }

    @Test
    void tags_has_correct_tag_but_missing_semicolon() throws IOException {
        Files.write(file, List.of("tags:key=value"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());

        final Map<String, String> tags = influxDbConfiguration.getTags();
        assertEquals(1, tags.size());
    }

    @Test
    void properties_that_are_numbers_have_invalid_string() throws IOException {
        Files.write(file, List.of("host:test", "port:800000", "reportingInterval:0", "connectTimeout:-1"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        //false because port is out of range
        assertFalse(influxDbConfiguration.validateConfiguration());

        //default values because values in file are no valid (zero or negative number)
        assertEquals(5000, influxDbConfiguration.getConnectTimeout());
        assertEquals(1, influxDbConfiguration.getReportingInterval());
    }

    @Test
    void validateConfiguration_cloud_token_missing() throws IOException {
        Files.write(file, List.of("mode:cloud", "host:localhost", "bucket:mybucket"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_cloud_bucket_missing() throws IOException {
        Files.write(file, List.of("mode:cloud", "host:localhost", "token:mytoken"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_cloud_ok() throws IOException {
        Files.write(file, List.of("mode:cloud", "host:localhost", "token:mytoken", "bucket:mybucket"));

        assertTrue(influxDbConfiguration.readPropertiesFromFile());
        assertFalse(influxDbConfiguration.validateConfiguration());
    }
}