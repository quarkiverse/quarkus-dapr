package io.quarkus.reactive.messaging.dapr;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class Bundle<MessageType> {

    private Multi<DaprMessage<?>> processor;
    private MultiEmitter<? super DaprMessage<?>> emitter;

    public void setProcessor(Multi<DaprMessage<?>> processor) {
        this.processor = processor;
    }

    public void setEmitter(MultiEmitter<? super DaprMessage<?>> emitter) {
        this.emitter = emitter;
    }

    public MultiEmitter<? super DaprMessage<?>> getEmitter() {
        return emitter;
    }

    public Multi<DaprMessage<?>> getProcessor() {
        return processor;
    }
}
