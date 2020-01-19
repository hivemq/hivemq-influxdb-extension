package com.hivemq.extensions.configuration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PropertiesReaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void readPropertiesFromFile_file_null() {
        getPropertyReader(null);
    }

    @Test
    public void readPropertiesFromFile_file_does_not_exist() {
        final File root = folder.getRoot();
        final PropertiesReader propertiesReader = getPropertyReader(root);

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertFalse(fileExists);
    }

    @Test
    public void readPropertiesFromFile_file_does_exist() throws IOException {
        final File root = folder.getRoot();
        folder.newFile("test");

        final PropertiesReader propertiesReader = getPropertyReader(root);
        final boolean fileExists = propertiesReader.readPropertiesFromFile();

        assertTrue(fileExists);
    }

    @Test(expected = NullPointerException.class)
    public void getProperty_key_null() throws IOException {
        final List<String> lines = Collections.singletonList("key:value");
        final PropertiesReader propertiesReader = getPropertyReaderFromFile(lines);

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertTrue(fileExists);

        final String property = propertiesReader.getProperty("key");
        assertEquals("value", property);

        propertiesReader.getProperty(null);
    }

    @Test
    public void getProperty_key_doesnt_exist() throws IOException {
        final List<String> lines = Collections.singletonList("key:value");
        final PropertiesReader propertiesReader = getPropertyReaderFromFile(lines);

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertTrue(fileExists);

        final String property = propertiesReader.getProperty("key");
        assertEquals("value", property);

        final String property1 = propertiesReader.getProperty("unknown");
        assertNull(property1);
    }

    @Test
    public void getProperty_key_exists() throws IOException {
        final List<String> lines = Collections.singletonList("key:value");
        final PropertiesReader propertiesReader = getPropertyReaderFromFile(lines);

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertTrue(fileExists);

        final String property = propertiesReader.getProperty("key");
        assertEquals("value", property);
    }

    @Test
    public void getProperty_before_loading_properties() throws IOException {
        final List<String> lines = Collections.singletonList("key:value");
        final PropertiesReader propertiesReader = getPropertyReaderFromFile(lines);

        final String property = propertiesReader.getProperty("key");
        assertNull(property);
    }

    private PropertiesReader getPropertyReaderFromFile(List<String> lines) throws IOException {
        writeFile(lines);
        return getPropertyReader(folder.getRoot());
    }

    private void writeFile(List<String> lines) throws IOException {
        final File file = folder.newFile("test");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    private PropertiesReader getPropertyReader(File configFilePath) {
        return new PropertiesReader(configFilePath) {
            @Override
            public String getFilename() {
                return "test";
            }
        };
    }
}