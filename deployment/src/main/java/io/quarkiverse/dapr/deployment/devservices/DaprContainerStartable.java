package io.quarkiverse.dapr.deployment.devservices;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PASSWORD;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PGSQL_NETWORK_ALIAS;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PGSQL_STATE_STORE;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.POSTGRESQL_PORT;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.USERNAME;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.quarkiverse.dapr.deployment.DaprProcessor;
import io.quarkiverse.dapr.deployment.QuarkusPorts;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.runtime.LaunchMode;

public class DaprContainerStartable extends DaprContainer implements Startable {

    private final DaprDevServiceBuildTimeConfig config;

    public DaprContainerStartable(DaprDevServiceBuildTimeConfig config, LaunchMode launchMode, Network network,
            List<Component> components) {
        super(DockerImageName.parse(config.daprdImage()).asCompatibleSubstituteFor(
                DAPR_RUNTIME_IMAGE_TAG));

        this.config = config;

        super.withAppName("local-dapr-app")
                .withAppPort(QuarkusPorts.http(launchMode))
                .withDaprLogLevel(DaprLogLevel.DEBUG)
                .withNetwork(network)
                .withAppChannelAddress("host.testcontainers.internal");

        Testcontainers.exposeHostPorts(QuarkusPorts.http(launchMode),
                QuarkusPorts.grpc(launchMode));

        for (Component component : components) {
            super.withComponent(component);
        }
    }

    @Override
    public String getConnectionInfo() {
        return "";
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void start() {
        if (!config.dashboard().enabled().get()) {
            // use in-memory
            super.withComponent(new Component("kvstore", "state.in-memory", "v1",
                    Collections.singletonMap("actorStateStore", String.valueOf(true))));
        } else {
            configureWithPgsqlStateStore();
        }

        super.start();
    }

    public void configureWithPgsqlStateStore() {
        final Map<String, String> pgsql = new HashMap<>();
        pgsql.put("port", String.valueOf(POSTGRESQL_PORT));
        pgsql.put("host", PGSQL_NETWORK_ALIAS);
        pgsql.put("user", USERNAME);
        pgsql.put("password", PASSWORD);
        pgsql.put("database", DaprProcessor.FEATURE);
        pgsql.put("actorStateStore", String.valueOf(true));
        super.withComponent(new Component(PGSQL_STATE_STORE, "state.postgresql", "v2", pgsql));
    }
}
