package io.quarkiverse.dapr.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Network;
import org.yaml.snakeyaml.Yaml;

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
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.runtime.util.ClassPathUtils;

public class DevServicesDaprProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevServicesDaprProcessor.class);
    private static final String FEATURE = "dapr";
    private static final String DAPR_GRPC_PORT_PROPERTY = "dapr.grpc.port";
    private static final String DAPR_HTTP_PORT_PROPERTY = "dapr.http.port";
    private static final int DAPR_DEFAULT_PORT = 8080;
    private static final String COMPONENTS_DIR = "components";

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

        boolean launchModeTest = launchMode.isTest();
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchModeTest ? "(test) " : "") + "Dev Services for Dapr starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {

            devService = startDapr(dockerStatusBuildItem, config, launchModeTest);

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

    private DevServicesResultBuildItem.RunningDevService startDapr(
            DockerStatusBuildItem dockerStatusBuildItem,
            DaprDevServiceBuildTimeConfig config, boolean launchModeTest) {

        if (!config.enabled().orElse(false)) {
            LOGGER.debug("Not starting Dev Services for Dapr, as it has been disabled in the config.");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            LOGGER.warn("Docker isn't working.");
            return null;
        }

        DaprContainer dapr = new DaprContainer(config.daprdImage())
                .withAppName("local-dapr-app")
                .withAppPort(QuarkusPorts.http(launchModeTest))
                .withDaprLogLevel(DaprContainer.DaprLogLevel.debug)
                .withAppChannelAddress("host.testcontainers.internal");

        Yaml yaml = new Yaml();
        try {
            List<Component> components = tryGenerateComponentsFromResources(yaml);
            for (Component component : components) {
                dapr = dapr.withComponent(component);
            }
        } catch (IOException e) {
            LOGGER.warn("Was not possible to add custom components to Dapr Sidecar", e);
        }

        createDaprNetwork();

        dapr.withNetwork(getNetwork());

        Testcontainers.exposeHostPorts(QuarkusPorts.http(launchModeTest),
                QuarkusPorts.grpc(launchModeTest));

        dapr.start();

        System.setProperty(DAPR_GRPC_PORT_PROPERTY, Integer.toString(dapr.getGRPCPort()));
        System.setProperty(DAPR_HTTP_PORT_PROPERTY, Integer.toString(dapr.getHTTPPort()));

        return new DevServicesResultBuildItem.RunningDevService(FEATURE,
                dapr.getContainerId(),
                new ContainerShutdownCloseable(dapr, "Dapr"),
                Map.of());

    }

    private static void createDaprNetwork() {
        List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance()
                .client()
                .listNetworksCmd()
                .withNameFilter(FEATURE)
                .exec();
        if (networks.isEmpty()) {
            Network.builder()
                    .createNetworkCmdModifier(cmd -> cmd.withName(FEATURE))
                    .build()
                    .getId();
        }
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

    private static Network getNetwork() {
        return new Network() {
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
        };
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
