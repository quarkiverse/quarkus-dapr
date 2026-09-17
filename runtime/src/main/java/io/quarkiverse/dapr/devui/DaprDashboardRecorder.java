package io.quarkiverse.dapr.devui;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DaprDashboardRecorder {

    public Supplier<DaprComponent> component(String name, String type, String version, Map<String, String> metadata) {
        return () -> new DaprComponent(name, type, version, metadata);
    }
}
