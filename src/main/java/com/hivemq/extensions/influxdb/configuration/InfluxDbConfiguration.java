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
 * Reads a property file containing InfluxDB properties
 * and provides some utility methods for working with {@link Properties}.
 *
 * @author Michael Walter
 */
public class InfluxDbConfiguration extends PropertiesReader {

    private static final @NotNull String PROPERTY_HOST = "host";
    private static final @NotNull String PROPERTY_PORT = "port";
    private static final @NotNull String PROPERTY_MODE = "mode";
    private static final @NotNull String PROPERTY_DATABASE = "database";
    private static final @NotNull String PROPERTY_REPORTING_INTERVAL = "reportingInterval";
    private static final @NotNull String PROPERTY_CONNECT_TIMEOUT = "connectTimeout";
    private static final @NotNull String PROPERTY_PROTOCOL = "protocol";
    private static final @NotNull String PROPERTY_PREFIX = "prefix";
    private static final @NotNull String PROPERTY_AUTH = "auth";
    private static final @NotNull String PROPERTY_TAGS = "tags";
    private static final @NotNull String PROPERTY_BUCKET = "bucket";
    private static final @NotNull String PROPERTY_ORGANIZATION = "organization";
    private static final @NotNull String PROPERTY_VERSION = "version";

    private static final @NotNull String DEFAULT_MODE = "http";
    private static final @NotNull String DEFAULT_DATABASE = "hivemq";
    private static final int DEFAULT_REPORTING_INTERVAL = 1;
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final @NotNull String DEFAULT_PREFIX = "";
    private static final @NotNull Map<String, String> DEFAULT_TAGS = Map.of();

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(InfluxDbConfiguration.class);

    public InfluxDbConfiguration(final @NotNull File configFile) {
        super(configFile);
    }

    /**
     * Check if mandatory properties exist and are valid. Mandatory properties are port and host.
     *
     * @return <b>true</b> if all mandatory properties exist, else <b>false</b>.
     */
    public boolean validateConfiguration() {
        var errorCount = 0;
        errorCount += checkMandatoryProperty(PROPERTY_HOST);
        errorCount += checkMandatoryProperty(PROPERTY_PORT);
        if (errorCount != 0) {
            return false;
        }
        // check if host wasn't configured
        final var host = getProperty(PROPERTY_HOST);
        if ("<INFLUXDB IP>".equals(host) || "--INFLUX-DB-IP--".equals(host)) {
            errorCount++;
        }
        // check for valid port value
        final var port = validateIntProperty(PROPERTY_PORT, -1);
        if (port < 0 || port > 65535) {
            LOG.error("Value for mandatory InfluxDB property '{}' is not in valid port range", PROPERTY_PORT);
            errorCount++;
        }
        // check for valid version value
        if (getProperty(PROPERTY_VERSION) != null) {
            final var version = validateIntProperty(PROPERTY_VERSION, -1);
            if (version < 1 || version > 3) {
                LOG.error("Unsupported InfluxDB version {} (supported versions: 1, 2, 3)", version);
                errorCount++;
            }
        }
        // check for valid cloud configuration
        if ("cloud".equals(getProperty(PROPERTY_MODE))) {
            var cloudError = 0;
            cloudError += checkMandatoryProperty(PROPERTY_AUTH);
            cloudError += checkMandatoryProperty(PROPERTY_BUCKET);
            cloudError += checkMandatoryProperty(PROPERTY_ORGANIZATION);
            if (cloudError > 0) {
                LOG.warn("Mandatory properties for InfluxDB mode 'cloud' are not configured");
            }
            errorCount += cloudError;
        }
        return errorCount == 0;
    }

    /**
     * Check if mandatory property exists.
     *
     * @param property Property to check.
     * @return 0 if property exists, else 1.
     */
    private int checkMandatoryProperty(final @NotNull String property) {
        Objects.requireNonNull(property, "Value for mandatory InfluxDB property is not defined");
        final var value = getProperty(property);
        if (value == null) {
            LOG.error("Value for mandatory InfluxDB property '{}' is not defined", property);
            return 1;
        }
        return 0;
    }

    public @NotNull String getMode() {
        return validateStringProperty(PROPERTY_MODE, DEFAULT_MODE);
    }

    public @NotNull String getHost() {
        return Objects.requireNonNullElse(getProperty(PROPERTY_HOST), "");
    }

    public @NotNull String getDatabase() {
        return validateStringProperty(PROPERTY_DATABASE, DEFAULT_DATABASE);
    }

    public int getPort() {
        return validateIntProperty(PROPERTY_PORT, 0);
    }

    public int getReportingInterval() {
        return validateIntProperty(PROPERTY_REPORTING_INTERVAL, DEFAULT_REPORTING_INTERVAL);
    }

    public int getConnectTimeout() {
        return validateIntProperty(PROPERTY_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
    }

    public @NotNull String getProtocolOrDefault(final @NotNull String defaultProtocol) {
        final var protocol = getProperty(PROPERTY_PROTOCOL);
        if (protocol == null) {
            LOG.warn("No protocol configured for InfluxDB in mode '{}', using default '{}'",
                    getMode(),
                    defaultProtocol);
            return defaultProtocol;
        }
        return protocol;
    }

    public @NotNull String getPrefix() {
        return validateStringProperty(PROPERTY_PREFIX, DEFAULT_PREFIX);
    }

    public @Nullable String getAuth() {
        return getProperty(PROPERTY_AUTH);
    }

    public @NotNull Map<String, String> getTags() {
        final var tags = getProperty(PROPERTY_TAGS);
        if (tags == null) {
            return DEFAULT_TAGS;
        }
        final var split = StringUtils.splitPreserveAllTokens(tags, ";");
        final var tagMap = new HashMap<String, String>();
        for (final var tag : split) {
            final var tagPair = StringUtils.split(tag, "=");
            if (tagPair.length != 2 || tagPair[0].isEmpty() || tagPair[1].isEmpty()) {
                LOG.warn("Invalid tag format '{}' for InfluxDB", tag);
                continue;
            }
            tagMap.put(tagPair[0], tagPair[1]);
        }
        return tagMap;
    }

    public @Nullable String getBucket() {
        return getProperty(PROPERTY_BUCKET);
    }

    public @Nullable String getOrganization() {
        return getProperty(PROPERTY_ORGANIZATION);
    }

    /**
     * Get the configured InfluxDB version.
     *
     * @return the configured or auto-detected version (1, 2, or 3)
     */
    public int getVersion() {
        final var value = getProperty(PROPERTY_VERSION);
        if (value != null) {
            return validateIntProperty(PROPERTY_VERSION, 1);
        }
        LOG.warn("InfluxDB version not specified, please add a valid version to your configuration");
        final var mode = getMode();
        if ("cloud".equals(mode)) {
            LOG.info("Auto-detected as v2 based on mode 'cloud'");
            return 2;
        } else {
            LOG.info("Auto-detected as v1 based on mode '{}'", mode);
            return 1;
        }
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
                LOG.warn("No '{}' configured for InfluxDB, using default {}", key, defaultValue);
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
        Objects.requireNonNull(properties, "No properties loaded");
        Objects.requireNonNull(key, "Key to fetch property must not be null");
        final var value = properties.getProperty(key);
        if (value == null) {
            LOG.warn("No InfluxDB property '{}' configured, using default {}", key, defaultValue);
            return defaultValue;
        }
        final int valueAsInt;
        try {
            valueAsInt = Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            LOG.warn("Value {} for InfluxDB property '{}' is not a number, using default {}", value, key, defaultValue);
            return defaultValue;
        }
        if (valueAsInt == 0) {
            LOG.warn("Value for InfluxDB property '{}' can't be zero, using default {}", key, defaultValue);
            return defaultValue;
        }
        if (valueAsInt < 0) {
            LOG.warn("Value for InfluxDB property '{}' can't be negative, using default {}", key, defaultValue);
            return defaultValue;
        }
        return valueAsInt;
    }
}
