package io.quarkiverse.dapr.deployment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkiverse.dapr.core.DaprTopicRoutes;
import io.quarkiverse.dapr.core.DaprTopicRule;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dapr.Topic;
import io.dapr.actors.runtime.ActorRuntimeConfig;
import io.dapr.client.domain.CloudEvent;
import io.quarkiverse.dapr.config.DaprConfig;
import io.quarkiverse.dapr.core.DaprTopicSubscription;
import io.quarkiverse.dapr.endpoint.actor.ActorDeactivateHandler;
import io.quarkiverse.dapr.endpoint.actor.ActorInvokeMethodHandler;
import io.quarkiverse.dapr.endpoint.actor.ActorInvokeReminderHandler;
import io.quarkiverse.dapr.endpoint.actor.ActorInvokeTimerHandler;
import io.quarkiverse.dapr.endpoint.dapr.AbstractDaprHandler;
import io.quarkiverse.dapr.endpoint.dapr.DaprConfigHandler;
import io.quarkiverse.dapr.endpoint.dapr.DaprSubscribeHandler;
import io.quarkiverse.dapr.endpoint.health.DaprHealthzHandler;
import io.quarkiverse.dapr.jackson.DaprJacksonModuleCustomizer;
import io.quarkiverse.dapr.resteasy.CloudEventReader;
import io.quarkiverse.dapr.runtime.DaprProducer;
import io.quarkiverse.dapr.runtime.DaprRuntimeRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

/**
 * DaprProcessor
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
class DaprProcessor {

    private static final Logger log = LoggerFactory.getLogger(DaprProcessor.class);

    private static final String FEATURE = "dapr";
    private static final DotName DAPR_TOPIC = DotName.createSimple(Topic.class.getName());
    private static final DotName RESTEASY_PATH = DotName.createSimple("jakarta.ws.rs.Path");
    private static final DotName RESTEASY_POST = DotName.createSimple("jakarta.ws.rs.POST");
    private static final TypeReference<HashMap<String, String>> MAP_TYPE = new TypeReference<HashMap<String, String>>() {
    };
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

        routeBuildItemBuildProducer.produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new DaprSubscribeHandler()));
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
    void daprTopicBuildItems(BuildProducer<DaprTopicBuildItem> topicProducer, CombinedIndexBuildItem indexBuildItem,
            DaprConfig daprConfig) {
        Map<String, DaprConfig.DaprPubSubConfig> pubSubConfigMap = Optional.ofNullable(daprConfig.pubSub())
                .orElse(new HashMap<>(16));
        for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(DAPR_TOPIC)) {
            if (i.target().kind() == AnnotationTarget.Kind.METHOD) {

                MethodInfo methodInfo = i.target().asMethod();
                ClassInfo classInfo = methodInfo.declaringClass();
                Optional<AnnotationInstance> methodPostOp = methodInfo.annotations().stream()
                        .filter(annotation -> annotation.name()
                                .equals(RESTEASY_POST))
                        .findFirst();

                methodPostOp.ifPresent(mp -> {
                    Optional<AnnotationInstance> classPathOp = classInfo.annotationsMap().entrySet().stream()
                            .filter(entry -> entry.getKey()
                                    .equals(RESTEASY_PATH))
                            .map(Map.Entry::getValue)
                            .flatMap(Collection::stream)
                            .filter(a -> a.target()
                                    .kind() == AnnotationTarget.Kind.CLASS)
                            .findFirst();

                    String methodPath = null;
                    AnnotationInstance topic = null;
                    for (AnnotationInstance annotation : methodInfo.annotations()) {
                        DotName name = annotation.name();
                        if (name.equals(RESTEASY_PATH)) {
                            methodPath = annotation.value().asString();
                        }
                        if (name.equals(DAPR_TOPIC)) {
                            topic = annotation;
                        }
                    }

                    if (classPathOp.isPresent() || Objects.nonNull(methodPath)) {
                        DaprTopicBuildItem item = buildDaprTopicBuildItem(daprConfig, pubSubConfigMap, classInfo, classPathOp,
                                methodPath, topic);
                        topicProducer.produce(item);
                    }
                });
            }
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void addTopic(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses, DaprRuntimeRecorder daprRuntimeRecorder,
            List<DaprTopicBuildItem> daprTopicBuildItems) {
        for (DaprTopicBuildItem item : daprTopicBuildItems) {
            daprRuntimeRecorder.subscribeToTopics(
                    item.getPubSubName(),
                    item.getTopicName(),
                    item.getMatch(),
                    item.getPriority(),
                    item.getRoute(),
                    item.getMetadata());
        }
    }

    private static DaprTopicBuildItem buildDaprTopicBuildItem(DaprConfig daprConfig,
            Map<String, DaprConfig.DaprPubSubConfig> pubSubConfigMap,
            ClassInfo classInfo,
            Optional<AnnotationInstance> classPathOp,
            String methodPath,
            AnnotationInstance topic) {
        String path = "";
        if (classPathOp.isPresent()) {
            path += classPathOp.get().value().asString();
        }
        if (StringUtils.isNotBlank(methodPath)) {
            path += methodPath;
        }
        path = path.replaceAll("//", "/");
        String pubsubName = Optional.ofNullable(topic.value("pubsubName"))
                .map(AnnotationValue::asString)
                .orElse(daprConfig.defaultPubSub());

        String topicName = topic.value("name").asString();

        log.info(String.format("topic name %s", topicName));
        String ruleMatch = Optional.ofNullable(topic.value("rule"))
                .map(AnnotationValue::asNested)
                .map(a -> a.value("match"))
                .map(AnnotationValue::asString)
                .orElse("");
        int rulePriority = Optional.ofNullable(topic.value("rule"))
                .map(AnnotationValue::asNested)
                .map(a -> a.value("priority"))
                .map(AnnotationValue::asInt)
                .orElse(0);

        AnnotationValue metadataValue = topic.value("metadata");

        Map<String, String> consumeMetadata = Optional.ofNullable(pubSubConfigMap.get(pubsubName))
                .map(a -> new HashMap(a.consumeMetadata()))
                .orElse(new HashMap<>(8));
        Map<String, String> topicMetadata = Optional.ofNullable(metadataValue)
                .map(a -> {
                    try {
                        return OBJECT_MAPPER.readValue(a.asString(), MAP_TYPE);
                    } catch (JsonProcessingException e) {
                        log.error("dapr topic metadata to path error in class:{},topicName:{}",
                                classInfo.name().toString(),
                                topicName, e);
                        return null;
                    }
                })
                .orElse(new HashMap<>(8));
        consumeMetadata.putAll(topicMetadata);
        DaprTopicBuildItem item = new DaprTopicBuildItem(
                pubsubName,
                topicName,
                path,
                ruleMatch,
                rulePriority,
                consumeMetadata);
        return item;
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
