package io.quarkiverse.dapr.deployment.devservices;

import static io.quarkiverse.dapr.deployment.DaprProcessor.FEATURE;
import static io.quarkiverse.dapr.deployment.devservices.DashboardContainerStartable.INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PGSQL_STATE_STORE;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.POSTGRESQL_PORT;
import static io.quarkiverse.dapr.devui.DaprDashboardRPCService.DAPR_DASHBOARD_WORKFLOW_URL;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.yaml.snakeyaml.Yaml;

import io.dapr.config.Properties;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.MetadataEntry;
import io.quarkiverse.dapr.deployment.items.DaprComponentBuildItem;
import io.quarkiverse.dapr.devui.DaprComponent;
import io.quarkiverse.dapr.devui.DaprDashboardRPCService;
import io.quarkiverse.dapr.devui.DaprDashboardRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.runtime.util.ClassPathUtils;

public class DevServicesDaprProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevServicesDaprProcessor.class);

    private static final String QUARKUS_DAPR_SERVICE_NAME_PREFIX = "quarkus-dev-service-";
    private static final String DASHBOARD_WORKFLOW = QUARKUS_DAPR_SERVICE_NAME_PREFIX + "dashboard-workflow";
    private static final String STATESTORE_PG = QUARKUS_DAPR_SERVICE_NAME_PREFIX + "statestore-pgsql";
    private static final String POSTGRESQL_PORT_PROPERTY = "quarkus.dapr.devservices.dashboard.pgsql.port";
    private static final String COMPONENTS_DIR = "components";
    private static final String PGSQL_STATE_STORE_TYPE = "state.postgresql";

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

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIfNot = IsProduction.class)
    void daprComponentBeans(DaprDashboardRecorder recorder, List<DaprComponentBuildItem> componentBuildItems,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        for (DaprComponentBuildItem item : componentBuildItems) {
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(DaprComponent.class)
                    .scope(Singleton.class)
                    .identifier(item.getName())
                    .supplier(recorder.component(item.getName(), item.getType(), item.getVersion(), item.getMetadata()))
                    .unremovable()
                    .done());
        }
    }

    @BuildStep
    void discoverComponents(BuildProducer<DaprComponentBuildItem> componentProducer) {
        try {
            List<Component> components = tryGenerateComponents();
            for (Component component : components) {
                LOGGER.info("Discovered Dapr component: {} ({})", component.getName(), component.getType());
                Map<String, String> metadata = component.getMetadata().stream()
                        .collect(Collectors.toMap(MetadataEntry::getName, MetadataEntry::getValue, (v1, v2) -> v1));
                componentProducer.produce(new DaprComponentBuildItem(
                        component.getName(),
                        component.getType(),
                        component.getVersion(),
                        metadata));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to generate declared components", e);
        }
    }

    private List<Component> tryGenerateComponents() throws IOException {
        Yaml yaml = new Yaml();

        List<Component> components = new ArrayList<>();

        ClassPathUtils.consumeAsPaths(Thread.currentThread().getContextClassLoader(),
                COMPONENTS_DIR,
                path -> {
                    if (!Files.exists(path)) {
                        return;
                    }
                    try (final Stream<Path> pathStream = Files.walk(path)) {
                        pathStream.filter(Files::isRegularFile)
                                .forEach(file -> tryGenerateComponentFromFile(yaml, file)
                                        .ifPresent(components::add));
                    } catch (IOException e) {
                        throw new UncheckedIOException("Unable to generate component from resource", e);
                    }
                });

        return components;
    }

    private static Optional<Component> tryGenerateComponentFromFile(Yaml yaml, Path file) {

        String component;
        try {
            component = Files.readString(file);
        } catch (IOException e) {
            LOGGER.warn("Unable to read component file from {} file", file);
            return Optional.empty();
        }

        Map<String, Object> document = yaml.load(component);

        @SuppressWarnings("unchecked")
        Map<String, Object> spec = (Map<String, Object>) document.get("spec");
        String version = (String) spec.get("version");

        String type = (String) spec.get("type");

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) document
                .get("metadata");
        String name = (String) metadata.get("name");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> specMetadata = (List<Map<String, Object>>) spec
                .getOrDefault("metadata", Collections.emptyList());
        List<MetadataEntry> metadataEntries = new ArrayList<>();

        for (Map<String, Object> specMetadataItem : specMetadata) {
            String metadataItemName = (String) specMetadataItem.get("name");
            String metadataItemValue = (String) specMetadataItem
                    .get("value");
            metadataEntries
                    .add(new MetadataEntry(metadataItemName,
                            metadataItemValue));
        }
        return Optional.of(new Component(name, type, version, metadataEntries));
    }

    @BuildStep(onlyIfNot = { IsProduction.class })
    List<DevServicesResultBuildItem> devServices(
            DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchMode,
            List<DaprComponentBuildItem> componentBuildItems) {

        if (!config.enabled().get()) {
            return null;
        }

        List<Component> components = componentBuildItems.stream()
                .map(DaprComponentBuildItem::toComponent)
                .collect(Collectors.toList());

        Network.NetworkImpl network = Network.builder()
                .build();

        boolean dashboardEnabled = config.dashboard().enabled().get();
        Optional<Component> declaredStateStore = dashboardEnabled
                ? components.stream().filter(c -> PGSQL_STATE_STORE_TYPE.equals(c.getType())).findFirst()
                : Optional.empty();
        // the pgsql dev service is only needed when the user did not declare a postgresql state store
        boolean startPgsql = dashboardEnabled && declaredStateStore.isEmpty();

        List<DevServicesResultBuildItem> containers = new ArrayList<>();
        containers.add(configureDaprContainer(config, launchMode, network, components, startPgsql));

        if (dashboardEnabled) {
            containers.add(configureDashboardWorkflowContainer(network, declaredStateStore));
            if (startPgsql) {
                containers.add(configurePgsqlContainer(network));
            }
        }

        return containers;
    }

    private static DevServicesResultBuildItem configureDaprContainer(DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchMode, Network network, List<Component> components, boolean usePgsqlDevService) {
        DevServicesResultBuildItem.OwnedServiceBuilder<Startable> builder = DevServicesResultBuildItem.owned()
                .serviceName(FEATURE)
                .feature(FEATURE)
                .startable(new Supplier<Startable>() {
                    @Override
                    public Startable get() {
                        return new DaprContainerStartable(config,
                                launchMode.getLaunchMode(), network, components, usePgsqlDevService);
                    }
                });

        if (usePgsqlDevService) {
            builder.dependsOnConfig(POSTGRESQL_PORT_PROPERTY, (startable, value) -> {
                LOGGER.info("Dapr statestore {} is running", PGSQL_STATE_STORE);
            });
        }

        return builder
                .postStartHook(startable -> {
                    DaprContainerStartable daprContainerStartable = (DaprContainerStartable) startable;
                    System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(daprContainerStartable.getGrpcPort()));
                    System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(daprContainerStartable.getHttpPort()));
                })
                .build();
    }

    private static DevServicesResultBuildItem configureDashboardWorkflowContainer(Network network,
            Optional<Component> declaredStateStore) {
        DevServicesResultBuildItem.OwnedServiceBuilder<Startable> builder = DevServicesResultBuildItem.owned()
                .serviceName(DASHBOARD_WORKFLOW)
                .feature(FEATURE)
                .startable(new Supplier<Startable>() {
                    @Override
                    public Startable get() {
                        DashboardContainerStartable container = new DashboardContainerStartable(network);
                        declaredStateStore.ifPresent(container::setupStateStore);
                        return container;
                    }
                });

        if (declaredStateStore.isEmpty()) {
            builder.dependsOnConfig(POSTGRESQL_PORT_PROPERTY, (Startable startable, String value) -> {
                LOGGER.info("Running dependsOnConfig for DashboardContainerStartable container");
                DashboardContainerStartable d = (DashboardContainerStartable) startable;
                d.setupStateStore();
            });
        }

        return builder
                .configProvider(Map.of(DAPR_DASHBOARD_WORKFLOW_URL, startable -> {
                    DashboardContainerStartable container = (DashboardContainerStartable) startable;
                    return "http://127.0.0.1:" + container.getMappedPort(INTERNAL_DAPR_DASHBOARD_WORKFLOW_PORT);
                }))
                .build();
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
