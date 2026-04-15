package io.quarkiverse.dapr.deployment.devservices;

import static io.quarkiverse.dapr.deployment.DaprProcessor.FEATURE;
import static io.quarkiverse.dapr.deployment.devservices.DashboardContainerStartable.INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PGSQL_STATE_STORE;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.POSTGRESQL_PORT;
import static io.quarkiverse.dapr.devui.DaprDashboardRPCService.DAPR_DASHBOARD_WORKFLOW_URL;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.util.ArrayList;
import java.util.Collections;
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
    static final String DASHBOARD_WORKFLOW_LABEL = "quarkus-dev-service-dapr-dashboard";

    private static final int DAPR_INTERNAL_HTTP_PORT = 3500;
    private static final int DAPR_INTERNAL_GRPC_PORT = 50001;
    private static final ContainerLocator DAPR_CONTAINER_LOCATOR = locateContainerWithLabels(DAPR_INTERNAL_HTTP_PORT,
            DEV_SERVICE_LABEL);
    private static final ContainerLocator DASHBOARD_CONTAINER_LOCATOR = locateContainerWithLabels(
            DashboardContainerStartable.INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT,
            DASHBOARD_WORKFLOW_LABEL);

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
            // Even when reusing a shared Dapr container, we still need to start the
            // dashboard and state-store containers for this application (or discover them).
            if (config.dashboard().enabled().get()) {
                DevServicesResultBuildItem discoveredDashboard = discoverDashboardContainer(config, launchMode);
                if (discoveredDashboard != null) {
                    containers.add(discoveredDashboard);
                } else {
                    DevServicesResultBuildItem pgsql = configurePgsqlContainer(network, launchMode);
                    DevServicesResultBuildItem dashboard = configureDashboardWorkflowContainer(config, network, launchMode);
                    containers.add(pgsql);
                    containers.add(dashboard);
                }
            }
            return containers;
        }

        DevServicesResultBuildItem dapr = configureDaprContainer(config, launchMode, network);
        containers.add(dapr);

        if (config.dashboard().enabled().get()) {
            DevServicesResultBuildItem discoveredDashboard = discoverDashboardContainer(config, launchMode);
            if (discoveredDashboard != null) {
                containers.add(discoveredDashboard);
            } else {
                DevServicesResultBuildItem pgsql = configurePgsqlContainer(network, launchMode);
                DevServicesResultBuildItem dashboard = configureDashboardWorkflowContainer(config, network, launchMode);
                containers.add(pgsql);
                containers.add(dashboard);
            }
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

        int grpcPort = grpcAddress.getPort();
        int httpPort = httpAddress.getPort();
        configureDaprPorts(grpcPort, httpPort);
        LOGGER.info("Re-using shared Dapr container {} listening on HTTP {} and gRPC {}",
                containerId.get(), httpPort, grpcPort);

        // Always supply a non-null config map so DevServicesConfigBuildStep#setup won't NPE
        Map<String, String> portConfig = new HashMap<>();
        portConfig.put(Properties.HTTP_PORT.getName(), Integer.toString(httpPort));
        portConfig.put(Properties.GRPC_PORT.getName(), Integer.toString(grpcPort));

        return DevServicesResultBuildItem.discovered()
                .name(FEATURE)
                .containerId(containerId.get())
                .config(Collections.unmodifiableMap(portConfig))
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

    private static DevServicesResultBuildItem discoverDashboardContainer(DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchModeBuildItem) {
        Map<Integer, ContainerAddress> mappedPorts = new HashMap<>();
        Optional<String> containerId = DASHBOARD_CONTAINER_LOCATOR.locateContainer(
                config.dashboard().serviceName(),
                config.shared().get(),
                launchModeBuildItem.getLaunchMode(),
                mappedPorts::put);

        if (containerId.isEmpty()) {
            return null;
        }

        ContainerAddress dashboardAddress = mappedPorts.get(DashboardContainerStartable.INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT);
        if (dashboardAddress == null) {
            LOGGER.warn("Found shared Dapr Dashboard container {} but missing mapped port. Creating a new one instead.",
                    containerId.get());
            return null;
        }

        String dashboardUrl = "http://127.0.0.1:" + dashboardAddress.getPort();
        LOGGER.info("Re-using shared Dapr Dashboard container {} at {}", containerId.get(), dashboardUrl);

        Map<String, String> dashboardConfig = new HashMap<>();
        dashboardConfig.put(DAPR_DASHBOARD_WORKFLOW_URL, dashboardUrl);

        return DevServicesResultBuildItem.discovered()
                .name(FEATURE)
                .containerId(containerId.get())
                .config(Collections.unmodifiableMap(dashboardConfig))
                .build();
    }

    private static void configureDaprPorts(int grpcPort, int httpPort) {
        System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(grpcPort));
        System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(httpPort));
    }

    private static DevServicesResultBuildItem configureDashboardWorkflowContainer(DaprDevServiceBuildTimeConfig config,
            Network network, LaunchModeBuildItem launchMode) {
        DevServicesResultBuildItem dashboard = DevServicesResultBuildItem.owned()
                .serviceName(DASHBOARD_WORKFLOW)
                .feature(FEATURE)
                .startable(new Supplier<Startable>() {
                    @Override
                    public Startable get() {
                        return new DashboardContainerStartable(network, launchMode.getLaunchMode(),
                                config.dashboard().serviceName());
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

    private static DevServicesResultBuildItem configurePgsqlContainer(Network network,
            LaunchModeBuildItem launchMode) {
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
                        return new StateStoreContainerStartable(network, launchMode.getLaunchMode());
                    }
                })
                .build();
    }
}
