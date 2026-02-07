package io.quarkiverse.dapr.deployment.devservices;

import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PGSQL_NETWORK_ALIAS;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PGSQL_STATE_STORE;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainerConstants;
import io.dapr.testcontainers.WorkflowDashboardContainer;
import io.quarkus.deployment.builditem.Startable;

public class DashboardContainerStartable extends WorkflowDashboardContainer implements Startable {

    public static final int INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT = 8080;

    private static final Map<String, String> POSTGRE_SQL_DETAILS = new HashMap<>();

    public DashboardContainerStartable(Network network) {
        super(DockerImageName.parse(DaprContainerConstants.DAPR_WORKFLOWS_DASHBOARD));
        super.withExposedPorts(INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT)
                .withNetwork(network);
    }

    @Override
    public String getConnectionInfo() {
        return super.getHost();
    }

    @Override
    public void close() {
        super.close();
    }

    public void setupStateStore() {
        POSTGRE_SQL_DETAILS.put("port", "5432");
        POSTGRE_SQL_DETAILS.put("host", PGSQL_NETWORK_ALIAS);
        POSTGRE_SQL_DETAILS.put("user", "postgres");
        POSTGRE_SQL_DETAILS.put("password", "postgres");
        POSTGRE_SQL_DETAILS.put("database", "dapr");
        POSTGRE_SQL_DETAILS.put("actorStateStore", String.valueOf(true));
        super.withStateStoreComponent(new Component(PGSQL_STATE_STORE, "state.postgresql", "v2", POSTGRE_SQL_DETAILS));
    }

    public void configureWithPgsqlStateStore() {

    }
}
