package io.quarkiverse.dapr.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface WorkflowsConfig {

    /**
     * Indicates whether Dapr Workflow should be enabled.
     */
    @WithDefault("true")
    boolean enabled();
}
