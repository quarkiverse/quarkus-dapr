package io.quarkiverse.dapr.deployment.devservices;

import static io.quarkiverse.dapr.deployment.DaprProcessor.FEATURE;
import static io.quarkiverse.dapr.deployment.devservices.DashboardContainerStartable.INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.POSTGRESQL_PORT;
import static io.quarkiverse.dapr.devui.DaprDashboardRPCService.DAPR_DASHBOARD_WORKFLOW_URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import io.dapr.config.Properties;
import io.quarkiverse.dapr.config.DaprDevServiceBuildTimeConfig;
import io.quarkiverse.dapr.devui.DaprDashboardRPCService;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class DevServicesDaprProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevServicesDaprProcessor.class);

    private static final String QUARKUS_DAPR_SERVICE_NAME_PREFIX = "quarkus-dev-service-";
    private static final String DASHBOARD_WORKFLOW = QUARKUS_DAPR_SERVICE_NAME_PREFIX + "dashboard-workflow";
    private static final String STATESTORE_PG = QUARKUS_DAPR_SERVICE_NAME_PREFIX + "statestore-pgsql";
    private static final String POSTGRESQL_PORT_PROPERTY = "quarkus.dapr.devservices.dashboard.pgsql.port";

    @BuildStep
    public CardPageBuildItem cardPage() {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.externalPageBuilder("Dapr Dashboard Workflow")
                .dynamicUrlJsonRPCMethodName("getDashboardWorkflowUrl"));

        return cardPageBuildItem;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public JsonRPCProvidersBuildItem dashboardWorkflow() {
        return new JsonRPCProvidersBuildItem(DaprDashboardRPCService.class, BuiltinScope.SINGLETON.getName());
    }

    @BuildStep(onlyIfNot = { IsNormal.class })
    List<DevServicesResultBuildItem> devServices(
            DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchMode) {

        if (!config.enabled().get()) {
            return null;
        }

        Network.NetworkImpl network = Network.builder().build();

        List<DevServicesResultBuildItem> containers = new ArrayList<>();
        DevServicesResultBuildItem dapr = configureDaprContainer(config, launchMode, network);
        containers.add(dapr);

        if (config.dashboard().enabled().get()) {
            DevServicesResultBuildItem pgsql = configurePgsqlContainer(network);
            DevServicesResultBuildItem dashboard = configureDashboardWorkflowContainer(network);
            containers.add(dashboard);
            containers.add(pgsql);
        }

        return containers;
    }

    private static DevServicesResultBuildItem configureDaprContainer(DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchMode, Network network) {
        return DevServicesResultBuildItem.owned()
                .serviceName(FEATURE)
                .feature(FEATURE)
                .startable(new Supplier<Startable>() {
                    @Override
                    public Startable get() {
                        return new DaprContainerStartable(config,
                                launchMode.getLaunchMode(), network);
                    }
                })
                .postStartHook(startable -> {
                    DaprContainerStartable daprContainerStartable = (DaprContainerStartable) startable;
                    System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(daprContainerStartable.getGrpcPort()));
                    System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(daprContainerStartable.getHttpPort()));
                })
                .build();
    }

    private static DevServicesResultBuildItem configureDashboardWorkflowContainer(Network.NetworkImpl network) {
        DevServicesResultBuildItem dashboard = DevServicesResultBuildItem.owned()
                .serviceName(DASHBOARD_WORKFLOW)
                .feature(FEATURE)
                .startable(new Supplier<Startable>() {
                    @Override
                    public Startable get() {
                        return new DashboardContainerStartable(network);
                    }
                })
                .dependsOnConfig(POSTGRESQL_PORT_PROPERTY, (Startable startable, String value) -> {
                    LOGGER.info("Running dependsOnConfig for DashboardContainerStartable container");
                    DashboardContainerStartable d = (DashboardContainerStartable) startable;
                    d.setupStateStore();
                })
                .configProvider(Map.of(DAPR_DASHBOARD_WORKFLOW_URL, startable -> {
                    DashboardContainerStartable container = (DashboardContainerStartable) startable;
                    return "http://127.0.0.1:" + container.getMappedPort(INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT);
                }))
                .build();
        return dashboard;
    }

    private static DevServicesResultBuildItem configurePgsqlContainer(Network.NetworkImpl network) {
        DevServicesResultBuildItem pgsql = DevServicesResultBuildItem.owned()
                .serviceName(STATESTORE_PG)
                .feature(FEATURE)
                .configProvider(Map.of(POSTGRESQL_PORT_PROPERTY, startable -> {
                    StateStoreContainerStartable database = (StateStoreContainerStartable) startable;
                    return String.valueOf(database.getMappedPort(POSTGRESQL_PORT));
                }))
                .startable(new Supplier<Startable>() {
                    @Override
                    public Startable get() {
                        return new StateStoreContainerStartable(network);
                    }
                })
                .build();
        return pgsql;
    }
}
