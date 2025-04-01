package io.quarkus.reactive.messaging.dapr;

import org.eclipse.microprofile.reactive.messaging.Message;

public class DaprMessage<T> implements Message<T> {

    private final T payload;

    DaprMessage(T payload) {
        this.payload = payload;
    }

    @Override
    public T getPayload() {
        return payload;
    }
}
