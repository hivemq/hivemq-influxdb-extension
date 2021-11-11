package com.hivemq.extensions.influxdb;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class InfluxDbExtensionMainTest {

    private @NotNull InfluxDbExtensionMain main;
    private @NotNull ExtensionStartInput extensionStartInput;
    private @NotNull ExtensionStartOutput extensionStartOutput;
    private @NotNull Path file;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        main = new InfluxDbExtensionMain();

        extensionStartInput = mock(ExtensionStartInput.class);
        extensionStartOutput = mock(ExtensionStartOutput.class);
        final ExtensionInformation extensionInformation = mock(ExtensionInformation.class);
        when(extensionStartInput.getExtensionInformation()).thenReturn(extensionInformation);
        when(extensionStartInput.getExtensionInformation().getExtensionHomeFolder()).thenReturn(tempDir.toFile());

        file = tempDir.resolve("influxdb.properties");
    }

    @Test
    void extensionStart_whenNoConfigurationFile_thenPreventStartup() {
        main.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput).preventExtensionStartup(anyString());
    }

    @Test
    void extensionStart_whenConfigurationFileNotValid_thenPreventStartup() throws IOException {
        Files.write(file, List.of("host:localhost", "port:-3000"));

        main.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput).preventExtensionStartup(anyString());
    }
}