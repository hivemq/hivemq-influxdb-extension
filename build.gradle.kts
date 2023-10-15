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

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
        "test"(JvmTestSuite::class) {
            dependencies {
                implementation(libs.mockito)
                implementation(libs.wiremock.jre8)
                runtimeOnly(libs.logback.classic)
            }
        }
        "integrationTest"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.assertj)
                implementation(libs.awaitility)
                implementation(libs.hivemq.mqttClient)
                implementation(libs.okhttp)
                implementation(platform(libs.testcontainers.bom))
                implementation(libs.testcontainers.junitJupiter)
                implementation(libs.testcontainers.hivemq)
                implementation(libs.testcontainers.influxdb)
                implementation(libs.influxdb)
                runtimeOnly(libs.logback.classic)
            }
        }
    }
}

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("**/template-s3discovery.properties")
    exclude("**/logback-test.xml")
}
