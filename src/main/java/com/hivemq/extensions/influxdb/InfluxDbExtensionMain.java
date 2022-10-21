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
package com.hivemq.extensions.influxdb;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.google.common.collect.Sets;
import com.hivemq.extensions.influxdb.configuration.InfluxDbConfiguration;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.izettle.metrics.influxdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michael Walter
 */
public class InfluxDbExtensionMain implements ExtensionMain {

    private static final Logger log = LoggerFactory.getLogger(InfluxDbExtensionMain.class);
    private static final HashSet<String> METER_FIELDS = Sets.newHashSet("count", "m1_rate", "m5_rate", "m15_rate", "mean_rate");
    private static final HashSet<String> TIMER_FIELDS = Sets.newHashSet("count", "min", "max", "mean", "stddev", "p50", "p75", "p95", "p98", "p99", "p999", "m1_rate", "m5_rate", "m15_rate", "mean_rate");

    private ScheduledReporter reporter;

    @Override
    public void extensionStart(@NotNull final ExtensionStartInput extensionStartInput, @NotNull final ExtensionStartOutput extensionStartOutput) {

        try {
            final File extensionHomeFolder = extensionStartInput.getExtensionInformation().getExtensionHomeFolder();
            final InfluxDbConfiguration configuration = new InfluxDbConfiguration(extensionHomeFolder);

            if (!configuration.readPropertiesFromFile()) {
                extensionStartOutput.preventExtensionStartup("Could not read influxdb properties");
                return;
            }

            if (!configuration.validateConfiguration()) {
                extensionStartOutput.preventExtensionStartup("At least one mandatory property not set");
                return;
            }

            final InfluxDbSender sender = setupSender(configuration);

            if (sender == null) {
                extensionStartOutput.preventExtensionStartup("Couldn't create an influxdb sender. Please check that the configuration is correct");
                return;
            }

            final MetricRegistry metricRegistry = Services.metricRegistry();
            reporter = setupReporter(metricRegistry, sender, configuration);
            reporter.start(configuration.getReportingInterval(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Start failed because of", e);
            extensionStartOutput.preventExtensionStartup("Start failed because of an exception");
        }
    }

    @Override
    public void extensionStop(@NotNull final ExtensionStopInput extensionStopInput, @NotNull final ExtensionStopOutput extensionStopOutput) {
        if (reporter != null) {
            reporter.stop();
        }
    }

    @NotNull
    private ScheduledReporter setupReporter(@NotNull final MetricRegistry metricRegistry, @NotNull final InfluxDbSender sender, @NotNull final InfluxDbConfiguration configuration) {
        checkNotNull(metricRegistry, "MetricRegistry for influxdb must not be null");
        checkNotNull(sender, "InfluxDbSender for influxdb must not be null");
        checkNotNull(configuration, "Configuration for influxdb must not be null");

        final Map<String, String> tags = configuration.getTags();

        return InfluxDbReporter.forRegistry(metricRegistry)
                .withTags(tags)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .groupGauges(false)
                .skipIdleMetrics(false)
                .includeMeterFields(METER_FIELDS)
                .includeTimerFields(TIMER_FIELDS)
                .build(sender);
    }

    @Nullable
    private InfluxDbSender setupSender(@NotNull final InfluxDbConfiguration configuration) {
        checkNotNull(configuration, "Configuration for influxdb must not be null");

        final String host = configuration.getHost();
        final int port = configuration.getPort();
        final String protocol = configuration.getProtocol();
        final String database = configuration.getDatabase();
        final String auth = configuration.getAuth();
        final int connectTimeout = configuration.getConnectTimeout();
        final String prefix = configuration.getPrefix();

        // cloud
        final String bucket = configuration.getBucket();
        final String organization = configuration.getOrganization();

        InfluxDbSender sender = null;

        try {
            switch (configuration.getMode()) {
                case "http":
                    log.info("Creating InfluxDB HTTP sender for server {}:{} and database {}", host, port, database);
                    sender = new InfluxDbHttpSender(protocol, host, port, database, auth, TimeUnit.SECONDS, connectTimeout, connectTimeout, prefix);
                    break;
                case "tcp":
                    log.info("Creating InfluxDB TCP sender for server {}:{} and database {}", host, port, database);
                    sender = new InfluxDbTcpSender(host, port, connectTimeout, database, prefix);
                    break;
                case "udp":
                    log.info("Creating InfluxDB UDP sender for server {}:{} and database {}", host, port, database);
                    sender = new InfluxDbUdpSender(host, port, connectTimeout, database, prefix);
                    break;
                case "cloud":
                    log.info("Creating InfluxDB Cloud sender for endpoint {}, bucket {}, organization {}", host, bucket, organization);
                    checkNotNull(bucket, "Bucket name must be defined in cloud mode");
                    checkNotNull(organization, "Organization must be defined in cloud mode");
                    sender = new InfluxDbCloudSender(protocol, host, port, auth, TimeUnit.SECONDS, connectTimeout, connectTimeout, prefix, organization, bucket);
                    break;

            }
        } catch (Exception ex) {
            log.error("Not able to start InfluxDB sender, please check your configuration: {}", ex.getMessage());
            log.debug("Original Exception: ", ex);
        }

        return sender;
    }

}
