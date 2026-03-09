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
     * Indicates if the Dapr Dev Service should try to re-use a matching container.
     * <p>
     * This only applies in dev mode.
     */
    @WithDefault("true")
    Optional<Boolean> shared();

    /**
     * The value of the {@code quarkus-dev-service-dapr} label attached to the started container.
     * <p>
     * This is used to discover and re-use an existing shared Dapr container.
     */
    @WithDefault("dapr")
    String serviceName();

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
