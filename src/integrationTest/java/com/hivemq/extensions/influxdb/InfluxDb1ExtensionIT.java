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

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.domain.InfluxQLQuery;
import io.github.sgtsilvio.gradle.oci.junit.jupiter.OciImages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import static org.awaitility.Awaitility.await;

@Testcontainers
@SuppressWarnings({"resource", "SameParameterValue"})
class InfluxDb1ExtensionIT {

    private static final @NotNull String INFLUXDB_DATABASE = "hivemq";

    private final @NotNull Network network = Network.newNetwork();

    @Container
    private final @NotNull HiveMQContainer hivemq =
            new HiveMQContainer(OciImages.getImageName("hivemq/extensions/hivemq-influxdb-extension")
                    .asCompatibleSubstituteFor("hivemq/hivemq-ce")) //
                    .withNetwork(network)
                    .withCopyToContainer(MountableFile.forClasspathResource("config.properties"),
                            "/opt/hivemq/extensions/hivemq-influxdb-extension/conf/config.properties")
                    .withLogConsumer(outputFrame -> System.out.print("HIVEMQ: " + outputFrame.getUtf8String()))
                    .withEnv("HIVEMQ_DISABLE_STATISTICS", "true");

    @Container
    private final @NotNull InfluxDBContainer<?> influxDB =
            new InfluxDBContainer<>(OciImages.getImageName("influxdb:v1")).withAuthEnabled(false)
                    .withNetwork(network)
                    .withNetworkAliases("influxdb")
                    .withLogConsumer(outputFrame -> System.out.print("INFLUXDB: " + outputFrame.getUtf8String()));

    @Test
    void testMetricsAreForwardedToInfluxDB() {
        try (final var influxDBClient = InfluxDBClientFactory.create(influxDB.getUrl())) {
            final var createDbQuery = new InfluxQLQuery("CREATE DATABASE \"%s\"".formatted(INFLUXDB_DATABASE), "");
            influxDBClient.getInfluxQLQueryApi().query(createDbQuery);

            final var mqttClient =
                    Mqtt5Client.builder().serverHost(hivemq.getHost()).serverPort(hivemq.getMqttPort()).buildBlocking();
            mqttClient.connect();
            mqttClient.publishWith().topic("my/topic1").send();
            mqttClient.publishWith().topic("my/topic2").send();
            mqttClient.publishWith().topic("my/topic3").send();
            mqttClient.disconnect();

            await().until(() -> getMetricMax(influxDBClient, "com.hivemq.messages.incoming.publish.count") == 3);
            await().until(() -> getMetricMax(influxDBClient, "com.hivemq.messages.incoming.connect.count") == 1);
        }
    }

    @Test
    void configAtLegacyLocation_metricsAreForwardedToInfluxDB() {
        final var legacyHivemq =
                new HiveMQContainer(OciImages.getImageName("hivemq/extensions/hivemq-influxdb-extension")
                        .asCompatibleSubstituteFor("hivemq/hivemq-ce")) //
                        .withNetwork(network)
                        .withCopyToContainer(MountableFile.forClasspathResource("config.properties"),
                                "/opt/hivemq/extensions/hivemq-influxdb-extension/influxdb.properties")
                        .withLogConsumer(outputFrame -> System.out.print("HIVEMQ: " + outputFrame.getUtf8String()))
                        .withEnv("HIVEMQ_DISABLE_STATISTICS", "true");

        try (legacyHivemq) {
            legacyHivemq.start();
            try (final var influxDBClient = InfluxDBClientFactory.create(influxDB.getUrl())) {
                final var createDbQuery = new InfluxQLQuery("CREATE DATABASE \"%s\"".formatted(INFLUXDB_DATABASE), "");
                influxDBClient.getInfluxQLQueryApi().query(createDbQuery);

                final var mqttClient = Mqtt5Client.builder()
                        .serverHost(legacyHivemq.getHost())
                        .serverPort(legacyHivemq.getMqttPort())
                        .buildBlocking();
                mqttClient.connect();
                mqttClient.publishWith().topic("my/topic1").send();
                mqttClient.disconnect();

                await().until(() -> getMetricMax(influxDBClient, "com.hivemq.messages.incoming.publish.count") == 1);
            }
        }
    }

    private long getMetricMax(final @NotNull InfluxDBClient client, final @NotNull String metric) {
        final var influxQL = String.format("SELECT MAX(count) FROM \"%s\"", metric);
        final var query = new InfluxQLQuery(influxQL, INFLUXDB_DATABASE);
        final var result = client.getInfluxQLQueryApi().query(query);
        long max = 0;
        for (final var queryResult : result.getResults()) {
            for (final var series : queryResult.getSeries()) {
                for (final var record : series.getValues()) {
                    final var value = getValue(record.getValueByKey("max"));
                    if (value > max) {
                        max = value;
                    }
                }
            }
        }
        return max;
    }

    private static long getValue(final @Nullable Object valueField) {
        if (valueField instanceof Number) {
            return ((Number) valueField).longValue();
        } else if (valueField != null) {
            try {
                // try to parse as double if it's a string
                return (long) Double.parseDouble(valueField.toString());
            } catch (final NumberFormatException ignored) {
            }
        }
        return Long.MIN_VALUE;
    }
}
