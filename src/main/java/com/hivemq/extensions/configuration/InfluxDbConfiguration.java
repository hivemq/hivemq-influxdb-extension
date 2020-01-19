/*
 * Copyright 2018 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads a property file containing influxdb properties
 * and provides some utility methods for working with {@link Properties}.
 *
 * @author Christoph Schäbel
 * @author Michael Walter
 */
public class InfluxDbConfiguration extends PropertiesReader {

    private static final Logger log = LoggerFactory.getLogger(InfluxDbConfiguration.class);

    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String MODE = "mode";
    private static final String MODE_DEFAULT = "http";
    private static final String PROTOCOL = "protocol";
    private static final String PROTOCOL_DEFAULT = "http";
    private static final String REPORTING_INTERVAL = "reportingInterval";
    private static final String FILTERED_REPORTING_INTERVAL = "filteredReportingInterval";
    private static final int REPORTING_INTERVAL_DEFAULT = 1;
    private static final String PREFIX = "prefix";
    private static final String PREFIX_DEFAULT = "";
    private static final String DATABASE = "database";
    private static final String DATABASE_DEFAULT = "hivemq";
    private static final String CONNECT_TIMEOUT = "connectTimeout";
    private static final int CONNECT_TIMEOUT_DEFAULT = 5000;
    private static final String AUTH = "auth";
    private static final String TAGS = "tags";
    private static final HashMap<String, String> TAGS_DEFAULT = new HashMap<>();
    private static final String METRICS_FILTER_LIST = "metricsFilterList";
    private static final String CONSOLE_DEBUG = "consoleDebug";

    //InfluxDB Cloud
    private static final String BUCKET = "bucket";
    private static final String ORGANIZATION = "organization";


    public InfluxDbConfiguration(@NotNull final File configFilePath) {
        super(configFilePath);
    }

    /**
     * Check if mandatory properties exist and are valid. Mandatory properties are port and host.
     *
     * @return <b>true</b> if all mandatory properties exist, else <b>false</b>.
     */
    public boolean validateConfiguration() {
        int countError = 0;

        if (Boolean.parseBoolean(getProperty(CONSOLE_DEBUG))) return true;

        countError += checkMandatoryProperty(HOST);
        countError += checkMandatoryProperty(PORT);

        if (countError != 0){
            return false;
        }

        // check for valid port value
        final String port = getProperty(PORT);
        try {
            final int intPort = Integer.parseInt(port);

            if (intPort < 0 || intPort > 65535) {
                log.error("Value for mandatory InfluxDB property {} is not in valid port range.", PORT);
                countError++;
            }

        } catch (NumberFormatException e) {
            log.error("Value for mandatory InfluxDB property {} is not a number.", PORT);
            countError++;
        }

        // check if host is still --INFLUX-DB-IP--
        final String host = getProperty(HOST);

        if (host.equals("--INFLUX-DB-IP--")) {
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
    private int checkMandatoryProperty(@NotNull final String property) {
        checkNotNull(property, "Mandatory property must not be null");

        final String value = getProperty(property);

        if (value == null) {
            log.error("Mandatory property {} is not set.", property);
            return 1;
        }
        return 0;
    }


    @NotNull
    public String getMode() {
        return validateStringProperty(MODE, MODE_DEFAULT);
    }

    @Nullable
    public String getHost() {
        return getProperty(HOST);
    }

    @NotNull
    public String getDatabase() {
        return validateStringProperty(DATABASE, DATABASE_DEFAULT);
    }

    @Nullable
    public Integer getPort() {

        final Integer port;

        try {
            port = Integer.parseInt(getProperty(PORT));
        } catch (NumberFormatException e) {
            log.error("Value for {} is not a number", PORT);
            return null;
        }

        return port;
    }

    public int getReportingInterval() {
        return validateIntProperty(REPORTING_INTERVAL, REPORTING_INTERVAL_DEFAULT, false, false);
    }

    public int getFilteredReportingInterval() {
        return validateIntProperty(FILTERED_REPORTING_INTERVAL, REPORTING_INTERVAL_DEFAULT, false, false);
    }

    public int getConnectTimeout() {
        return validateIntProperty(CONNECT_TIMEOUT, CONNECT_TIMEOUT_DEFAULT, false, false);
    }

    @NotNull
    public String getProtocol() {
        final String protocol = getProperty(PROTOCOL);
        if (protocol == null) {
            if (getMode().equals("http")) {
                log.warn("No protocol configured for InfluxDb, using default: {}", PROTOCOL_DEFAULT);
                return PROTOCOL_DEFAULT;
            }
        }
        return protocol;
    }

    @NotNull
    public String getPrefix() {
        return validateStringProperty(PREFIX, PREFIX_DEFAULT);
    }

    @Nullable
    public String getAuth() {
        return getProperty(AUTH);
    }

    @NotNull
    public Map<String, String> getTags() {

        final String tags = getProperty(TAGS);
        if (tags == null) {
            return TAGS_DEFAULT;
        }

        final String[] split = StringUtils.splitPreserveAllTokens(tags, ";");

        final HashMap<String, String> tagMap = new HashMap<>();

        for (String tag : split) {
            final String[] tagPair = StringUtils.split(tag, "=");
            if (tagPair.length != 2 || tagPair[0].length() < 1 || tagPair[1].length() < 1) {
                log.warn("Invalid tag format {} for InfluxDB", tag);
                continue;
            }

            tagMap.put(tagPair[0], tagPair[1]);
        }

        return tagMap;
    }

    @Nullable
    public String getBucket() {
        return getProperty(BUCKET);
    }

    @Nullable
    public String getOrganization() {
        return getProperty(ORGANIZATION);
    }

    @Override
    public String getFilename() {
        return "influxdb.properties";
    }

    @Nullable
    public String getMetricFilter() {
        return getProperty(METRICS_FILTER_LIST);
    }

    @Nullable
    public boolean consoleDebugging() {
        return Boolean.parseBoolean(getProperty(CONSOLE_DEBUG));
    }

    /**
     * Fetch property with given <b>key</b>. If the fetched {@link String} is <b>null</b> the <b>defaultValue</b> will be returned.
     *
     * @param key          Key of the property.
     * @param defaultValue Default value as fallback, if property has no value.
     * @return the actual value of the property if it is set, else the <b>defaultValue</b>.
     */
    private String validateStringProperty(@NotNull final String key, @NotNull final String defaultValue) {
        checkNotNull(key, "Key to fetch property must not be null");
        checkNotNull(defaultValue, "Default value for property must not be null");

        final String value = getProperty(key);

        if (value == null) {

            if (!defaultValue.isEmpty()) {
                log.warn("No '{}' configured for InfluxDb, using default: {}", key, defaultValue);
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
     * @param key             Key of the property
     * @param defaultValue    Default value as fallback, if property has no value
     * @param zeroAllowed     use <b>true</b> if property can be zero
     * @param negativeAllowed use <b>true</b> is property can be negative int
     * @return the actual value of the property if it is set and valid, else the <b>defaultValue</b>
     */
    private int validateIntProperty(@NotNull final String key, final int defaultValue, final boolean zeroAllowed, final boolean negativeAllowed) {
        checkNotNull(key, "Key to fetch property must not be null");

        final String value = properties.getProperty(key);

        if (value == null) {
            log.warn("No '{}' configured for InfluxDb, using default: {}", key, defaultValue);
            return defaultValue;
        }

        int valueAsInt;

        try {
            valueAsInt = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Value for InfluxDB property '{}' is not a number, original value {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }

        if (!zeroAllowed && valueAsInt == 0) {
            log.warn("Value for InfluxDB property '{}' can't be zero. Using default: {}", key, defaultValue);
            return defaultValue;
        }

        if (!negativeAllowed && valueAsInt < 0) {
            log.warn("Value for InfluxDB property '{}' can't be negative. Using default: {}", key, defaultValue);
            return defaultValue;
        }

        return valueAsInt;
    }
}