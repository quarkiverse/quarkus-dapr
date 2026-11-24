package io.quarkiverse.dapr.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.dapr.config.Properties;
import io.dapr.config.Property;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Runtime configuration of the connection to the Dapr sidecar.
 * <p>
 * Values that are not set are not overridden, so the Dapr SDK keeps resolving them from
 * system properties ({@code dapr.*}) and environment variables ({@code DAPR_*}).
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.dapr")
public interface DaprRuntimeConfig {

    /**
     * Dapr sidecar host. Maps to the Dapr SDK property {@code dapr.sidecar.ip}.
     */
    Optional<String> sidecarIp();

    /**
     * Dapr sidecar HTTP port. Maps to the Dapr SDK property {@code dapr.http.port}.
     */
    OptionalInt httpPort();

    /**
     * Dapr sidecar HTTP endpoint, it takes precedence over the sidecar host and HTTP port.
     * Maps to the Dapr SDK property {@code dapr.http.endpoint}.
     */
    Optional<String> httpEndpoint();

    /**
     * Dapr sidecar gRPC port. Maps to the Dapr SDK property {@code dapr.grpc.port}.
     */
    OptionalInt grpcPort();

    /**
     * Dapr sidecar gRPC endpoint, it takes precedence over the sidecar host and gRPC port.
     * Maps to the Dapr SDK property {@code dapr.grpc.endpoint}.
     */
    Optional<String> grpcEndpoint();

    /**
     * API token sent to the Dapr sidecar in the {@code dapr-api-token} header.
     * Maps to the Dapr SDK property {@code dapr.api.token}.
     */
    Optional<String> apiToken();

    /**
     * Converts the configured values into Dapr SDK property overrides.
     * Values that are not set are not included.
     */
    static Map<Property<?>, String> toPropertyOverrides(DaprRuntimeConfig config) {
        Map<Property<?>, String> overrides = new HashMap<>();
        config.sidecarIp().ifPresent(v -> overrides.put(Properties.SIDECAR_IP, v));
        config.httpPort().ifPresent(v -> overrides.put(Properties.HTTP_PORT, Integer.toString(v)));
        config.httpEndpoint().ifPresent(v -> overrides.put(Properties.HTTP_ENDPOINT, v));
        config.grpcPort().ifPresent(v -> overrides.put(Properties.GRPC_PORT, Integer.toString(v)));
        config.grpcEndpoint().ifPresent(v -> overrides.put(Properties.GRPC_ENDPOINT, v));
        config.apiToken().ifPresent(v -> overrides.put(Properties.API_TOKEN, v));
        return overrides;
    }
}
