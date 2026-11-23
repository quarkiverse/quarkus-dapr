package io.quarkiverse.dapr.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.config.DaprConfig;
import io.quarkiverse.dapr.config.DaprRuntimeConfig;
import io.quarkiverse.dapr.core.SyncDaprClient;
import io.quarkiverse.dapr.serializer.JacksonDaprObjectSerializer;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;

/**
 * DaprProducer
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
@ApplicationScoped
public class DaprProducer {

    /**
     * Dapr SDK properties built from {@link DaprRuntimeConfig}, shared by every Dapr client created by the extension.
     */
    @Produces
    @DefaultBean
    @Singleton
    @Unremovable
    public Properties daprProperties(DaprRuntimeConfig config) {
        return new Properties(DaprRuntimeConfig.toPropertyOverrides(config));
    }

    @Produces
    @DefaultBean
    @Startup
    @Singleton
    @Unremovable
    public DaprWorkflowClient getDaprWorkflowClient(Properties properties) {
        return new DaprWorkflowClient(properties);
    }

    @Produces
    @DefaultBean
    @Startup
    @Singleton
    @Unremovable
    public JacksonDaprObjectSerializer jacksonDaprObjectSerializer(ObjectMapper objectMapper) {
        return new JacksonDaprObjectSerializer(objectMapper);
    }

    @Produces
    @DefaultBean
    @Startup
    @Singleton
    @Unremovable
    public DaprClient daprClient(JacksonDaprObjectSerializer serializer, DaprRuntimeConfig config) {
        return new DaprClientBuilder()
                .withObjectSerializer(serializer)
                .withStateSerializer(serializer)
                .withPropertyOverrides(DaprRuntimeConfig.toPropertyOverrides(config))
                .build();
    }

    @Produces
    @DefaultBean
    @Startup
    @Singleton
    @Unremovable
    public SyncDaprClient syncDaprClient(DaprClient client, DaprConfig config) {
        return new SyncDaprClient(client, config);
    }

    /**
     * Creates the {@link ActorRuntime} singleton with the configured properties before any application
     * code calls {@link ActorRuntime#getInstance()}, which would otherwise create it with the SDK defaults.
     */
    void initActorRuntime(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) StartupEvent ev,
            Properties properties) {
        ActorRuntime.getInstance(properties);
    }

    void onStop(@Observes ShutdownEvent ev, DaprClient client) {
        try {
            client.close();

        } catch (Throwable e) {

        }
    }
}
