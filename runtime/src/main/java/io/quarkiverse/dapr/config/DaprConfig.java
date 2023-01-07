package io.quarkiverse.dapr.config;

import java.util.HashMap;
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
@ConfigRoot(name = "dapr", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class DaprConfig {

    /**
     * default pub sub config
     */
    @ConfigItem(defaultValue = "redis")
    public String defaultPubSub;

    /**
     * pub sub config
     */
    @ConfigItem
    public Map<String, DaprPubSubConfig> pubSub = new HashMap<>();

    @ConfigGroup
    public static class DaprPubSubConfig {

        /**
         * pub sub type
         */
        @ConfigItem(defaultValue = "redis")
        public String type;

        /**
         * publish pub sub default metadata
         */
        @ConfigItem
        public Map<String, String> publishMetadata = new HashMap<>();

        /**
         * consume pub sub default metadata
         */
        @ConfigItem
        public Map<String, String> consumeMetadata = new HashMap<>();

    }

}
