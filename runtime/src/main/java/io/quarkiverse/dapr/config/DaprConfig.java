package io.quarkiverse.dapr.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * DaprConfig
 *
 * @author nayan
 * @date 2022/8/11 13:44
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.dapr")
public interface DaprConfig {

    /**
     * default pub sub config
     */
    @WithDefault("redis")
    String defaultPubSub();

    /**
     * pub sub config
     */
    Map<String, DaprPubSubConfig> pubSub();

    WorkflowsConfig workflow();

    @ConfigGroup
    interface DaprPubSubConfig {

        /**
         * pub sub type
         */
        @WithDefault("redis")
        String type();

        /**
         * publish pub sub default metadata
         */
        Map<String, String> publishMetadata();

        /**
         * consume pub sub default metadata
         */
        Map<String, String> consumeMetadata();

    }

}
