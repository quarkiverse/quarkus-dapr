package io.quarkus.reactive.messaging.dapr;

public class DaprConfig {

    private final String pubsubName;
    private final String topic;

    public DaprConfig(String pubsubName, String topic) {
        this.pubsubName = pubsubName;
        this.topic = topic;
    }

    public String pubsubName() {
        return pubsubName;
    }

    public String topic() {
        return topic;
    }

    public String key() {
        return String.format("%s.%s", pubsubName, topic);
    }

    public String path() {
        return "/" + key();
    }
}
