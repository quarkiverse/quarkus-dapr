package io.quarkiverse.dapr.deployment.devservices;

import static io.quarkiverse.dapr.deployment.DaprProcessor.FEATURE;
import static io.quarkiverse.dapr.deployment.devservices.DashboardContainerStartable.INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PGSQL_STATE_STORE;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.POSTGRESQL_PORT;
import static io.quarkiverse.dapr.devui.DaprDashboardRPCService.DAPR_DASHBOARD_WORKFLOW_URL;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import io.dapr.config.Properties;
import io.quarkiverse.dapr.devui.DaprDashboardRPCService;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class DevServicesDaprProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevServicesDaprProcessor.class);
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-dapr";
    private static final int DAPR_INTERNAL_HTTP_PORT = 3500;
    private static final int DAPR_INTERNAL_GRPC_PORT = 50001;
    private static final ContainerLocator DAPR_CONTAINER_LOCATOR = locateContainerWithLabels(DAPR_INTERNAL_HTTP_PORT,
            DEV_SERVICE_LABEL);

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

    @BuildStep(onlyIfNot = { IsProduction.class })
    List<DevServicesResultBuildItem> devServices(
            DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchMode) {

        if (!config.enabled().get()) {
            return null;
        }

        Network.NetworkImpl network = Network.builder()
                .build();

        List<DevServicesResultBuildItem> containers = new ArrayList<>();
        DevServicesResultBuildItem discoveredDapr = discoverDaprContainer(config, launchMode);
        if (discoveredDapr != null) {
            containers.add(discoveredDapr);
            return containers;
        }

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

    private static DevServicesResultBuildItem discoverDaprContainer(DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchModeBuildItem) {
        Map<Integer, ContainerAddress> mappedPorts = new HashMap<>();
        Optional<String> containerId = DAPR_CONTAINER_LOCATOR.locateContainer(config.serviceName(), config.shared().get(),
                launchModeBuildItem.getLaunchMode(),
                mappedPorts::put);

        if (containerId.isEmpty()) {
            return null;
        }

        ContainerAddress grpcAddress = mappedPorts.get(DAPR_INTERNAL_GRPC_PORT);
        ContainerAddress httpAddress = mappedPorts.get(DAPR_INTERNAL_HTTP_PORT);
        if (grpcAddress == null || httpAddress == null) {
            LOGGER.warn("Found shared Dapr container {} but missing mapped ports. Creating a new container instead.",
                    containerId.get());
            return null;
        }

        configureDaprPorts(grpcAddress.getPort(), httpAddress.getPort());
        LOGGER.info("Re-using shared Dapr container {} listening on HTTP {} and gRPC {}",
                containerId.get(), httpAddress.getPort(), grpcAddress.getPort());
        return DevServicesResultBuildItem.discovered()
                .name(FEATURE)
                .containerId(containerId.get())
                .build();
    }

    private static DevServicesResultBuildItem configureDaprContainer(DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchMode, Network network) {
        DevServicesResultBuildItem.OwnedServiceBuilder<Startable> builder = DevServicesResultBuildItem.owned()
                .serviceName(FEATURE)
                .feature(FEATURE)
                .startable(new Supplier<Startable>() {
                    @Override
                    public Startable get() {
                        return new DaprContainerStartable(config,
                                launchMode.getLaunchMode(), network);
                    }
                });

        if (config.dashboard().enabled().get()) {
            builder.dependsOnConfig(POSTGRESQL_PORT_PROPERTY, (startable, value) -> {
                LOGGER.info("Dapr statestore {} is running", PGSQL_STATE_STORE);
            });
        }

        return builder
                .postStartHook(startable -> {
                    DaprContainerStartable daprContainerStartable = (DaprContainerStartable) startable;
                    configureDaprPorts(daprContainerStartable.getGrpcPort(), daprContainerStartable.getHttpPort());
                })
                .build();
    }

    private static void configureDaprPorts(int grpcPort, int httpPort) {
        System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(grpcPort));
        System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(httpPort));
    }

    private static DevServicesResultBuildItem configureDashboardWorkflowContainer(Network network) {
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

    private static DevServicesResultBuildItem configurePgsqlContainer(Network network) {
        return DevServicesResultBuildItem.owned()
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
    }
}
