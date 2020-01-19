package com.hivemq.extensions;

import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InfluxDbExtensionMainTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    ExtensionStartInput extensionStartInput;

    @Mock
    ExtensionStartOutput extensionStartOutput;

    @Mock
    ExtensionInformation extensionInformation;

    private File root;

    private File file;

    @Before
    public void set_up() throws IOException {
        MockitoAnnotations.initMocks(this);

        root = folder.getRoot();
        String fileName = "influxdb.properties";
        file = folder.newFile(fileName);
    }


    @Test
    public void extensionStart_failed_no_configuration_file() {
        file.delete();

        final InfluxDbExtensionMain main = new InfluxDbExtensionMain();
        when(extensionStartInput.getExtensionInformation()).thenReturn(extensionInformation);
        when(extensionStartInput.getExtensionInformation().getExtensionHomeFolder()).thenReturn(root);


        main.extensionStart(extensionStartInput, extensionStartOutput);

        verify(extensionStartOutput).preventExtensionStartup(anyString());
    }

    @Test
    public void extensionStart_failed_configuration_file_not_valid() throws IOException {

        final List<String> lines = Arrays.asList("host:localhost", "port:-3000");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final InfluxDbExtensionMain main = new InfluxDbExtensionMain();
        when(extensionStartInput.getExtensionInformation()).thenReturn(extensionInformation);
        when(extensionStartInput.getExtensionInformation().getExtensionHomeFolder()).thenReturn(root);


        main.extensionStart(extensionStartInput, extensionStartOutput);

        verify(extensionStartOutput).preventExtensionStartup(anyString());
    }

    @Ignore
    @Test
    public void extensionStart_failed_configuration_file_valid() throws IOException {

        final List<String> lines = Arrays.asList("host:localhost", "port:3000");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final InfluxDbExtensionMain main = new InfluxDbExtensionMain();
        when(extensionStartInput.getExtensionInformation()).thenReturn(extensionInformation);
        when(extensionStartInput.getExtensionInformation().getExtensionHomeFolder()).thenReturn(root);


        main.extensionStart(extensionStartInput, extensionStartOutput);

        verify(extensionStartOutput, times(0));
    }

    @Test
    public void extensionStop() {
    }
}