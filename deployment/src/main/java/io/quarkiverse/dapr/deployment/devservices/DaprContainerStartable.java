package io.quarkiverse.dapr.deployment.devservices;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PASSWORD;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PGSQL_NETWORK_ALIAS;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.PGSQL_STATE_STORE;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.POSTGRESQL_PORT;
import static io.quarkiverse.dapr.deployment.devservices.StateStoreContainerStartable.USERNAME;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.yaml.snakeyaml.Yaml;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.MetadataEntry;
import io.quarkiverse.dapr.deployment.DaprProcessor;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.runtime.util.ClassPathUtils;

public class DaprContainerStartable extends DaprContainer implements Startable {

    private static final String COMPONENTS_DIR = "components";
    private static final Logger LOGGER = LoggerFactory.getLogger(DaprContainerStartable.class);

    private final DaprDevServiceBuildTimeConfig config;

    public DaprContainerStartable(DaprDevServiceBuildTimeConfig config, int appPort, int grpcPort, Network network) {
        super(DockerImageName.parse(config.daprdImage()).asCompatibleSubstituteFor(
                DAPR_RUNTIME_IMAGE_TAG));

        this.config = config;

        super.withAppName("local-dapr-app")
                .withAppPort(appPort)
                .withDaprLogLevel(DaprLogLevel.DEBUG)
                .withNetwork(network)
                .withAppChannelAddress("host.testcontainers.internal");

        Testcontainers.exposeHostPorts(appPort, grpcPort);

        generateDeclaredComponents();
    }

    private void generateDeclaredComponents() {
        try {
            List<Component> components = tryGenerateComponents();
            for (Component component : components) {
                super.withComponent(component);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to generate declared components", e);
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
                .getOrDefault("metadata", Collections.emptyMap());
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
