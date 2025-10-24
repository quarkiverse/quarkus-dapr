package io.quarkiverse.dapr.deployment.items;

import java.util.Map;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

public final class TopicBuildItem extends MultiBuildItem {
    private final String pubsubName;
    private final String topicName;
    private final String route;
    private final String match;
    private final int priority;
    private final Map<String, String> metadata;

    public TopicBuildItem(String pubsubName, String topicName, String route, String match, int priority,
            Map<String, String> metadata) {
        this.pubsubName = pubsubName;
        this.topicName = topicName;
        this.route = route;
        this.match = Objects.requireNonNullElse(match, "");
        this.priority = priority;
        this.metadata = metadata;
    }

    public String getPubsubName() {
        return pubsubName;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getRoute() {
        return route;
    }

    public String getMatch() {
        return match;
    }

    public int getPriority() {
        return priority;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
