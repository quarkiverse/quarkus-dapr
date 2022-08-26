package io.quarkiverse.dapr.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * DaprConfig
 *
 * @author nayan
 * @date 2022/8/11 13:44
 */
@ConfigRoot(prefix = "", name = "dapr", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class DaprConfig {

    /**
     * pub sub config
     */
    @ConfigItem
    public DaprPubSubConfig pubSub;

    @ConfigGroup
    public static class DaprPubSubConfig {
        /**
         * pub sub name
         */
        @ConfigItem(defaultValue = "redis")
        public String name;

        /**
         * pub sub type
         */
        @ConfigItem(defaultValue = "redis")
        public String type;

        /**
         * pub sub default metadata
         */
        @ConfigItem
        public Map<String, String> metadata;
    }
}
