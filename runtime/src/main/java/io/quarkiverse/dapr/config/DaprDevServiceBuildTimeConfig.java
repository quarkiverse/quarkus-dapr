package io.quarkiverse.dapr.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.dapr.devservices")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DaprDevServiceBuildTimeConfig {

    /**
     * Whether this Dev Service should start with the application in dev mode or
     * tests.
     * <p>
     * Dev Services are disabled by default.
     *
     * @asciidoclet
     */
    Optional<Boolean> enabled();

    /**
     * The Dapr container image to use.
     * <p>
     */
    @WithDefault("daprio/daprd:latest")
    String daprdImage();
}
