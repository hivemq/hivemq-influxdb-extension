package com.hivemq.extensions;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.configuration.InfluxDbConfiguration;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MetricRegistryService {

    private MetricRegistry originalRegistry;
    private MetricRegistry filteredRegistry;
    private MetricRegistry remainingRegistry;

    public MetricRegistryService(InfluxDbConfiguration config, MetricRegistry originalRegistry) {
        this.originalRegistry = originalRegistry;
        this.filteredRegistry = extractFilteredRegistry(config);
        this.remainingRegistry = (isFilteredRegistryConfigured()) ? extractRemainingRegistry() : originalRegistry;
    }

    public boolean isFilteredRegistryConfigured() {
        return filteredRegistry != null && filteredRegistry.getMetrics().size() > 0;
    }

    public MetricRegistry getFiltered() {
        return filteredRegistry;
    }

    public MetricRegistry getRemaining() {
        return remainingRegistry;
    }

    /**
     * Extract a MetricRegistry with all metrics that match the defined filter
     * @param configuration for this extension
     * @return new MetricRegistry with filtered metrics
     */
    private MetricRegistry extractFilteredRegistry(InfluxDbConfiguration configuration) {
        String filterStr = configuration.getMetricFilter();
        if (filterStr == null) return null;

        Set<String> filterSet = Arrays.stream(filterStr.split(";")).map(String::trim).collect(Collectors.toSet());

        Map<String, Metric> originalMetrics = originalRegistry.getMetrics();
        Map<String, Metric> filteredMetrics = originalMetrics.entrySet().stream()
              .filter(e -> filterSet.stream().anyMatch(n -> e.getKey().startsWith(n)))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        MetricRegistry ret = new MetricRegistry();
        for (Map.Entry<String, Metric> e : filteredMetrics.entrySet()) {
            ret.register(e.getKey(), e.getValue());
        }
        return ret;
    }

    /**
     * @return new MetricRegistry = originalMetricRegistry - filteredMetricRegistry
     */
    private MetricRegistry extractRemainingRegistry() {
        Map<String, Metric> originalMetrics = originalRegistry.getMetrics();
        Map<String, Metric> filteredMetrics = filteredRegistry.getMetrics();

        Map<String, Metric> remainingMetrics = originalMetrics.entrySet().stream()
              .filter(e -> !filteredMetrics.containsKey(e.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        MetricRegistry ret = new MetricRegistry();
        for (Map.Entry<String, Metric> e : remainingMetrics.entrySet()) {
            ret.register(e.getKey(), e.getValue());
        }
        return ret;
    }

}
