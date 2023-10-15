plugins {
    id("com.hivemq.extension")
    id("com.github.hierynomus.license")
    id("io.github.sgtsilvio.gradle.defaults")
    id("org.asciidoctor.jvm.convert")
}

group = "com.hivemq.extensions"
description = "HiveMQ extension for transferring monitoring data to the time series database InfluxDB"

hivemqExtension {
    name.set("InfluxDB Monitoring Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(1000)
    mainClass.set("$group.influxdb.InfluxDbExtensionMain")
    sdkVersion.set("${property("hivemq-extension-sdk.version")}")

    resources {
        from("LICENSE")
        from("README.adoc") { rename { "README.txt" } }
        from(tasks.asciidoctor)
    }
}

dependencies {
    implementation("com.izettle:metrics-influxdb:${property("metrics-influxdb.version")}")
    implementation("org.apache.commons:commons-lang3:${property("commons-lang3.version")}")
}

tasks.asciidoctor {
    sourceDirProperty.set(layout.projectDirectory)
    sources("README.adoc")
    secondarySources { exclude("**") }
}

/* ******************** test ******************** */

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
    testImplementation("com.github.tomakehurst:wiremock-jre8:${property("wiremock.version")}")

    testRuntimeOnly("ch.qos.logback:logback-classic:${property("logback.version")}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

/* ******************** integration test ******************** */

dependencies {
    integrationTestImplementation(platform("org.testcontainers:testcontainers-bom:${property("testcontainers.version")}"))

    integrationTestCompileOnly("org.jetbrains:annotations:${property("jetbrains-annotations.version")}")
    integrationTestImplementation("org.assertj:assertj-core:${property("assertj.version")}")
    integrationTestImplementation("org.awaitility:awaitility:${property("awaitility.version")}")
    integrationTestImplementation("com.hivemq:hivemq-mqtt-client:${property("hivemq-mqtt-client.version")}")
    integrationTestImplementation("com.squareup.okhttp3:okhttp:${property("okhttp.version")}")
    integrationTestImplementation("org.testcontainers:junit-jupiter")
    integrationTestImplementation("org.testcontainers:influxdb")
    integrationTestImplementation("org.testcontainers:hivemq")
    integrationTestImplementation("org.influxdb:influxdb-java:${property("influxdb.version")}")
    integrationTestRuntimeOnly("ch.qos.logback:logback-classic:${property("logback.version")}")
}

/* ******************** checks ******************** */

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("**/template-s3discovery.properties")
    exclude("**/logback-test.xml")
}
