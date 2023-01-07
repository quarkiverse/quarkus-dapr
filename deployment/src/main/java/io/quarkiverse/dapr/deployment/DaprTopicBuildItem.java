package io.quarkiverse.dapr.deployment;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * DaprTopicBuildItem
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public final class DaprTopicBuildItem extends MultiBuildItem {
    private final String pubSubName;
    private final String topicName;
    private final String route;
    private final String match;
    private final int priority;
    private final Map<String, String> metadata;

    public DaprTopicBuildItem(String pubSubName, String topicName, String route, String match, int priority,
            Map<String, String> metadata) {
        this.pubSubName = pubSubName;
        this.topicName = topicName;
        this.route = route;
        this.match = match;
        this.priority = priority;
        this.metadata = metadata;
    }

    public String getPubSubName() {
        return pubSubName;
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
