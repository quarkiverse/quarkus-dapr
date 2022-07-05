package io.quarkiverse.dapr.deployment;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.jboss.jandex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dapr.Topic;
import io.dapr.actors.runtime.ActorRuntimeConfig;
import io.dapr.client.domain.CloudEvent;
import io.quarkiverse.dapr.core.DaprTopicSubscription;
import io.quarkiverse.dapr.endpoint.actor.ActorDeactivateHandler;
import io.quarkiverse.dapr.endpoint.actor.ActorInvokeMethodHandler;
import io.quarkiverse.dapr.endpoint.actor.ActorInvokeReminderHandler;
import io.quarkiverse.dapr.endpoint.actor.ActorInvokeTimerHandler;
import io.quarkiverse.dapr.endpoint.dapr.AbstractDaprHandler;
import io.quarkiverse.dapr.endpoint.dapr.DaprConfigHandler;
import io.quarkiverse.dapr.endpoint.dapr.DaprSubscribeHandler;
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
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
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
    private static final DotName RESTEASY_PATH = DotName.createSimple("javax.ws.rs.Path");
    private static final DotName RESTEASY_POST = DotName.createSimple("javax.ws.rs.POST");
    private static final TypeReference<HashMap<String, String>> MAP_TYPE = new TypeReference<HashMap<String, String>>() {
    };
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void addDaprEndpoint(BuildProducer<RouteBuildItem> routeBuildItemBuildProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        routeBuildItemBuildProducer.produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new DaprConfigHandler()));

        routeBuildItemBuildProducer.produce(getDaprRouteBuildItem(nonApplicationRootPathBuildItem, new DaprSubscribeHandler()));
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
    void vertxProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(CloudEventReader.class.getName()));
    }

    @BuildStep
    void daprTopicBuildItems(BuildProducer<DaprTopicBuildItem> topicProducer, CombinedIndexBuildItem indexBuildItem) {
        for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(DAPR_TOPIC)) {
            if (i.target().kind() == AnnotationTarget.Kind.METHOD) {

                MethodInfo methodInfo = i.target().asMethod();
                ClassInfo classInfo = methodInfo.declaringClass();
                Optional<AnnotationInstance> methodPostOp = methodInfo.annotations().stream()
                        .filter(annotation -> annotation.name().equals(RESTEASY_POST))
                        .findFirst();

                methodPostOp.ifPresent(mp -> {
                    Optional<AnnotationInstance> classPathOp = classInfo.annotations().entrySet().stream()
                            .filter(entry -> entry.getKey().equals(RESTEASY_PATH))
                            .map(Map.Entry::getValue)
                            .flatMap(list -> list.stream())
                            .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
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
                        String path = "";
                        if (classPathOp.isPresent()) {
                            path += classPathOp.get().value().asString();
                        }
                        if (StringUtils.isNotBlank(methodPath)) {
                            path += methodPath;
                        }
                        path.replaceAll("//", "/");

                        String pubsubName = topic.value("pubsubName").asString();
                        String topicName = topic.value("name").asString();

                        try {
                            AnnotationValue metadataValue = topic.value("metadata");
                            Map<String, String> metadata = Objects.nonNull(metadataValue)
                                    ? objectMapper.readValue(metadataValue.asString(), MAP_TYPE)
                                    : new HashMap<>();
                            topicProducer.produce(new DaprTopicBuildItem(
                                    pubsubName,
                                    topicName,
                                    path,
                                    metadata));
                        } catch (Exception e) {
                            log.error("dapr topic map to path error in class:{},topicName:{}", classInfo.name().toString(),
                                    topicName, e);
                        }
                    }
                });
            }
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void addTopic(DaprRuntimeRecorder daprRuntimeRecorder, List<DaprTopicBuildItem> daprTopicBuildItems) {
        for (DaprTopicBuildItem item : daprTopicBuildItems) {
            daprRuntimeRecorder.subscribeToTopics(
                    item.getPubSubName(),
                    item.getTopicName(),
                    item.getRoute(),
                    item.getMetadata());
        }
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
        reflectiveClassBuildProducer.produce(new ReflectiveClassBuildItem(true, true,
                DaprTopicSubscription.class,
                ActorRuntimeConfig.class,
                CloudEvent.class));
    }
}
