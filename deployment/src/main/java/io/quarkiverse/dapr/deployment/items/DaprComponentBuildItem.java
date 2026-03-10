package io.quarkiverse.dapr.deployment.items;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.MetadataEntry;
import io.quarkus.builder.item.MultiBuildItem;

public final class DaprComponentBuildItem extends MultiBuildItem {

    private final String name;
    private final String type;
    private final String version;
    private final Map<String, String> metadata;

    public DaprComponentBuildItem(String name, String type, String version, Map<String, String> metadata) {
        this.name = name;
        this.type = type;
        this.version = version;
        this.metadata = metadata == null ? Collections.emptyMap() : metadata;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Component toComponent() {
        List<MetadataEntry> metadataEntries = metadata.entrySet().stream()
                .map(entry -> new MetadataEntry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return new Component(name, type, version, metadataEntries);
    }
}
