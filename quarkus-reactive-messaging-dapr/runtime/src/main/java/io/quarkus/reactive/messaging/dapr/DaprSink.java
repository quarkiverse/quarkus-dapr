package io.quarkus.reactive.messaging.dapr;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.dapr.client.DaprClient;
import io.smallrye.mutiny.Uni;

public class DaprSink {

    private final DaprClient daprClient;
    private final String pubsubName;
    private final String topic;
    private final SubscriberBuilder<Message<?>, Void> subscriber;

    public DaprSink(DaprClient daprClient, String pubsubName, String topic) {
        this.daprClient = daprClient;
        this.pubsubName = pubsubName;
        this.topic = topic;
        subscriber = ReactiveStreams.<Message<?>> builder().flatMapCompletionStage(message -> Uni.createFrom()
                .completionStage(
                        () -> this.daprClient.publishEvent(this.pubsubName, this.topic, message.getPayload()).toFuture())
                .onItemOrFailure().transform((unused, throwable) -> {
                    if (throwable != null) {
                        return Uni.createFrom().completionStage(message.nack(throwable).thenApply(x -> message));
                    }
                    return Uni.createFrom().completionStage(message.ack().thenApply(x -> message));
                }).subscribeAsCompletionStage()).ignore();
    }

    SubscriberBuilder<Message<?>, Void> sink() {
        return subscriber;
    }
}
