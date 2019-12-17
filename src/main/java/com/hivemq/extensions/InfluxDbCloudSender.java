package com.hivemq.extensions;

import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.utils.TimeUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class InfluxDbCloudSender extends InfluxDbHttpSender {
    private final String authToken;
    private final int connectTimeout;
    private final int readTimeout;
    private final URL url;

    public InfluxDbCloudSender(String protocol, String cloudUrl, int port, String database,
                               String authString, TimeUnit timePrecision,
                               int connectTimeout, int readTimeout, String measurementPrefix,
                               final String organization, final String bucket) throws Exception {
        super(protocol, cloudUrl, port, database, authString, timePrecision, connectTimeout, readTimeout, measurementPrefix);
        this.authToken = authString;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;

        final String endpoint = new URL(protocol, cloudUrl, port, "/write").toString();
        final String queryDb = String.format("db=%s", URLEncoder.encode(database, StandardCharsets.UTF_8));
        final String queryPrecision = String.format("precision=%s", TimeUtils.toTimePrecision(timePrecision));
        final String orgParameter = String.format("org=%s", URLEncoder.encode(organization, StandardCharsets.UTF_8));
        final String bucketParameter = String.format("bucket=%s", URLEncoder.encode(bucket, StandardCharsets.UTF_8));
        this.url = new URL(endpoint + "?" + queryDb + "&" + queryPrecision + "&" + orgParameter + "&" + bucketParameter);
    }

    @Override
    protected int writeData(byte[] line) throws Exception {
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Token " + authToken);
        con.setDoOutput(true);
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);

        OutputStream out = con.getOutputStream();
        try {
            out.write(line);
            out.flush();
        } finally {
            out.close();
        }

        int responseCode = con.getResponseCode();

        // Check if non 2XX response code.
        if (responseCode / 100 != 2) {
            throw new IOException(
                    "Server returned HTTP response code: " + responseCode + " for URL: " + url + " with content :'"
                            + con.getResponseMessage() + "'");
        }
        return responseCode;
    }
}
