package io.quarkus.reactive.messaging.dapr;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.dapr.client.DaprClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public class DaprSink {

    private final DaprClient daprClient;
    private final String pubsubName;
    private final String topic;
    private final SubscriberBuilder<Message<?>, Void> subscriber;

    public DaprSink(DaprClient daprClient, String pubsubName, String topic) {
        this.daprClient = daprClient;
        this.pubsubName = pubsubName;
        this.topic = topic;
        subscriber = ReactiveStreams.<Message<?>> builder().flatMapCompletionStage(this::publish).ignore();
    }

    private CompletionStage<Void> publish(Message<?> message) {
        // Duplicate the current Vert.x context (if any) so the client call and the resulting
        // ack/nack are dispatched on the same context, regardless of the thread the Dapr client
        // (OkHttp/gRPC) resumes on.
        Context context = DaprVertxContext.capture();
        return Uni.createFrom()
                .completionStage(() -> daprClient.publishEvent(pubsubName, topic, message.getPayload()).toFuture())
                .onItemOrFailure().transformToUni((unused, throwable) -> Uni.createFrom()
                        .completionStage(() -> DaprVertxContext.callOnContext(context,
                                () -> throwable != null ? message.nack(throwable) : message.ack())))
                .subscribeAsCompletionStage();
    }

    SubscriberBuilder<Message<?>, Void> sink() {
        return subscriber;
    }
}
