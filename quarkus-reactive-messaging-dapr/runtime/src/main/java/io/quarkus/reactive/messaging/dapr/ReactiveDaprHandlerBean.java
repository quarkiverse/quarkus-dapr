package io.quarkus.reactive.messaging.dapr;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.reactive.messaging.dapr.config.ConfigReader;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class ReactiveDaprHandlerBean {

    private static final Logger log = LoggerFactory.getLogger(ReactiveDaprHandlerBean.class);
    private final Map<String, Bundle<DaprMessage<?>>> processors = new HashMap<>();

    @Inject
    ConfigReader config;

    @PostConstruct
    void init() {
        configs().forEach(this::addProcessor);
    }

    public Collection<DaprConfig> configs() {
        return config.getConfigs();
    }

    public Multi<DaprMessage<?>> getProcessor(DaprConnectorIncomingConfiguration incomingConfig) {
        String key = key(incomingConfig);
        return processors.get(key).getProcessor();
    }

    public void handle(RoutingContext event) {
        log.info("processor keys: {}", processors.keySet());
        Bundle<DaprMessage<?>> bundle = processors.get(key(event));
        if (bundle != null) {
            MultiEmitter<? super DaprMessage<?>> emitter = bundle.getEmitter();
            handleRequest(event, emitter);
        } else {
            event.response().setStatusCode(404).end();
        }
    }

    private String key(RoutingContext event) {
        String path = event.currentRoute().getPath();
        log.info("key from RoutingContext is {}", path);
        return path;
    }

    private void onUnexpectedError(RoutingContext event, Throwable error, String message) {
        if (!event.response().ended()) {
            event.response().setStatusCode(500).end("Unexpected error while processing the message");
            log.error(message, error);
        }
    }

    private void handleRequest(RoutingContext event, MultiEmitter<? super DaprMessage<?>> emitter) {
        log.info("handling request: {}", event.currentRoute().getPath());
        if (emitter == null) {
            onUnexpectedError(event, null, "No consumer subscribed for messages sent to Reactive Messaging Dapr");
        } else {
            try {
                emitter.emit(new DaprMessage<>(event.getBody(), () -> {
                    if (!event.response().ended()) {
                        event.response().setStatusCode(202).end();
                    }
                },
                        error -> onUnexpectedError(event, error, "Failed to process message.")));
            } catch (Exception any) {
                onUnexpectedError(event, any, "Emitting message failed");
            }
        }
    }

    private void addProcessor(DaprConfig daprConfig) {
        Bundle<DaprMessage<?>> bundle = new Bundle<>();
        Multi<DaprMessage<?>> processor = Multi.createFrom()
                .<DaprMessage<?>> emitter(bundle::setEmitter, BackPressureStrategy.BUFFER);
        bundle.setProcessor(processor);
        Bundle<DaprMessage<?>> previousProcessor = processors.put(key(daprConfig), bundle);
        if (previousProcessor != null) {
            throw new IllegalStateException("incoming config are duplicated " + description(daprConfig));
        }
    }

    private String description(final DaprConfig daprConfig) {
        return String.format("pubsubName: %s, topic: %s", daprConfig.pubsubName(), daprConfig.topic());
    }

    private String key(final DaprConnectorIncomingConfiguration incomingConfig) {
        String topic = incomingConfig.getTopic();
        String pubsubName = incomingConfig.getPubsubName();
        return String.format("/%s/%s", pubsubName, topic);
    }

    private String key(final DaprConfig daprConfig) {
        String pubsubName = daprConfig.pubsubName();
        String topic = daprConfig.topic();
        return String.format("/%s/%s", pubsubName, topic);
    }
}
