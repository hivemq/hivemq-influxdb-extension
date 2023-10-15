plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
    alias(libs.plugins.asciidoctor)
}

group = "com.hivemq.extensions"
description = "HiveMQ extension for transferring monitoring data to the time series database InfluxDB"

hivemqExtension {
    name.set("InfluxDB Monitoring Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(1000)
    mainClass.set("$group.influxdb.InfluxDbExtensionMain")
    sdkVersion.set(libs.versions.hivemq.extensionSdk)

    resources {
        from("LICENSE")
        from("README.adoc") { rename { "README.txt" } }
        from(tasks.asciidoctor)
    }
}

dependencies {
    implementation(libs.metrics.influxdb)
    implementation(libs.commonsLang)
}

tasks.asciidoctor {
    sourceDirProperty.set(layout.projectDirectory)
    sources("README.adoc")
    secondarySources { exclude("**") }
}

/* ******************** test ******************** */

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito)
    testImplementation(libs.wiremock.jre8)
    testRuntimeOnly(libs.logback.classic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

/* ******************** integration test ******************** */

dependencies {
    integrationTestCompileOnly(libs.jetbrains.annotations)
    integrationTestImplementation(libs.assertj)
    integrationTestImplementation(libs.awaitility)
    integrationTestImplementation(libs.hivemq.mqttClient)
    integrationTestImplementation(libs.okhttp)
    integrationTestImplementation(platform(libs.testcontainers.bom))
    integrationTestImplementation(libs.testcontainers.junitJupiter)
    integrationTestImplementation(libs.testcontainers.influxdb)
    integrationTestImplementation(libs.testcontainers.hivemq)
    integrationTestImplementation(libs.influxdb)
    integrationTestRuntimeOnly(libs.logback.classic)
}

/* ******************** checks ******************** */

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("**/template-s3discovery.properties")
    exclude("**/logback-test.xml")
}
