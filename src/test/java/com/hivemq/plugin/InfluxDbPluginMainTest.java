package com.hivemq.plugin;

import com.hivemq.plugin.api.parameter.PluginInformation;
import com.hivemq.plugin.api.parameter.PluginStartInput;
import com.hivemq.plugin.api.parameter.PluginStartOutput;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InfluxDbPluginMainTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    PluginStartInput pluginStartInput;

    @Mock
    PluginStartOutput pluginStartOutput;

    @Mock
    PluginInformation pluginInformation;

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
    public void pluginStart_failed_no_configuration_file() {
        file.delete();

        final InfluxDbPluginMain main = new InfluxDbPluginMain();
        when(pluginStartInput.getPluginInformation()).thenReturn(pluginInformation);
        when(pluginStartInput.getPluginInformation().getPluginHomeFolder()).thenReturn(root);


        main.pluginStart(pluginStartInput, pluginStartOutput);

        verify(pluginStartOutput).preventPluginStartup(anyString());
    }

    @Test
    public void pluginStart_failed_configuration_file_not_valid() throws IOException {

        final List<String> lines = Arrays.asList("host:localhost", "port:-3000");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbPluginMain main = new InfluxDbPluginMain();
        when(pluginStartInput.getPluginInformation()).thenReturn(pluginInformation);
        when(pluginStartInput.getPluginInformation().getPluginHomeFolder()).thenReturn(root);


        main.pluginStart(pluginStartInput, pluginStartOutput);

        verify(pluginStartOutput).preventPluginStartup(anyString());
    }

    @Ignore
    @Test
    public void pluginStart_failed_configuration_file_valid() throws IOException {

        final List<String> lines = Arrays.asList("host:localhost", "port:3000");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final InfluxDbPluginMain main = new InfluxDbPluginMain();
        when(pluginStartInput.getPluginInformation()).thenReturn(pluginInformation);
        when(pluginStartInput.getPluginInformation().getPluginHomeFolder()).thenReturn(root);


        main.pluginStart(pluginStartInput, pluginStartOutput);

        verify(pluginStartOutput, times(0));
    }

    @Test
    public void pluginStop() {
    }
}