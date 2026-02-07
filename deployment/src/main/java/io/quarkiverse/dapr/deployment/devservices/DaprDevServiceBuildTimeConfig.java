package io.quarkiverse.dapr.deployment.devservices;

import java.util.Optional;

import io.dapr.testcontainers.DaprContainerConstants;
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
     * Dapr Dev Services are enabled by default.
     */
    @WithDefault("true")
    Optional<Boolean> enabled();

    /**
     * The Dapr container image to use.
     * <p>
     */
    @WithDefault(DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG)
    String daprdImage();

    /**
     * Dapr Dashboard configuration
     */
    Dashboard dashboard();

    interface Dashboard {

        /**
         * Whether this Dev Service should start the Dapr Workflow Dashboard.
         */
        @WithDefault("true")
        Optional<Boolean> enabled();

    }
}
