package io.quarkiverse.dapr.deployment;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.LaunchMode;

public class QuarkusPorts {

    enum Map {
        GRPC("quarkus.grpc.server.port", 9000),
        GRPC_TEST("quarkus.grpc.server.test-port", 9001),
        HTTP("quarkus.http.port", 8080),
        HTTP_TEST("quarkus.http.test-port", 8081);

        private final String property;
        private final int defaultPort;

        Map(String property, int defaultPort) {
            this.property = property;
            this.defaultPort = defaultPort;
        }
    }

    public static int http(LaunchMode launchMode) {
        if (launchMode == LaunchMode.TEST) {
            return ConfigProvider.getConfig().getOptionalValue(Map.HTTP_TEST.property, Integer.class)
                    .orElse(Map.HTTP_TEST.defaultPort);
        }
        return ConfigProvider.getConfig().getOptionalValue(Map.HTTP.property, Integer.class).orElse(Map.HTTP.defaultPort);
    }

    public static int grpc(LaunchMode launchMode) {
        if (launchMode == LaunchMode.TEST) {
            return ConfigProvider.getConfig().getOptionalValue(Map.GRPC_TEST.property, Integer.class)
                    .orElse(Map.GRPC_TEST.defaultPort);
        }
        return ConfigProvider.getConfig().getOptionalValue(Map.GRPC.property, Integer.class).orElse(Map.GRPC.defaultPort);
    }
}
