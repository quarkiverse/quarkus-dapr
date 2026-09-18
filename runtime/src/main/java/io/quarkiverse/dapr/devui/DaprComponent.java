package io.quarkiverse.dapr.devui;

import java.util.Collections;
import java.util.Map;

/**
 * A Dapr component discovered at build time from the {@code components} directory in the classpath.
 * Exposed in the Dev UI so users can see which components are in use.
 */
public final class DaprComponent {

    private final String name;
    private final String type;
    private final String version;
    private final Map<String, String> metadata;

    public DaprComponent(String name, String type, String version, Map<String, String> metadata) {
        this.name = name;
        this.type = type;
        this.version = version;
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
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
}
