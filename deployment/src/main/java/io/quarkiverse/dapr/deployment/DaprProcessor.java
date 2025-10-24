package io.quarkiverse.dapr.deployment;

import static io.quarkiverse.dapr.config.ConfigUtils.getConfigValueIfNecessary;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.dapr.actors.runtime.ActorRuntimeConfig;
import io.dapr.client.domain.CloudEvent;
import io.quarkiverse.dapr.core.DaprTopicRoutes;
import io.quarkiverse.dapr.core.DaprTopicRule;
import io.quarkiverse.dapr.core.DaprTopicSubscription;
import io.quarkiverse.dapr.core.SubscriptionBuilder;
import io.quarkiverse.dapr.core.TopicKey;
import io.quarkiverse.dapr.deployment.items.AfterSubscribeRouteBuildItem;
import io.quarkiverse.dapr.deployment.items.TopicBuildItem;
import io.quarkiverse.dapr.endpoint.actor.ActorDeactivateHandler;
import io.quarkiverse.dapr.endpoint.actor.ActorInvokeMethodHandler;
import io.quarkiverse.dapr.endpoint.actor.ActorInvokeReminderHandler;
import io.quarkiverse.dapr.endpoint.actor.ActorInvokeTimerHandler;
import io.quarkiverse.dapr.endpoint.dapr.AbstractDaprHandler;
import io.quarkiverse.dapr.endpoint.dapr.DaprConfigHandler;
import io.quarkiverse.dapr.endpoint.dapr.SubscriberRecorder;
import io.quarkiverse.dapr.endpoint.health.DaprHealthzHandler;
import io.quarkiverse.dapr.jackson.DaprJacksonModuleCustomizer;
import io.quarkiverse.dapr.resteasy.CloudEventReader;
import io.quarkiverse.dapr.runtime.DaprProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

class DaprProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaprProcessor.class);
    private static final String FEATURE = "dapr";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void addHealthzEndpoint(BuildProducer<RouteBuildItem> routeBuildItemBuildProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        routeBuildItemBuildProducer.produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new DaprHealthzHandler()));
    }

    @BuildStep
    void addDaprEndpoint(BuildProducer<RouteBuildItem> routeBuildItemBuildProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        routeBuildItemBuildProducer.produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new DaprConfigHandler()));
        routeBuildItemBuildProducer.produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new DaprHealthzHandler()));
    }

    @BuildStep
    void addActorEndpoint(BuildProducer<RouteBuildItem> routeBuildItemBuildProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        routeBuildItemBuildProducer
                .produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new ActorDeactivateHandler()));
        routeBuildItemBuildProducer
                .produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new ActorInvokeMethodHandler()));
        routeBuildItemBuildProducer
                .produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new ActorInvokeTimerHandler()));
        routeBuildItemBuildProducer
                .produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new ActorInvokeReminderHandler()));
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(DaprProducer.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(DaprJacksonModuleCustomizer.class));
    }

    @BuildStep
    void vertxProviders(BuildProducer<MessageBodyReaderBuildItem> providers) {
        providers.produce(new MessageBodyReaderBuildItem(CloudEventReader.class.getName(), CloudEvent.class.getName(),
                Collections.singletonList(MediaType.APPLICATION_JSON)));
    }

    @BuildStep
    @Produce(AfterSubscribeRouteBuildItem.class)
    @Record(ExecutionTime.STATIC_INIT)
    RouteBuildItem addSubscribeHandlerRoute(List<TopicBuildItem> topicBuildItems, SubscriberRecorder recorder)
            throws JsonProcessingException {

        Map<TopicKey, SubscriptionBuilder> builders = new HashMap<>();

        for (TopicBuildItem buildItem : topicBuildItems) {

            String topicName = getConfigValueIfNecessary(buildItem.getTopicName(), true);
            String pubsubName = getConfigValueIfNecessary(buildItem.getPubsubName(), true);

            TopicKey key = new TopicKey(pubsubName, topicName);

            SubscriptionBuilder possibleBuilder = builders.get(key);
            if (possibleBuilder != null) {
                LOGGER.warn(
                        "Duplicate topic subscription found for pubsubName '{}' and topic '{}'. The first subscription will be used.",
                        pubsubName, topicName);
                continue;
            }

            SubscriptionBuilder builder = builders.getOrDefault(key, new SubscriptionBuilder(pubsubName, topicName));

            builders.put(key, configureBuilderWithTopic(builder, buildItem));
        }

        List<DaprTopicSubscription> subscriptions = builders.values().stream().map(SubscriptionBuilder::build)
                .collect(Collectors.toList());

        String json = DeploymentObjectMapper.getInstance().writeValueAsString(subscriptions);

        return RouteBuildItem.builder()
                .displayOnNotFoundPage()
                .routeFunction("/dapr/subscribe", recorder.route())
                .handler(recorder.handler(json))
                .build();
    }

    private SubscriptionBuilder configureBuilderWithTopic(SubscriptionBuilder builder, TopicBuildItem topicBuildItem) {

        String match = getConfigValueIfNecessary(topicBuildItem.getMatch(), true);

        if (match != null && !match.isEmpty()) {
            builder.addRule(topicBuildItem.getRoute(), match, topicBuildItem.getPriority());
        } else if (Objects.isNull(builder.getDefaultPath())) {
            builder.setDefaultPath(topicBuildItem.getRoute());
        }

        Map<String, String> metadata = topicBuildItem.getMetadata();
        if (metadata != null && !metadata.isEmpty()) {
            builder.setMetadata(metadata);
        }

        return builder;
    }

    private RouteBuildItem getDaprRouteBuildItem(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            AbstractDaprHandler handler) {
        return nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute(handler.baseRoute(), handler.subRoute())
                .handler(handler)
                .displayOnNotFoundPage()
                .build();
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildProducer) {
        ReflectiveClassBuildItem buildItem = ReflectiveClassBuildItem.builder(DaprTopicSubscription.class.getName(),
                ActorRuntimeConfig.class.getName(),
                CloudEvent.class.getName(), DaprTopicRule.class.getName(), DaprTopicRoutes.class.getName())
                .methods(true)
                .fields(true)
                .build();
        reflectiveClassBuildProducer.produce(buildItem);
    }

}
