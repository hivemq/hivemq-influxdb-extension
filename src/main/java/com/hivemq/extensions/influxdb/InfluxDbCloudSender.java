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

import java.io.IOException;
import java.io.OutputStream;
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
    private final String authToken;
    private final int connectTimeout;
    private final int readTimeout;
    private final URL url;

    public InfluxDbCloudSender(
            String protocol,
            String host,
            int port,
            String authToken,
            TimeUnit timePrecision,
            int connectTimeout,
            int readTimeout,
            String measurementPrefix,
            final String organization,
            final String bucket) throws Exception {
        super(protocol, host, port, "", authToken, timePrecision, connectTimeout, readTimeout, measurementPrefix);
        this.authToken = authToken;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;

        final String endpoint = new URL(protocol, host, port, "/api/v2/write").toString();
        final String queryPrecision = String.format("precision=%s", TimeUtils.toTimePrecision(timePrecision));
        final String orgParameter = String.format("org=%s", URLEncoder.encode(organization, StandardCharsets.UTF_8));
        final String bucketParameter = String.format("bucket=%s", URLEncoder.encode(bucket, StandardCharsets.UTF_8));
        this.url = new URL(endpoint + "?" + queryPrecision + "&" + orgParameter + "&" + bucketParameter);
    }

    @Override
    protected int writeData(byte[] line) throws Exception {
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Token " + authToken);
        con.setDoOutput(true);
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);
        con.setRequestProperty("Content-Encoding", "gzip");

        try (OutputStream out = con.getOutputStream();
             final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out)) {
            gzipOutputStream.write(line);
            gzipOutputStream.flush();
            out.flush();
        }

        int responseCode = con.getResponseCode();

        // Check if non 2XX response code.
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
