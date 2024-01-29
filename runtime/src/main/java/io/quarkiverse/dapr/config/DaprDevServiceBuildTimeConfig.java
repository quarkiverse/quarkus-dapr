package io.quarkiverse.dapr.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "dapr.devservices", phase = ConfigPhase.BUILD_TIME)
public class DaprDevServiceBuildTimeConfig {

    /**
     * Whether this Dev Service should start with the application in dev mode or
     * tests.
     * <p>
     * Dev Services are disabled by default.
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();
}
