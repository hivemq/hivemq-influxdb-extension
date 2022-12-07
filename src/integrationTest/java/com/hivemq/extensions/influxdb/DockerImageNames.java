package com.hivemq.extensions.influxdb;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Yannick Weber
 */
public final class DockerImageNames {

    public static final @NotNull DockerImageName HIVEMQ_IMAGE =
            DockerImageName.parse("acidsepp/hivemq-ce").withTag("latest").asCompatibleSubstituteFor("hivemq/hivemq-ce");

    public static final @NotNull DockerImageName INFLUXDB_IMAGE = DockerImageName.parse("influxdb").withTag("1.4.3");

    public DockerImageNames() {
    }
}
