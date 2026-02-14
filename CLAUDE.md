# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Build and run unit tests
./gradlew test               # Unit tests only
./gradlew integrationTest    # Integration tests (requires Docker)
./gradlew check              # All checks (tests + license headers)
./gradlew hivemqExtensionZip # Package the extension as a zip
```

Run a single test class:
```bash
./gradlew test --tests "com.hivemq.extensions.influxdb.configuration.InfluxDbConfigurationTest"
```

Run a single test method:
```bash
./gradlew test --tests "com.hivemq.extensions.influxdb.configuration.InfluxDbConfigurationTest.validateConfiguration_ok"
```

## Architecture

This is a HiveMQ extension that periodically forwards broker metrics to InfluxDB. It supports InfluxDB v1, v2, and v3.

### Data Flow

`InfluxDbExtensionMain` (entry point, implements HiveMQ `ExtensionMain`) orchestrates the lifecycle:
1. `ConfigResolver` resolves the config file path (supports legacy `influxdb.properties` location with deprecation warning, prefers `conf/config.properties`)
2. `InfluxDbConfiguration` (extends `PropertiesReader`) loads, validates, and exposes all properties
3. A version-specific sender is created based on the `version` property
4. An `InfluxDbReporter` (from iZettle metrics-influxdb library) is started on a schedule, sending HiveMQ metrics through the sender

### Sender Implementations

| Version | Class | Endpoint | Auth |
|---------|-------|----------|------|
| v1 | `InfluxDbHttpSender` / `TcpSender` / `UdpSender` (library) | `/write` | Optional |
| v2 | `InfluxDbCloudSender` (custom) | `/api/v2/write` | Required, `Token` prefix, `@NotNull` |
| v3 | `InfluxDbV3Sender` (custom) | `/api/v3/write_lp` | Optional, `Bearer` prefix, `@Nullable` |

Both custom senders extend `InfluxDbHttpSender` from the iZettle library and override `writeData()` with GZIP compression and version-specific URL construction.

### Configuration Validation

`validateConfiguration()` in `InfluxDbConfiguration` is the gate — if it returns `false`, `extensionStart()` calls `preventExtensionStartup()` and the extension does not start. It validates: mandatory host/port, port range, version range (1-3), and cloud-mode requirements (auth, bucket, organization).

## Code Conventions

- Java 11 source compatibility (compiled with Java 11 compiler), Java 21 toolchain
- Uses `@NotNull` / `@Nullable` annotations from JetBrains for nullability contracts
- Uses `var` (local variable type inference) throughout
- All source files require the Apache 2.0 license header (enforced by `./gradlew check`)
- Formatting: 4 spaces, 120 char line length, LF line endings (see `.editorconfig`)

## Test Structure

- **Unit tests** (`src/test/`): JUnit 5, Mockito, WireMock, AssertJ
- **Integration tests** (`src/integrationTest/`): TestContainers with real HiveMQ + InfluxDB containers, require Docker
- Config files for integration tests: `src/integrationTest/resources/config-v{1,2,3}.properties`
