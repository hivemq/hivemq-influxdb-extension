package com.hivemq.extensions;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author Christoph Schäbel
 */
public class InfluxDbCloudSenderTest {

    private InfluxDbCloudSender sender;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    @Test
    public void test_write_data() throws Exception {

        sender = new InfluxDbCloudSender("http", "localhost", wireMockRule.port(), "token", TimeUnit.MILLISECONDS, 3000, 3000, "", "testorg", "testbucket");

        wireMockRule.stubFor(post(urlPathEqualTo("/api/v2/write"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        sender.writeData("line=line".getBytes());

        verify(postRequestedFor(urlEqualTo("/api/v2/write?precision=ms&org=testorg&bucket=testbucket"))
                .withHeader("Authorization", equalTo("Token token"))
                .withRequestBody(equalTo("line=line")));
    }

}
