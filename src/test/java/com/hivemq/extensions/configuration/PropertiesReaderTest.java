package com.hivemq.extensions.configuration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PropertiesReaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void readPropertiesFromFile_file_null() {
        new PropertiesReader(null) {
            @Override
            public String getFilename() {
                return "test";
            }
        };
    }


    @Test
    public void readPropertiesFromFile_file_does_not_exist() {

        final File root = folder.getRoot();

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();

        assertFalse(fileExists);
    }

    @Test
    public void readPropertiesFromFile_file_does_exist() throws IOException {
        final File root = folder.getRoot();

        folder.newFile("test");

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();

        assertTrue(fileExists);
    }

    @Test(expected = NullPointerException.class)
    public void getProperty_key_null() throws IOException {
        final File root = folder.getRoot();

        final File file = folder.newFile("test");

        final List<String> lines = Collections.singletonList("key:value");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertTrue(fileExists);

        final String property = propertiesReader.getProperty("key");
        assertEquals("value", property);

        propertiesReader.getProperty(null);
    }

    @Test
    public void getProperty_key_doesnt_exist() throws IOException {
        final File root = folder.getRoot();

        final File file = folder.newFile("test");

        final List<String> lines = Collections.singletonList("key:value");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertTrue(fileExists);

        final String property = propertiesReader.getProperty("key");
        assertEquals("value", property);

        final String property1 = propertiesReader.getProperty("unknown");
        assertNull(property1);
    }

    @Test
    public void getProperty_key_exists() throws IOException {
        final File root = folder.getRoot();

        final File file = folder.newFile("test");

        final List<String> lines = Collections.singletonList("key:value");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertTrue(fileExists);

        final String property = propertiesReader.getProperty("key");
        assertEquals("value", property);
    }

    @Test
    public void getProperty_before_loading_properties() throws IOException {
        final File root = folder.getRoot();

        final File file = folder.newFile("test");

        final List<String> lines = Collections.singletonList("key:value");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final String property = propertiesReader.getProperty("key");
        assertNull(property);
    }

}