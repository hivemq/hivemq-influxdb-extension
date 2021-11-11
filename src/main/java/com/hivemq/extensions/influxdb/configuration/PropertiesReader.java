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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Load the content of a {@link File} into {@link Properties}.
 *
 * @author Michael Walter
 */
public abstract class PropertiesReader {

    private static final Logger log = LoggerFactory.getLogger(PropertiesReader.class);

    private final File configFilePath;
    Properties properties;

    PropertiesReader(@NotNull final File configFilePath) {
        checkNotNull(configFilePath, "Path to config file must not be null");
        this.configFilePath = configFilePath;
    }

    /**
     * Read the {@link Properties} from the properties {@link File}.
     *
     * @return <b>true</b> if properties are loaded, else <b>false</b>.
     */
    public boolean readPropertiesFromFile() {

        final File file = new File(configFilePath + File.separator + getFilename());

        try {
            loadProperties(file);

        } catch (IOException e) {
            log.error("Not able to load configuration file '{}'", file.getAbsolutePath());
            return false;
        }

        return true;
    }

    /**
     * Fetch a property with given key from {@link Properties}.
     *
     * @param key The name of the property to look for.
     * @return The property for the value if it exists, <b>null</b> if key or {@link Properties} doesn't exist or the value is an empty string.
     */
    @Nullable
    String getProperty(@NotNull final String key) {
        checkNotNull(key, "Key to fetch property for must not be null.");

        if (properties == null) {
            return null;
        }

        final String property = properties.getProperty(key);

        if (property == null || property.isEmpty()) {
            return null;
        }

        return property;
    }

    /**
     * Loads the properties from the configuration {@link File} into {@link Properties}.
     *
     * @param file {@link File} where to load the properties from.
     * @throws IOException If properties could not be read from <b>file</b>.
     */
    private void loadProperties(@NotNull final File file) throws IOException {
        checkNotNull(file, "File that contains properties must not be null");

        try (final FileReader in = new FileReader(file)) {
            properties = new Properties();
            properties.load(in);
        }
    }

    public abstract String getFilename();
}