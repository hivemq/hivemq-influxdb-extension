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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.awaitility.Awaitility.await;

@Testcontainers
@SuppressWarnings({"resource", "SameParameterValue", "HttpUrlsUsage"})
class InfluxDb3ExtensionIT {

    private static final int INFLUXDB_PORT = 8181;
    private static final @NotNull String INFLUXDB_DATABASE = "hivemq";

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(InfluxDb3ExtensionIT.class);

    private final @NotNull Network network = Network.newNetwork();

    @Container
    private final @NotNull HiveMQContainer hivemq =
            new HiveMQContainer(OciImages.getImageName("hivemq/extensions/hivemq-influxdb-extension")
                    .asCompatibleSubstituteFor("hivemq/hivemq-ce")) //
                    .withNetwork(network)
                    .withCopyToContainer(MountableFile.forClasspathResource("config-v3.properties"),
                            "/opt/hivemq/extensions/hivemq-influxdb-extension/conf/config.properties")
                    .withLogConsumer(outputFrame -> LOG.info("HIVEMQ: {}", outputFrame.getUtf8String()))
                    .withEnv("HIVEMQ_DISABLE_STATISTICS", "true");

    @Container
    private final @NotNull GenericContainer<?> influxDB =
            new GenericContainer<>(OciImages.getImageName("influxdb:v3")) //
                    .withCommand("serve", "--node-id", "test", "--object-store", "memory", "--without-auth")
                    .withExposedPorts(INFLUXDB_PORT)
                    .waitingFor(Wait.forHttp("/health").forPort(INFLUXDB_PORT))
                    .withNetwork(network)
                    .withNetworkAliases("influxdb")
                    .withLogConsumer(outputFrame -> LOG.info("INFLUXDB: {}", outputFrame.getUtf8String()));

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void testMetricsAreForwardedToInfluxDB() {
        final var mqttClient =
                Mqtt5Client.builder().serverHost(hivemq.getHost()).serverPort(hivemq.getMqttPort()).buildBlocking();
        mqttClient.connect();
        mqttClient.publishWith().topic("my/topic1").send();
        mqttClient.publishWith().topic("my/topic2").send();
        mqttClient.publishWith().topic("my/topic3").send();
        mqttClient.disconnect();

        final var host = influxDB.getHost();
        final var port = influxDB.getMappedPort(INFLUXDB_PORT);
        await().until(() -> getMetricMax(host, port, "com.hivemq.messages.incoming.publish.count") == 3);
        await().until(() -> getMetricMax(host, port, "com.hivemq.messages.incoming.connect.count") == 1);
    }

    private static long getMetricMax(final @NotNull String host, final int port, final @NotNull String metric) {
        try {
            final var sql = "SELECT max(\"count\") AS max_val FROM \"%s\"".formatted(metric);
            final var uri = URI.create("http://%s:%d/api/v3/query_sql?db=%s&format=csv&q=%s".formatted(host,
                    port,
                    URLEncoder.encode(INFLUXDB_DATABASE, StandardCharsets.UTF_8),
                    URLEncoder.encode(sql, StandardCharsets.UTF_8)));
            final var request = HttpRequest.newBuilder().uri(uri).GET().build();
            final var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.error("Failed to get metric {}: status code {}\n{}",
                        metric,
                        response.statusCode(),
                        response.body());
                return 0;
            }
            final var lines = response.body().strip().split("\n");
            if (lines.length < 2) {
                LOG.error("Failed to get metric {}: Unexpected response body format\n{}", metric, response.body());
                return 0;
            }
            return (long) Double.parseDouble(lines[1].strip());
        } catch (final Exception e) {
            LOG.error("Failed to get metric {}: {}", metric, e.getMessage());
            return 0;
        }
    }
}
