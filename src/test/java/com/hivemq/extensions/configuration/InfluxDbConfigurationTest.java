package com.hivemq.extensions.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class InfluxDbConfigurationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File root;

    private File file;

    @Before
    public void set_up() throws IOException {
        root = folder.getRoot();
        String fileName = "influxdb.properties";
        file = folder.newFile(fileName);
    }

    @Test
    public void validateConfiguration_ok() throws IOException {

        final List<String> lines = Arrays.asList("host:localhost", "port:3000");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertTrue(validateConfiguration);
    }

    @Test
    public void validateConfiguration_wrong_port() throws IOException {

        final List<String> lines = Arrays.asList("host:localhost", "port:-3000");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_host_missing() throws IOException {

        final List<String> lines = Collections.singletonList("port:3000");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_port_missing() throws IOException {

        final List<String> lines = Collections.singletonList("host:localhost");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_port_null() throws IOException {

        final List<String> lines = Arrays.asList("host:localhost", "port:");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        assertNull(propertiesReader.getProperty("port"));

        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_host_null() throws IOException {

        final List<String> lines = Arrays.asList("host:", "port:3000");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        assertNull(propertiesReader.getProperty("host"));

        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void all_properties_empty() throws IOException {

        final List<String> lines = Arrays.asList(
                "mode:",
                "host:",
                "port:",
                "tags:",
                "prefix:",
                "protocol:",
                "database:",
                "connectTimeout:",
                "reportingInterval:",
                "auth:");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);

        propertiesReader.readPropertiesFromFile();


        assertFalse(propertiesReader.validateConfiguration());
        assertEquals("http", propertiesReader.getMode());
        assertTrue(propertiesReader.getTags().isEmpty());
        assertEquals("", propertiesReader.getPrefix());
        assertEquals("http", propertiesReader.getProtocol());
        assertEquals("hivemq", propertiesReader.getDatabase());
        assertEquals(5000, propertiesReader.getConnectTimeout());
        assertEquals(1, propertiesReader.getReportingInterval());
        assertNull(propertiesReader.getAuth());
        assertNull(propertiesReader.getHost());
        assertNull(propertiesReader.getPort());
    }

    @Test
    public void all_properties_null() throws IOException {

        final List<String> lines = Collections.emptyList();
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final String mode = propertiesReader.getMode();

        assertFalse(propertiesReader.validateConfiguration());
        assertEquals("http", propertiesReader.getMode());
        assertTrue(propertiesReader.getTags().isEmpty());
        assertEquals("", propertiesReader.getPrefix());
        assertEquals("http", propertiesReader.getProtocol());
        assertEquals("hivemq", propertiesReader.getDatabase());
        assertEquals(5000, propertiesReader.getConnectTimeout());
        assertEquals(1, propertiesReader.getReportingInterval());
        assertNull(propertiesReader.getAuth());
        assertNull(propertiesReader.getHost());
        assertNull(propertiesReader.getPort());
    }

    @Test
    public void all_properties_have_correct_values() throws IOException {

        final List<String> lines = Arrays.asList(
                "mode:tcp",
                "host:hivemq.monitoring.com",
                "port:3000",
                "tags:host=hivemq1;version=3.4.1",
                "prefix:node1",
                "protocol:tcp",
                "database:test-hivemq",
                "connectTimeout:10000",
                "reportingInterval:5",
                "auth:username:password");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        @NotNull final Map<String, String> tags = propertiesReader.getTags();

        assertTrue(propertiesReader.validateConfiguration());
        assertEquals("tcp", propertiesReader.getMode());
        assertEquals(2, tags.size());
        assertEquals("hivemq1", tags.get("host"));
        assertEquals("3.4.1", tags.get("version"));
        assertEquals("node1", propertiesReader.getPrefix());
        assertEquals("tcp", propertiesReader.getProtocol());
        assertEquals("test-hivemq", propertiesReader.getDatabase());
        assertEquals(10000, propertiesReader.getConnectTimeout());
        assertEquals(5, propertiesReader.getReportingInterval());
        assertEquals("username:password", propertiesReader.getAuth());
        assertEquals("hivemq.monitoring.com", propertiesReader.getHost());
        assertEquals(3000, propertiesReader.getPort().intValue());
    }

    @Test
    public void tags_invalid_configured() throws IOException {

        final List<String> lines = Collections.singletonList(
                "tags:host=hivemq1;version=;use=monitoring");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        final Map<String, String> tags = propertiesReader.getTags();

        assertFalse(propertiesReader.validateConfiguration());
        assertEquals(2, tags.size());
        assertEquals("hivemq1", tags.get("host"));
        assertEquals("monitoring", tags.get("use"));
        assertNull(tags.get("version"));
    }

    @Test
    public void tags_has_only_semicolons() throws IOException {

        final List<String> lines = Collections.singletonList(
                "tags:;;;;;;");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        final Map<String, String> tags = propertiesReader.getTags();

        assertEquals(0, tags.size());
    }

    @Test
    public void tags_has_only_a_key() throws IOException {

        final List<String> lines = Collections.singletonList(
                "tags:okay");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        final Map<String, String> tags = propertiesReader.getTags();

        assertEquals(0, tags.size());
    }

    @Test
    public void tags_has_correct_tag_but_missing_semicolon() throws IOException {

        final List<String> lines = Collections.singletonList(
                "tags:key=value");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        final Map<String, String> tags = propertiesReader.getTags();

        assertEquals(1, tags.size());
    }

    @Test
    public void properties_that_are_numbers_have_invalid_string() throws IOException {

        final List<String> lines = Arrays.asList("host:test",
                "port:800000", "reportingInterval:0", "connectTimeout:-1");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbConfiguration propertiesReader = new InfluxDbConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        //false because port is out of range
        assertFalse(propertiesReader.validateConfiguration());

        //default values because values in file are no valid (zero or negative number)
        assertEquals(5000, propertiesReader.getConnectTimeout());
        assertEquals(1, propertiesReader.getReportingInterval());
    }

}