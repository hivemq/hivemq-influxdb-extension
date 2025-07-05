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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Sender for InfluxDB Cloud.
 *
 * @author Simon Baier
 */
public class InfluxDbCloudSender extends InfluxDbHttpSender {

    private final @NotNull String authToken;
    private final int connectTimeout;
    private final int readTimeout;
    private final @NotNull URL url;

    public InfluxDbCloudSender(
            final @NotNull String protocol,
            final @NotNull String host,
            final int port,
            final @NotNull String authToken,
            final @NotNull TimeUnit timePrecision,
            final int connectTimeout,
            final int readTimeout,
            final @NotNull String measurementPrefix,
            final @NotNull String organization,
            final @NotNull String bucket) throws Exception {
        super(protocol, host, port, "", authToken, timePrecision, connectTimeout, readTimeout, measurementPrefix);
        this.authToken = authToken;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        final var endpoint = new URL(protocol, host, port, "/api/v2/write").toString();
        final var queryPrecision = String.format("precision=%s", TimeUtils.toTimePrecision(timePrecision));
        final var orgParameter = String.format("org=%s", URLEncoder.encode(organization, StandardCharsets.UTF_8));
        final var bucketParameter = String.format("bucket=%s", URLEncoder.encode(bucket, StandardCharsets.UTF_8));
        this.url = new URL(endpoint + "?" + queryPrecision + "&" + orgParameter + "&" + bucketParameter);
    }

    @Override
    protected int writeData(final byte @NotNull [] line) throws Exception {
        final var con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Token " + authToken);
        con.setDoOutput(true);
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);
        con.setRequestProperty("Content-Encoding", "gzip");
        try (final var out = con.getOutputStream(); final var gzipOutputStream = new GZIPOutputStream(out)) {
            gzipOutputStream.write(line);
            gzipOutputStream.flush();
            out.flush();
        }
        // check if non 2XX response code
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
