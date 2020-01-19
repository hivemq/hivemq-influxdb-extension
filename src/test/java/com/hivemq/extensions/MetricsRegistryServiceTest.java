package com.hivemq.extensions;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.extensions.configuration.InfluxDbConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MetricsRegistryServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File root;

    private File file;

    @Before
    public void set_up() throws IOException {
        root = folder.getRoot();
        String fileName = "influxdb.properties";
        file = folder.newFile(fileName);
    }

    @Test
    public void load_with_missing_filter_ok() throws IOException {
        final List<String> lines = Collections.singletonList("empty:");
        MetricRegistryService service = new MetricRegistryService(getConfig(lines), mockOriginalMetricRegistry());
        assertFalse(service.isFilteredRegistryConfigured());
        assertNull(service.getFiltered());
        assertEquals(100, service.getRemaining().getMetrics().size());
    }

    @Test
    public void load_with_configured_filter_ok() throws IOException {
        final List<String> lines = Collections.singletonList("metricsFilterList: com.hivemq.messages.99");
        MetricRegistryService service = new MetricRegistryService(getConfig(lines), mockOriginalMetricRegistry());
        assertTrue(service.isFilteredRegistryConfigured());
        assertEquals(1, service.getFiltered().getMetrics().size());
        assertEquals(99, service.getRemaining().getMetrics().size());
        assertEquals(mockOriginalMetricRegistry().getMetrics().size(),
              service.getFiltered().getMetrics().size() + service.getRemaining().getMetrics().size());
    }

    @Test
    public void load_with_many_configured_filters_ok() throws IOException {
        final List<String> lines = Collections.singletonList("metricsFilterList: com.hivemq.messages.1; com.hivemq.messages.2");
        MetricRegistryService service = new MetricRegistryService(getConfig(lines), mockOriginalMetricRegistry());
        assertTrue(service.isFilteredRegistryConfigured());
        assertEquals(22, service.getFiltered().getMetrics().size());
        assertEquals(78, service.getRemaining().getMetrics().size());
        assertEquals(mockOriginalMetricRegistry().getMetrics().size(),
              service.getFiltered().getMetrics().size() + service.getRemaining().getMetrics().size());
    }

    @Test
    public void load_with_custom_configured_filters_ok() throws IOException {
        final List<String> lines = Collections.singletonList(
              "metricsFilterList: com.hivemq.messages.1; com.hivemq.messages.2; com.hivemq.cache; com.hivemq.msg.rate");
        MetricRegistry original = mockOriginalMetricRegistry();
        original.register("com.hivemq.cache.something.else", new Metric(){});

        MetricRegistryService service = new MetricRegistryService(getConfig(lines), original);
        assertTrue(service.isFilteredRegistryConfigured());
        assertEquals(23, service.getFiltered().getMetrics().size());
        assertEquals(78, service.getRemaining().getMetrics().size());
        assertEquals(original.getMetrics().size(),
              service.getFiltered().getMetrics().size() + service.getRemaining().getMetrics().size());
    }

    private MetricRegistry mockOriginalMetricRegistry() {
        MetricRegistry ret = new MetricRegistry();
        for (int i = 0; i < 100; i++) {
            ret.register("com.hivemq.messages." + i, new Metric() { });
        }
        return ret;
    }

    private InfluxDbConfiguration getConfig(List<String> lines) throws IOException {
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        final InfluxDbConfiguration ret = new InfluxDbConfiguration(root);
        ret.readPropertiesFromFile();
        return ret;
    }
}
