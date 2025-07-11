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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Reads a property file containing influxdb properties
 * and provides some utility methods for working with {@link Properties}.
 *
 * @author Christoph Schäbel
 * @author Michael Walter
 */
public class InfluxDbConfiguration extends PropertiesReader {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(InfluxDbConfiguration.class);

    private static final @NotNull String HOST = "host";
    private static final @NotNull String PORT = "port";
    private static final @NotNull String MODE = "mode";
    private static final @NotNull String MODE_DEFAULT = "http";
    private static final @NotNull String PROTOCOL = "protocol";
    private static final @NotNull String REPORTING_INTERVAL = "reportingInterval";
    private static final int REPORTING_INTERVAL_DEFAULT = 1;
    private static final @NotNull String PREFIX = "prefix";
    private static final @NotNull String PREFIX_DEFAULT = "";
    private static final @NotNull String DATABASE = "database";
    private static final @NotNull String DATABASE_DEFAULT = "hivemq";
    private static final @NotNull String CONNECT_TIMEOUT = "connectTimeout";
    private static final int CONNECT_TIMEOUT_DEFAULT = 5000;
    private static final @NotNull String AUTH = "auth";
    private static final @NotNull String TAGS = "tags";
    private static final @NotNull HashMap<String, String> TAGS_DEFAULT = new HashMap<>();

    //InfluxDB Cloud
    private static final @NotNull String BUCKET = "bucket";
    private static final @NotNull String ORGANIZATION = "organization";


    public InfluxDbConfiguration(final @NotNull File configFilePath) {
        super(configFilePath);
    }

    /**
     * Check if mandatory properties exist and are valid. Mandatory properties are port and host.
     *
     * @return <b>true</b> if all mandatory properties exist, else <b>false</b>.
     */
    public boolean validateConfiguration() {
        int countError = 0;
        countError += checkMandatoryProperty(HOST);
        countError += checkMandatoryProperty(PORT);
        if (countError != 0) {
            return false;
        }
        // check for valid port value
        final var port = getProperty(PORT);
        try {
            final var intPort = Integer.parseInt(port);
            if (intPort < 0 || intPort > 65535) {
                LOG.error("Value for mandatory InfluxDB property {} is not in valid port range.", PORT);
                countError++;
            }
        } catch (final NumberFormatException e) {
            LOG.error("Value for mandatory InfluxDB property {} is not a number.", PORT);
            countError++;
        }
        // check if host is still --INFLUX-DB-IP--
        final var host = getProperty(HOST);
        if ("--INFLUX-DB-IP--".equals(host)) {
            countError++;
        }
        return countError == 0;
    }

    /**
     * Check if mandatory property exists.
     *
     * @param property Property to check.
     * @return 0 if property exists, else 1.
     */
    private int checkMandatoryProperty(final @NotNull String property) {
        Objects.requireNonNull(property, "Mandatory property must not be null");
        final var value = getProperty(property);
        if (value == null) {
            LOG.error("Mandatory property {} is not set.", property);
            return 1;
        }
        return 0;
    }

    public @NotNull String getMode() {
        return validateStringProperty(MODE, MODE_DEFAULT);
    }

    public @Nullable String getHost() {
        return getProperty(HOST);
    }

    public @NotNull String getDatabase() {
        return validateStringProperty(DATABASE, DATABASE_DEFAULT);
    }

    public @Nullable Integer getPort() {
        final int port;
        try {
            port = Integer.parseInt(getProperty(PORT));
        } catch (final NumberFormatException e) {
            LOG.error("Value for {} is not a number", PORT);
            return null;
        }
        return port;
    }

    public int getReportingInterval() {
        return validateIntProperty(REPORTING_INTERVAL, REPORTING_INTERVAL_DEFAULT);
    }

    public int getConnectTimeout() {
        return validateIntProperty(CONNECT_TIMEOUT, CONNECT_TIMEOUT_DEFAULT);
    }

    public @NotNull String getProtocolOrDefault(final @NotNull String defaultProtocol) {
        final var protocol = getProperty(PROTOCOL);
        if (protocol == null) {
            LOG.warn("No protocol configured for InfluxDb in mode '{}', using default: '{}'",
                    getMode(),
                    defaultProtocol);
            return defaultProtocol;
        }
        return protocol;
    }

    public @NotNull String getPrefix() {
        return validateStringProperty(PREFIX, PREFIX_DEFAULT);
    }

    public @Nullable String getAuth() {
        return getProperty(AUTH);
    }

    public @NotNull Map<String, String> getTags() {
        final var tags = getProperty(TAGS);
        if (tags == null) {
            return TAGS_DEFAULT;
        }
        final var split = StringUtils.splitPreserveAllTokens(tags, ";");
        final var tagMap = new HashMap<String, String>();
        for (final var tag : split) {
            final var tagPair = StringUtils.split(tag, "=");
            if (tagPair.length != 2 || tagPair[0].isEmpty() || tagPair[1].isEmpty()) {
                LOG.warn("Invalid tag format {} for InfluxDB", tag);
                continue;
            }
            tagMap.put(tagPair[0], tagPair[1]);
        }
        return tagMap;
    }

    public @Nullable String getBucket() {
        return getProperty(BUCKET);
    }

    public @Nullable String getOrganization() {
        return getProperty(ORGANIZATION);
    }

    @Override
    public @NotNull String getFilename() {
        return "influxdb.properties";
    }

    /**
     * Fetch property with given <b>key</b>. If the fetched {@link String} is <b>null</b> the <b>defaultValue</b> will
     * be returned.
     *
     * @param key          Key of the property.
     * @param defaultValue Default value as fallback, if property has no value.
     * @return the actual value of the property if it is set, else the <b>defaultValue</b>.
     */
    private String validateStringProperty(final @NotNull String key, final @NotNull String defaultValue) {
        Objects.requireNonNull(key, "Key to fetch property must not be null");
        Objects.requireNonNull(defaultValue, "Default value for property must not be null");
        final var value = getProperty(key);
        if (value == null) {
            if (!defaultValue.isEmpty()) {
                LOG.warn("No '{}' configured for InfluxDb, using default: {}", key, defaultValue);
            }
            return defaultValue;
        }
        return value;
    }

    /**
     * Fetch property with given <b>key</b>.
     * If the fetched {@link String} value is not <b>null</b> convert the value to an int and check validation
     * constraints if given flags are <b>false</b> before returning the value.
     *
     * @param key          Key of the property
     * @param defaultValue Default value as fallback, if property has no value
     * @return the actual value of the property if it is set and valid, else the <b>defaultValue</b>
     */
    private int validateIntProperty(final @NotNull String key, final int defaultValue) {
        Objects.requireNonNull(key, "Key to fetch property must not be null");
        final var value = properties.getProperty(key);
        if (value == null) {
            LOG.warn("No '{}' configured for InfluxDb, using default: {}", key, defaultValue);
            return defaultValue;
        }
        final int valueAsInt;
        try {
            valueAsInt = Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            LOG.warn("Value for InfluxDB property '{}' is not a number, original value {}. Using default: {}",
                    key,
                    value,
                    defaultValue);
            return defaultValue;
        }
        if (valueAsInt == 0) {
            LOG.warn("Value for InfluxDB property '{}' can't be zero. Using default: {}", key, defaultValue);
            return defaultValue;
        }
        if (valueAsInt < 0) {
            LOG.warn("Value for InfluxDB property '{}' can't be negative. Using default: {}", key, defaultValue);
            return defaultValue;
        }
        return valueAsInt;
    }
}
