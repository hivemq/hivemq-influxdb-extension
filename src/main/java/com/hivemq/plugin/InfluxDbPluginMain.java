package com.hivemq.plugin;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.google.common.collect.Sets;
import com.hivemq.configuration.InfluxDbConfiguration;
import com.hivemq.plugin.api.PluginMain;
import com.hivemq.plugin.api.annotations.NotNull;
import com.hivemq.plugin.api.annotations.Nullable;
import com.hivemq.plugin.api.parameter.PluginStartInput;
import com.hivemq.plugin.api.parameter.PluginStartOutput;
import com.hivemq.plugin.api.parameter.PluginStopInput;
import com.hivemq.plugin.api.parameter.PluginStopOutput;
import com.hivemq.plugin.api.services.Services;
import com.izettle.metrics.influxdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michael Walter
 */
public class InfluxDbPluginMain implements PluginMain {

    private static final Logger log = LoggerFactory.getLogger(InfluxDbPluginMain.class);
    private static final HashSet<String> METER_FIELDS = Sets.newHashSet("count", "m1_rate", "m5_rate", "m15_rate", "mean_rate");
    private static final HashSet<String> TIMER_FIELDS = Sets.newHashSet("count", "min", "max", "mean", "stddev", "p50", "p75", "p95", "p98", "p99", "p999", "m1_rate", "m5_rate", "m15_rate", "mean_rate");

    private ScheduledReporter reporter;

    @Override
    public void pluginStart(@NotNull final PluginStartInput pluginStartInput, @NotNull final PluginStartOutput pluginStartOutput) {

        final InfluxDbConfiguration configuration = new InfluxDbConfiguration(pluginStartInput.getPluginInformation().getPluginHomeFolder());

        if (!configuration.readPropertiesFromFile()) {
            pluginStartOutput.preventPluginStartup("Could not read influxdb properties.");
            return;
        }

        if (!configuration.validateConfiguration()) {
            pluginStartOutput.preventPluginStartup("At least one mandatory property not set.");
            return;
        }

        final MetricRegistry metricRegistry = Services.metricRegistry();
        final InfluxDbSender sender = setupSender(configuration);

        reporter = setupReporter(metricRegistry, sender, configuration);
        reporter.start(configuration.getReportingInterval(), TimeUnit.SECONDS);
    }

    @Override
    public void pluginStop(@NotNull final PluginStopInput pluginStopInput, @NotNull final PluginStopOutput pluginStopOutput) {
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

            }
        } catch (Exception ex) {
            log.error("Not able to start InfluxDB sender, please check your configuration: {}", ex.getMessage());
            log.debug("Original Exception: ", ex);
        }

        return sender;
    }

}
