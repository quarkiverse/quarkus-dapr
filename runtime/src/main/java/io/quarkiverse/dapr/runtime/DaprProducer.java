package io.quarkiverse.dapr.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.quarkiverse.dapr.core.SyncDaprClient;
import io.quarkiverse.dapr.serializer.JacksonDaprObjectSerializer;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

/**
 * DaprProducer
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
@ApplicationScoped
public class DaprProducer {

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
    public DaprClient daprClient(JacksonDaprObjectSerializer serializer) {
        return new DaprClientBuilder()
                .withObjectSerializer(serializer)
                .withStateSerializer(serializer)
                .build();
    }

    @Produces
    @DefaultBean
    @Startup
    @Singleton
    @Unremovable
    public SyncDaprClient syncDaprClient(DaprClient client) {
        return new SyncDaprClient(client);
    }
}
