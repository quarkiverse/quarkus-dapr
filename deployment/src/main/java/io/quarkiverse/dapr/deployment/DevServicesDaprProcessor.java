package io.quarkiverse.dapr.deployment;

import io.diagrid.dapr.DaprContainer;
import io.diagrid.dapr.DaprContainer.Component;
import io.diagrid.dapr.DaprContainer.MetadataEntry;
import io.quarkiverse.dapr.config.DaprDevServiceBuildTimeConfig;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.runtime.util.ClassPathUtils;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Network;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DevServicesDaprProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevServicesDaprProcessor.class);
    private static final String FEATURE = "dapr";
    private static final String DAPR_GRPC_PORT_PROPERTY = "dapr.grpc.port";
    private static final String DAPR_HTTP_PORT_PROPERTY = "dapr.http.port";
    private static final int DAPR_DEFAULT_PORT = 8080;
    private static final int DAPRD_HTTP_PORT = 3500;
    private static final int DAPRD_GRPC_PORT = 50001;
    private static final String COMPONENTS_DIR = "components";
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-dapr";
    private static final ContainerLocator daprContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL,
            DAPRD_HTTP_PORT);

    static volatile DevServicesResultBuildItem.RunningDevService devService;
    static volatile DaprDevServiceBuildTimeConfig cfg;
    static volatile boolean first = true;

    @BuildStep(onlyIfNot = { IsNormal.class })
    DevServicesResultBuildItem devServices(
            DockerStatusBuildItem dockerStatusBuildItem,
            DaprDevServiceBuildTimeConfig config,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            LaunchModeBuildItem launchMode,
            CuratedApplicationShutdownBuildItem closeBuildItem) {

        if (devService != null) {
            boolean shouldShutdownTheServer = !config.equals(cfg);
            if (!shouldShutdownTheServer) {
                return devService.toBuildItem();
            }
            shutdownDapr();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Dev Services for Dapr starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {

            devService = startDapr(dockerStatusBuildItem, config, launchMode);

            if (devService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }

        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return null;
        }

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devService != null) {
                    shutdownDapr();
                }
                first = true;
                devService = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = config;

        if (devService.isOwner()) {
            LOGGER.info("Dev Services for Dapr started");
        }
        return devService.toBuildItem();
    }

    private String configurePortBindings(int containerPort, int bindPort) {
        return String.format("0.0.0.0:%d:%d", containerPort, bindPort);
    }

    private DevServicesResultBuildItem.RunningDevService startDapr(
            DockerStatusBuildItem dockerStatusBuildItem,
            DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchMode) {

        if (!config.enabled.orElse(false)) {
            LOGGER.debug("Not starting Dev Services for Dapr, as it has been disabled in the config.");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            LOGGER.warn("Docker isn't working.");
            return null;
        }

        final Optional<ContainerAddress> maybeContainerAddress = daprContainerLocator.locateContainer(
                config.serviceName,
                config.shared,
                launchMode.getLaunchMode());

        final Supplier<DevServicesResultBuildItem.RunningDevService> defaultDaprContainer = () -> {
            DaprContainer dapr = new DaprContainer("daprio/daprd:1.12.2")
                    .withAppName("local-dapr-app")
                    .withAppPort(DAPR_DEFAULT_PORT)
                    .withDaprLogLevel(DaprContainer.DaprLogLevel.debug)
                    .withAppChannelAddress("host.testcontainers.internal");

            if (config.serviceName != null) {
                dapr.withLabel(DEV_SERVICE_LABEL, config.serviceName);
            }

            Yaml yaml = new Yaml();

            try {
                List<Component> components = tryGenerateComponentsFromResources(yaml);
                for (Component component : components) {
                    dapr = dapr.withComponent(component);
                }
            } catch (IOException e) {
                LOGGER.warn("Was not possible to add custom components to Dapr Sidecar", e);
            }

            final Supplier<Integer> getGrpcPort = () -> !config.shared ? config.grpcPort : DAPRD_GRPC_PORT;
            final Supplier<Integer> getHttpPort = () -> !config.shared ? config.httpPort : DAPRD_HTTP_PORT;
            // This is necessary when the user wants to use Dev Services shared
            dapr.setPortBindings(List.of(
                    configurePortBindings(getGrpcPort.get(), DAPRD_HTTP_PORT),
                    configurePortBindings(getHttpPort.get(), DAPRD_GRPC_PORT)));

            List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd()
                    .withNameFilter("dapr").exec();
            if (networks.isEmpty()) {
                Network.builder()
                        .createNetworkCmdModifier(cmd -> cmd.withName(FEATURE))
                        .build().getId();
            }

            dapr.withNetwork(new Network() {
                @Override
                public String getId() {
                    return FEATURE;
                }

                @Override
                public void close() {

                }

                @Override
                public Statement apply(Statement base, Description description) {
                    return null;
                }
            });

            dapr.start();

            Testcontainers.exposeHostPorts(DAPR_DEFAULT_PORT);
            System.setProperty(DAPR_GRPC_PORT_PROPERTY, Integer.toString(dapr.getGRPCPort()));
            System.setProperty(DAPR_HTTP_PORT_PROPERTY, Integer.toString(dapr.getHTTPPort()));

            return new DevServicesResultBuildItem.RunningDevService(FEATURE,
                    dapr.getContainerId(),
                    new ContainerShutdownCloseable(dapr, "Dapr"),
                    Map.of());
        };

        return maybeContainerAddress
                .map(containerAddress -> new DevServicesResultBuildItem.RunningDevService(
                        FEATURE,
                        containerAddress.getId(),
                        null,
                        Map.of()))
                .orElseGet(defaultDaprContainer);

    }

    private static List<Component> tryGenerateComponentsFromResources(Yaml yaml) throws IOException {
        List<Component> components = new ArrayList<>();
        ClassPathUtils.consumeAsPaths(Thread.currentThread().getContextClassLoader(),
                COMPONENTS_DIR,
                path -> {
                    if (!Files.exists(path)) {
                        return;
                    }
                    try (final Stream<Path> pathStream = Files.walk(path)) {
                        pathStream.filter(Files::isRegularFile)
                                .forEach(p -> tryGenerateComponentFromFile(yaml, p).ifPresent(components::add));
                    } catch (IOException e) {
                        LOGGER.warn("Error while adding components to Dapr Sidecar");
                    }
                });
        return components;
    }

    private static Optional<Component> tryGenerateComponentFromFile(Yaml yaml, Path p) {

        String componentAsString;
        try {
            componentAsString = Files.readString(p);
        } catch (IOException e) {
            LOGGER.warn("Error while reading file {}", p);
            return Optional.empty();
        }

        Map<String, Object> map = yaml.load(componentAsString);

        Map<String, Object> spec = (Map<String, Object>) map.get("spec");
        String type = (String) spec.get("type");
        Map<String, Object> metadata = (Map<String, Object>) map
                .get("metadata");
        String name = (String) metadata.get("name");
        List<Map<String, Object>> specMetadata = (List<Map<String, Object>>) spec
                .getOrDefault("metadata", Collections.emptyMap());
        ArrayList<MetadataEntry> metadataEntries = new ArrayList<>();

        for (Map<String, Object> specMetadataItem : specMetadata) {
            String metadataItemName = (String) specMetadataItem.get("name");
            String metadataItemValue = (String) specMetadataItem
                    .get("value");
            metadataEntries
                    .add(new MetadataEntry(metadataItemName,
                            metadataItemValue));
        }
        return Optional.of(new Component(name, type, metadataEntries));
    }

    private void shutdownDapr() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                LOGGER.error("Failed to stop the Dapr Sidecar", e);
            } finally {
                devService = null;
            }
        }
    }
}
