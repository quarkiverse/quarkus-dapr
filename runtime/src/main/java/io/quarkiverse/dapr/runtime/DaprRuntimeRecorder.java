package io.quarkiverse.dapr.runtime;

import java.util.Map;

import io.quarkiverse.dapr.config.ConfigUtils;
import io.quarkiverse.dapr.core.DaprRuntime;
import io.quarkus.runtime.annotations.Recorder;

/**
 * DaprRuntimeRecorder
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
@Recorder
public class DaprRuntimeRecorder {

    public void subscribeToTopics(String pubSubName, String topicName, String match, int priority, String route,
            Map<String, String> metadata) {
        String topicNameValue = ConfigUtils.getConfigValueIfNecessary(topicName, true);
        DaprRuntime.getInstance().addSubscribedTopic(pubSubName, topicNameValue, match, priority, route, metadata);
    }
}
