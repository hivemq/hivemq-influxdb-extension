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
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.influxdb.configuration.ConfigResolver;
import com.hivemq.extensions.influxdb.configuration.InfluxDbConfiguration;
import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.InfluxDbReporter;
import com.izettle.metrics.influxdb.InfluxDbSender;
import com.izettle.metrics.influxdb.InfluxDbTcpSender;
import com.izettle.metrics.influxdb.InfluxDbUdpSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Michael Walter
 */
public class InfluxDbExtensionMain implements ExtensionMain {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(InfluxDbExtensionMain.class);

    private static final @NotNull Set<String> METER_FIELDS =
            Set.of("count", "m1_rate", "m5_rate", "m15_rate", "mean_rate");
    private static final @NotNull Set<String> TIMER_FIELDS = Set.of("count",
            "min",
            "max",
            "mean",
            "stddev",
            "p50",
            "p75",
            "p95",
            "p98",
            "p99",
            "p999",
            "m1_rate",
            "m5_rate",
            "m15_rate",
            "mean_rate");

    private @Nullable ScheduledReporter reporter;

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {
        try {
            final var extensionHomeFolder = extensionStartInput.getExtensionInformation().getExtensionHomeFolder();
            final var configResolver = new ConfigResolver(extensionHomeFolder.toPath(),
                    "InfluxDB Extension",
                    "conf/config.properties",
                    "influxdb.properties");
            final var configuration = new InfluxDbConfiguration(configResolver.get().toFile());
            if (!configuration.readPropertiesFromFile()) {
                extensionStartOutput.preventExtensionStartup("Could not read influxdb properties");
                return;
            }
            if (!configuration.validateConfiguration()) {
                extensionStartOutput.preventExtensionStartup("At least one mandatory property not set");
                return;
            }
            final var sender = setupSender(configuration);
            if (sender == null) {
                extensionStartOutput.preventExtensionStartup(
                        "Couldn't create an influxdb sender. Please check that the configuration is correct");
                return;
            }
            final var metricRegistry = Services.metricRegistry();
            reporter = setupReporter(metricRegistry, sender, configuration);
            reporter.start(configuration.getReportingInterval(), TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.warn("Start failed because of", e);
            extensionStartOutput.preventExtensionStartup("Start failed because of an exception");
        }
    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {
        if (reporter != null) {
            reporter.stop();
        }
    }

    private @NotNull ScheduledReporter setupReporter(
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull InfluxDbSender sender,
            final @NotNull InfluxDbConfiguration configuration) {
        Objects.requireNonNull(metricRegistry, "MetricRegistry for influxdb must not be null");
        Objects.requireNonNull(sender, "InfluxDbSender for influxdb must not be null");
        Objects.requireNonNull(configuration, "Configuration for influxdb must not be null");
        final var tags = configuration.getTags();
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

    private @Nullable InfluxDbSender setupSender(final @NotNull InfluxDbConfiguration configuration) {
        Objects.requireNonNull(configuration, "Configuration for influxdb must not be null");
        final var host = configuration.getHost();
        final var port = configuration.getPort();
        final var database = configuration.getDatabase();
        final var auth = configuration.getAuth();
        final var connectTimeout = configuration.getConnectTimeout();
        final var prefix = configuration.getPrefix();
        final var mode = configuration.getMode();

        // cloud / v2
        final var bucket = configuration.getBucket();
        final var organization = configuration.getOrganization();

        // Determine effective version
        var version = configuration.getVersion();
        if (version == null) {
            if ("cloud".equals(mode)) {
                version = 2;
            } else {
                version = 1;
            }
            LOG.warn("InfluxDB version not specified. Auto-detected as v{} based on mode={}. " +
                    "Please add 'version={}' to your configuration.", version, mode, version);
        }

        InfluxDbSender sender = null;
        try {
            switch (version) {
                case 1:
                    sender = setupV1Sender(configuration, mode, host, port, database, auth, connectTimeout, prefix);
                    break;
                case 2:
                    LOG.info("Creating InfluxDB v2 sender for {}, bucket {}, organization {}",
                            host,
                            bucket,
                            organization);
                    Objects.requireNonNull(bucket, "Bucket name must be defined for InfluxDB v2");
                    Objects.requireNonNull(organization, "Organization must be defined for InfluxDB v2");
                    sender = new InfluxDbCloudSender(
                            configuration.getProtocolOrDefault("cloud".equals(mode) ? "https" : "http"),
                            host,
                            port,
                            auth,
                            TimeUnit.SECONDS,
                            connectTimeout,
                            connectTimeout,
                            prefix,
                            organization,
                            bucket);
                    break;
                case 3:
                    LOG.info("Creating InfluxDB v3 sender for {}:{}, database {}", host, port, database);
                    sender = new InfluxDbV3Sender(
                            configuration.getProtocolOrDefault("http"),
                            host,
                            port,
                            auth,
                            TimeUnit.SECONDS,
                            connectTimeout,
                            connectTimeout,
                            prefix,
                            database);
                    break;
                default:
                    LOG.error("Unsupported InfluxDB version: {}", version);
            }
        } catch (final Exception ex) {
            LOG.error("Not able to start InfluxDB sender, please check your configuration: {}", ex.getMessage());
            LOG.debug("Original Exception: ", ex);
        }
        return sender;
    }

    private @Nullable InfluxDbSender setupV1Sender(
            final @NotNull InfluxDbConfiguration configuration,
            final @NotNull String mode,
            final @Nullable String host,
            final @Nullable Integer port,
            final @NotNull String database,
            final @Nullable String auth,
            final int connectTimeout,
            final @NotNull String prefix) throws Exception {
        switch (mode) {
            case "http":
                LOG.info("Creating InfluxDB v1 HTTP sender for {}:{}, database {}", host, port, database);
                return new InfluxDbHttpSender(configuration.getProtocolOrDefault("http"),
                        host,
                        port,
                        database,
                        auth,
                        TimeUnit.SECONDS,
                        connectTimeout,
                        connectTimeout,
                        prefix);
            case "tcp":
                LOG.info("Creating InfluxDB v1 TCP sender for {}:{}, database {}", host, port, database);
                return new InfluxDbTcpSender(host, port, connectTimeout, database, prefix);
            case "udp":
                LOG.info("Creating InfluxDB v1 UDP sender for {}:{}, database {}", host, port, database);
                return new InfluxDbUdpSender(host, port, connectTimeout, database, prefix);
            case "cloud":
                LOG.warn("mode=cloud with version=1 is unusual. Falling back to HTTP sender.");
                return new InfluxDbHttpSender(configuration.getProtocolOrDefault("https"),
                        host,
                        port,
                        database,
                        auth,
                        TimeUnit.SECONDS,
                        connectTimeout,
                        connectTimeout,
                        prefix);
            default:
                LOG.error("Unsupported mode '{}' for InfluxDB v1. Supported modes: http, tcp, udp", mode);
                return null;
        }
    }

    public static Set<String> newHashSet(final String @NotNull ... elements) {
        final var set = new HashSet<String>();
        Collections.addAll(set, elements);
        return set;
    }
}
