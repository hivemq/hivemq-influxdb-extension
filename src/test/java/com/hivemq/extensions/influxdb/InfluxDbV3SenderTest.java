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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
class InfluxDbV3SenderTest {

    @Test
    void test_write_data(final @NotNull WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        final var sender = new InfluxDbV3Sender("http",
                "localhost",
                wireMockRuntimeInfo.getHttpPort(),
                "mytoken",
                TimeUnit.SECONDS,
                3000,
                3000,
                "",
                "testdb");

        stubFor(post(urlPathEqualTo("/api/v3/write_lp")).willReturn(aResponse().withStatus(200).withBody("")));

        sender.writeData("measurement,tag=value field=1".getBytes());

        verify(postRequestedFor(urlEqualTo("/api/v3/write_lp?precision=s&db=testdb"))
                .withHeader("Authorization", equalTo("Bearer mytoken"))
                .withRequestBody(equalTo("measurement,tag=value field=1")));
    }

    @Test
    void test_write_data_milliseconds_precision(final @NotNull WireMockRuntimeInfo wireMockRuntimeInfo)
            throws Exception {
        final var sender = new InfluxDbV3Sender("http",
                "localhost",
                wireMockRuntimeInfo.getHttpPort(),
                "mytoken",
                TimeUnit.MILLISECONDS,
                3000,
                3000,
                "",
                "testdb");

        stubFor(post(urlPathEqualTo("/api/v3/write_lp")).willReturn(aResponse().withStatus(200).withBody("")));

        sender.writeData("line=line".getBytes());

        verify(postRequestedFor(urlEqualTo("/api/v3/write_lp?precision=ms&db=testdb")));
    }

    @Test
    void test_write_data_without_auth_token(final @NotNull WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        final var sender = new InfluxDbV3Sender("http",
                "localhost",
                wireMockRuntimeInfo.getHttpPort(),
                null,
                TimeUnit.SECONDS,
                3000,
                3000,
                "",
                "testdb");

        stubFor(post(urlPathEqualTo("/api/v3/write_lp")).willReturn(aResponse().withStatus(200).withBody("")));

        sender.writeData("line=line".getBytes());

        verify(postRequestedFor(urlEqualTo("/api/v3/write_lp?precision=s&db=testdb"))
                .withHeader("Authorization", absent()));
    }

    @Test
    void test_write_data_with_empty_auth_token(final @NotNull WireMockRuntimeInfo wireMockRuntimeInfo)
            throws Exception {
        final var sender = new InfluxDbV3Sender("http",
                "localhost",
                wireMockRuntimeInfo.getHttpPort(),
                "",
                TimeUnit.SECONDS,
                3000,
                3000,
                "",
                "testdb");

        stubFor(post(urlPathEqualTo("/api/v3/write_lp")).willReturn(aResponse().withStatus(200).withBody("")));

        sender.writeData("line=line".getBytes());

        verify(postRequestedFor(urlEqualTo("/api/v3/write_lp?precision=s&db=testdb"))
                .withHeader("Authorization", absent()));
    }

    @Test
    void test_write_data_gzip_encoding(final @NotNull WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        final var sender = new InfluxDbV3Sender("http",
                "localhost",
                wireMockRuntimeInfo.getHttpPort(),
                "mytoken",
                TimeUnit.SECONDS,
                3000,
                3000,
                "",
                "testdb");

        stubFor(post(urlPathEqualTo("/api/v3/write_lp")).willReturn(aResponse().withStatus(200).withBody("")));

        sender.writeData("line=line".getBytes());

        verify(postRequestedFor(urlPathEqualTo("/api/v3/write_lp"))
                .withHeader("Content-Encoding", equalTo("gzip")));
    }

    @Test
    void test_write_data_database_url_encoded(final @NotNull WireMockRuntimeInfo wireMockRuntimeInfo)
            throws Exception {
        final var sender = new InfluxDbV3Sender("http",
                "localhost",
                wireMockRuntimeInfo.getHttpPort(),
                "mytoken",
                TimeUnit.SECONDS,
                3000,
                3000,
                "",
                "my database");

        stubFor(post(urlPathEqualTo("/api/v3/write_lp")).willReturn(aResponse().withStatus(200).withBody("")));

        sender.writeData("line=line".getBytes());

        verify(postRequestedFor(urlEqualTo("/api/v3/write_lp?precision=s&db=my+database")));
    }

    @Test
    void test_write_data_server_error(final @NotNull WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        final var sender = new InfluxDbV3Sender("http",
                "localhost",
                wireMockRuntimeInfo.getHttpPort(),
                "mytoken",
                TimeUnit.SECONDS,
                3000,
                3000,
                "",
                "testdb");

        stubFor(post(urlPathEqualTo("/api/v3/write_lp")).willReturn(aResponse().withStatus(500)
                .withBody("Internal Server Error")));

        assertThrows(IOException.class, () -> sender.writeData("line=line".getBytes()));
    }

    @Test
    void test_write_data_unauthorized(final @NotNull WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        final var sender = new InfluxDbV3Sender("http",
                "localhost",
                wireMockRuntimeInfo.getHttpPort(),
                "badtoken",
                TimeUnit.SECONDS,
                3000,
                3000,
                "",
                "testdb");

        stubFor(post(urlPathEqualTo("/api/v3/write_lp")).willReturn(aResponse().withStatus(401)
                .withBody("Unauthorized")));

        assertThrows(IOException.class, () -> sender.writeData("line=line".getBytes()));
    }
}
