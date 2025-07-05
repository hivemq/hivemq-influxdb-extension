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
import io.github.sgtsilvio.gradle.oci.junit.jupiter.OciImages;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import static org.awaitility.Awaitility.await;
import static org.influxdb.querybuilder.BuiltQuery.QueryBuilder.select;

@SuppressWarnings({"resource"})
@Testcontainers
public class InfluxDbExtensionIT {

    private static final @NotNull String INFLUXDB_NAME = "hivemq";

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(InfluxDbExtensionIT.class);

    private final @NotNull Network network = Network.newNetwork();

    @Container
    private final @NotNull HiveMQContainer hivemq =
            new HiveMQContainer(OciImages.getImageName("hivemq/extensions/hivemq-influxdb-extension")
                    .asCompatibleSubstituteFor("hivemq/hivemq-ce")) //
                    .withNetwork(network)
                    .withCopyToContainer(MountableFile.forClasspathResource("influxdb.properties"),
                            "/opt/hivemq/extensions/hivemq-influxdb-extension/influxdb.properties")
                    .withLogConsumer(outputFrame -> System.out.print("HIVEMQ: " + outputFrame.getUtf8String()));

    @Container
    private final @NotNull InfluxDBContainer<?> influxDB =
            new InfluxDBContainer<>(OciImages.getImageName("influxdb")).withAuthEnabled(false)
                    .withNetwork(network)
                    .withNetworkAliases("influxdb");

    @Test
    void testMetricsAreForwardedToInfluxDB() {
        final var influxDbClient = influxDB.getNewInfluxDB();
        influxDbClient.setDatabase("hivemq");

        final var query = influxDbClient.query(new Query("CREATE DATABASE \"" + INFLUXDB_NAME + "\""));
        LOG.info("created database with query result: {}", query);
        influxDbClient.setDatabase(INFLUXDB_NAME);

        final var mqttClient =
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
        var acc = 0L;
        final var queryResult = client.query(select("count").from(INFLUXDB_NAME, metric));
        for (final var result : queryResult.getResults()) {
            final var series = result.getSeries();
            if (series == null) {
                break;
            }
            final var values = series.get(series.size() - 1).getValues();
            if (values == null) {
                break;
            }
            long max = 0;
            for (final var value : values) {
                if (value == null) {
                    break;
                }
                final var val = (double) value.get(1);
                if (max < val) {
                    max = (long) val;
                }
            }
            acc += max;
        }
        return acc;
    }
}
