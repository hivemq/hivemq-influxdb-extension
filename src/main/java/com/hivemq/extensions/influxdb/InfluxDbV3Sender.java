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

import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.utils.TimeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Sender for InfluxDB v3 using the {@code /api/v3/write_lp} endpoint.
 * <p>
 * Supports InfluxDB 3 Core, Enterprise, and Cloud.
 * Follows the same pattern as {@link InfluxDbCloudSender}.
 */
public class InfluxDbV3Sender extends InfluxDbHttpSender {

    private final @Nullable String authToken;
    private final int connectTimeout;
    private final int readTimeout;
    private final @NotNull URL url;

    public InfluxDbV3Sender(
            final @NotNull String protocol,
            final @NotNull String host,
            final int port,
            final @Nullable String authToken,
            final @NotNull TimeUnit timePrecision,
            final int connectTimeout,
            final int readTimeout,
            final @NotNull String measurementPrefix,
            final @NotNull String database) throws Exception {
        super(protocol,
                host,
                port,
                database,
                authToken != null ? authToken : "",
                timePrecision,
                connectTimeout,
                readTimeout,
                measurementPrefix);
        this.authToken = authToken;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        final var endpoint = new URL(protocol, host, port, "/api/v3/write_lp").toString();
        final var queryPrecision = String.format("precision=%s", TimeUtils.toTimePrecision(timePrecision));
        final var dbParameter = String.format("db=%s", URLEncoder.encode(database, StandardCharsets.UTF_8));
        this.url = new URL(endpoint + "?" + queryPrecision + "&" + dbParameter);
    }

    @Override
    protected int writeData(final byte @NotNull [] line) throws Exception {
        final var con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        if (authToken != null && !authToken.isEmpty()) {
            con.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        con.setDoOutput(true);
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);
        con.setRequestProperty("Content-Encoding", "gzip");
        try (final var out = con.getOutputStream(); final var gzipOutputStream = new GZIPOutputStream(out)) {
            gzipOutputStream.write(line);
            gzipOutputStream.flush();
            out.flush();
        }
        final var responseCode = con.getResponseCode();
        if (responseCode / 100 != 2) {
            throw new IOException("Server returned HTTP response code: " +
                    responseCode +
                    " for URL: " +
                    url +
                    " with content :'" +
                    con.getResponseMessage() +
                    "'");
        }
        return responseCode;
    }
}
