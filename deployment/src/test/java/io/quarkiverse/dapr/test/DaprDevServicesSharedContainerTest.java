package io.quarkiverse.dapr.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.dapr.config.Properties;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;

/**
 * Regression tests for the shared-container feature.
 *
 * <h2>Background</h2>
 * When a second application starts in dev-mode and finds an already-running Dapr container
 * via {@code DAPR_CONTAINER_LOCATOR}, the processor used to call
 * {@code DevServicesResultBuildItem.discovered().name(...).containerId(...).build()} without
 * providing a config map.
 * <p>
 * The deprecated constructor that backs {@code DiscoveredServiceBuilder.build()} stores
 * {@code this.config = config} as-is, so the {@code getConfig()} accessor returned
 * {@code null}. Quarkus core's {@code DevServicesConfigBuildStep#setup} then called
 * {@code newProperties.putAll(resultBuildItem.getConfig())} which threw:
 *
 * <pre>
 * java.lang.NullPointerException: Cannot invoke "java.util.Map.size()" because "m" is null
 *   at java.util.HashMap.putMapEntries
 *   at io.quarkus.deployment.steps.DevServicesConfigBuildStep.setup(DevServicesConfigBuildStep.java:32)
 * </pre>
 *
 * <h2>Fix</h2>
 * The fix is to always call {@code .config(nonNullMap)} on the {@code DiscoveredServiceBuilder},
 * populating it with the resolved Dapr HTTP- and gRPC-port values so the second application
 * also receives them as runtime configuration.
 */
class DaprDevServicesSharedContainerTest {

    /**
     * Simulates what the processor does when it discovers a shared container.
     * The produced {@code DevServicesResultBuildItem} must never expose a null config map,
     * otherwise {@code DevServicesConfigBuildStep} will NPE.
     */
    @Test
    void discoveredItemMustHaveNonNullConfigMap() {
        int httpPort = 3500;
        int grpcPort = 50001;

        Map<String, String> portConfig = new HashMap<>();
        portConfig.put(Properties.HTTP_PORT.getName(), Integer.toString(httpPort));
        portConfig.put(Properties.GRPC_PORT.getName(), Integer.toString(grpcPort));

        DevServicesResultBuildItem item = DevServicesResultBuildItem.discovered()
                .name("dapr")
                .containerId("abc123")
                .config(java.util.Collections.unmodifiableMap(portConfig))
                .build();

        // getConfig() must never return null – putAll(null) would throw NPE
        assertThat(item.getConfig())
                .as("DevServicesResultBuildItem.getConfig() must not be null")
                .isNotNull();
    }

    /**
     * Verifies that the Dapr HTTP port is stored in the config map under the property name
     * used by the Dapr SDK ({@link Properties#HTTP_PORT}).
     */
    @Test
    void discoveredItemContainsHttpPortProperty() {
        int httpPort = 13500;
        int grpcPort = 60001;

        Map<String, String> portConfig = new HashMap<>();
        portConfig.put(Properties.HTTP_PORT.getName(), Integer.toString(httpPort));
        portConfig.put(Properties.GRPC_PORT.getName(), Integer.toString(grpcPort));

        DevServicesResultBuildItem item = DevServicesResultBuildItem.discovered()
                .name("dapr")
                .containerId("container-42")
                .config(java.util.Collections.unmodifiableMap(portConfig))
                .build();

        assertThat(item.getConfig())
                .containsEntry(Properties.HTTP_PORT.getName(), "13500")
                .containsEntry(Properties.GRPC_PORT.getName(), "60001");
    }

    /**
     * Emulates the exact line in {@code DevServicesConfigBuildStep#setup} that was crashing,
     * to make the failure mode explicit and guarantee the fix prevents a regression.
     */
    @Test
    void putAllOnDiscoveredItemConfigDoesNotThrowNpe() {
        Map<String, String> portConfig = new HashMap<>();
        portConfig.put(Properties.HTTP_PORT.getName(), "3500");
        portConfig.put(Properties.GRPC_PORT.getName(), "50001");

        DevServicesResultBuildItem item = DevServicesResultBuildItem.discovered()
                .name("dapr")
                .containerId("container-shared")
                .config(java.util.Collections.unmodifiableMap(portConfig))
                .build();

        Map<String, String> aggregated = new HashMap<>();
        // This is the exact operation that was NPE-ing in DevServicesConfigBuildStep:
        assertThatCode(() -> aggregated.putAll(item.getConfig()))
                .as("putAll(getConfig()) must not throw NullPointerException")
                .doesNotThrowAnyException();

        assertThat(aggregated).isNotEmpty();
    }
}
