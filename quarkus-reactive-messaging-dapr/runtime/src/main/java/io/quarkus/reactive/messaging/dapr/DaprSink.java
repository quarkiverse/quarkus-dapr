package io.quarkus.reactive.messaging.dapr;

import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.dapr.client.DaprClient;

public class DaprSink {

    private final DaprClient daprClient;
    private final String pubsubName;
    private final String topic;

    public DaprSink(DaprClient daprClient, String pubsubName, String topic) {
        this.daprClient = daprClient;
        this.pubsubName = pubsubName;
        this.topic = topic;
    }

    SubscriberBuilder<Message<?>, Void> sink() {
        return ReactiveStreams.<Message<?>> builder().flatMapCompletionStage(
                message -> CompletableFuture.supplyAsync(() -> daprClient.publishEvent(this.pubsubName, this.topic, message)))
                .ignore();
    }
}
