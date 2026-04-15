package io.quarkiverse.dapr.deployment.devservices;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;

import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkiverse.dapr.deployment.DaprProcessor;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.runtime.LaunchMode;

public class StateStoreContainerStartable extends PostgreSQLContainer implements Startable {

    public static int POSTGRESQL_PORT = 5432;
    public static String POSTGRES = "postgres";
    public static String PGSQL_NETWORK_ALIAS = POSTGRES;
    public static String USERNAME = POSTGRES;
    public static String PASSWORD = POSTGRES;
    public static String PGSQL_STATE_STORE = "kvstore";
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-dapr-pgsql";

    public StateStoreContainerStartable(Network network, LaunchMode launchMode) {
        super(DockerImageName.parse("postgres"));
        super.withNetwork(network)
                .withNetworkAliases(PGSQL_NETWORK_ALIAS)
                .withDatabaseName(DaprProcessor.FEATURE)
                .withUsername(USERNAME)
                .withPassword(PASSWORD);
        configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, "dapr-pgsql");
    }

    @Override
    public String getConnectionInfo() {
        return super.getHost();
    }

    @Override
    public void close() {
        super.close();
    }
}
