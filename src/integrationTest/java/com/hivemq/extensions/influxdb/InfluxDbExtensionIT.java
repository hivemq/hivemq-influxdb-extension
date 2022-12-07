package com.hivemq.extensions.influxdb;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.util.List;

import static com.hivemq.extensions.influxdb.DockerImageNames.HIVEMQ_IMAGE;
import static com.hivemq.extensions.influxdb.DockerImageNames.INFLUXDB_IMAGE;
import static org.awaitility.Awaitility.await;
import static org.influxdb.querybuilder.BuiltQuery.QueryBuilder.select;

@SuppressWarnings({"resource"})
@Testcontainers
public class InfluxDbExtensionIT {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(InfluxDbExtensionIT.class);

    private static final @NotNull String INFLUXDB_NAME = "hivemq";

    private final @NotNull Network network = Network.newNetwork();

    @Container
    private final @NotNull HiveMQContainer hivemq = new HiveMQContainer(HIVEMQ_IMAGE) //
            .withExtension(MountableFile.forClasspathResource("hivemq-influxdb-extension"))
            .waitForExtension("InfluxDB Monitoring Extension")
            .withNetwork(network)
            .withFileInExtensionHomeFolder(MountableFile.forClasspathResource("influxdb.properties"),
                    "hivemq-influxdb-extension",
                    "influxdb.properties")
            .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()));

    @Container
    private final @NotNull InfluxDBContainer<?> influxDB = new InfluxDBContainer<>(INFLUXDB_IMAGE) //
            .withAuthEnabled(false).withNetwork(network).withNetworkAliases("influxdb");

    @Test
    void testMetricsAreForwardedToInfluxDB() {
        final InfluxDB influxDbClient = influxDB.getNewInfluxDB();
        influxDbClient.setDatabase("hivemq");

        final QueryResult query = influxDbClient.query(new Query("CREATE DATABASE \"" + INFLUXDB_NAME + "\""));
        LOG.info("created database with query result: {}", query);
        influxDbClient.setDatabase(INFLUXDB_NAME);

        final Mqtt5BlockingClient mqttClient =
                Mqtt5Client.builder().serverHost(hivemq.getHost()).serverPort(hivemq.getMqttPort()).buildBlocking();
        mqttClient.connect();
        mqttClient.publishWith().topic("my/topic1").send();
        mqttClient.publishWith().topic("my/topic2").send();
        mqttClient.publishWith().topic("my/topic3").send();
        mqttClient.disconnect();

        await().until(() -> getMetricMax(influxDbClient, "com.hivemq.messages.incoming.publish.count") == 3);
        await().until(() -> getMetricMax(influxDbClient, "com.hivemq.messages.incoming.connect.count") == 1);
    }

    private long getMetricMax(final @NotNull InfluxDB client, final @NotNull String metric) {
        long acc = 0;
        final QueryResult queryResult = client.query(select("count").from(INFLUXDB_NAME, metric));
        for (final QueryResult.Result result : queryResult.getResults()) {
            final List<QueryResult.Series> series = result.getSeries();
            if (series == null) {
                break;
            }
            final List<List<Object>> values = series.get(series.size() - 1).getValues();
            if (values == null) {
                break;
            }
            long max = 0;
            for (final List<Object> value : values) {
                if (value == null) {
                    break;
                }
                final double val = (double) value.get(1);
                if (max < val) {
                    max = (long) val;
                }
            }
            acc += max;
        }
        return acc;
    }
}
