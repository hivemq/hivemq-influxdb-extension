package com.hivemq.extensions.influxdb;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author Christoph Schäbel
 */
class InfluxDbCloudSenderTest {

    @RegisterExtension
    final @NotNull WireMockExtension wireMockExtension = new WireMockExtension();

    @Test
    void test_write_data() throws Exception {
        final InfluxDbCloudSender sender = new InfluxDbCloudSender("http", "localhost", wireMockExtension.getRuntimeInfo().getHttpPort(), "token", TimeUnit.MILLISECONDS, 3000, 3000, "", "testorg", "testbucket");

        wireMockExtension.stubFor(post(urlPathEqualTo("/api/v2/write"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        sender.writeData("line=line".getBytes());

        verify(postRequestedFor(urlEqualTo("/api/v2/write?precision=ms&org=testorg&bucket=testbucket"))
                .withHeader("Authorization", equalTo("Token token"))
                .withRequestBody(equalTo("line=line")));
    }

}
