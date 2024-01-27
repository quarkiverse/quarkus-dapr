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

    /**
     * Whether the Dapr managed by Quarkus Dev Services is shared.
     * <p>
     * When shared, Quarkus looks for running containers using label-based service
     * discovery.
     * If a matching container is found, it is used, and so a second one is not
     * started.
     * Otherwise, Dev Services for Dapr starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-dapr} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-dapr} label attached to
     * the started container.
     * <p>
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Dapr
     * looks for a container with the
     * {@code quarkus-dev-service-dapr} label
     * set to the configured value. If found, it will use this container instead of
     * starting a new one. Otherwise it
     * starts a new container with the {@code quarkus-dev-service-dapr}
     * label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Dapr sidecars.
     */
    @ConfigItem(defaultValue = "dapr")
    public String serviceName;

    /**
     * The HTTP port used to binding the Dapr container HTTP port.
     */
    @ConfigItem(defaultValue = "3500")
    public Integer httpPort;

    /**
     * The GRPC port used to biding the Dapr container GRPC port.
     */
    @ConfigItem(defaultValue = "50001")
    public Integer grpcPort;
}
