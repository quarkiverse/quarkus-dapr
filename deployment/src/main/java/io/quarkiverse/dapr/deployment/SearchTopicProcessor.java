package io.quarkiverse.dapr.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

import io.quarkiverse.dapr.config.DaprConfig;
import io.quarkiverse.dapr.deployment.items.TopicBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

public class SearchTopicProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTopicProcessor.class);
    private static final TypeReference<HashMap<String, String>> MAP_TYPE = new TypeReference<HashMap<String, String>>() {
    };

    @BuildStep
    void daprTopicBuildItems(BuildProducer<TopicBuildItem> topicProducer, CombinedIndexBuildItem indexBuildItem,
            DaprConfig daprConfig) {
        Map<String, DaprConfig.DaprPubSubConfig> pubSubConfigMap = Optional.ofNullable(daprConfig.pubSub())
                .orElse(new HashMap<>(16));

        for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(DotNames.TOPIC_DOTNAME)) {
            if (i.target().kind() == AnnotationTarget.Kind.METHOD) {

                MethodInfo methodInfo = i.target().asMethod();
                ClassInfo classInfo = methodInfo.declaringClass();
                Optional<AnnotationInstance> methodPostOp = methodInfo.annotations().stream()
                        .filter(annotation -> annotation.name()
                                .equals(DotNames.RESTEASY_POST_DOTNAME))
                        .findFirst();

                methodPostOp.ifPresent(mp -> {
                    Optional<AnnotationInstance> classPathOp = classInfo.annotationsMap().entrySet().stream()
                            .filter(entry -> entry.getKey()
                                    .equals(DotNames.RESTEASY_PATH_DOTNAME))
                            .map(Map.Entry::getValue)
                            .flatMap(Collection::stream)
                            .filter(a -> a.target()
                                    .kind() == AnnotationTarget.Kind.CLASS)
                            .findFirst();

                    String methodPath = null;
                    AnnotationInstance topic = null;
                    for (AnnotationInstance annotation : methodInfo.annotations()) {
                        DotName name = annotation.name();
                        if (name.equals(DotNames.RESTEASY_PATH_DOTNAME)) {
                            methodPath = annotation.value().asString();
                        }
                        if (name.equals(DotNames.TOPIC_DOTNAME)) {
                            topic = annotation;
                        }
                    }

                    if (classPathOp.isPresent() || Objects.nonNull(methodPath)) {
                        TopicBuildItem item = buildDaprTopicBuildItem(daprConfig, pubSubConfigMap, classInfo, classPathOp,
                                methodPath, topic);
                        topicProducer.produce(item);
                    }
                });
            }
        }
    }

    private static TopicBuildItem buildDaprTopicBuildItem(DaprConfig daprConfig,
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

        LOGGER.debug("Registering topic with name '{}'", topicName);
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
                        return DeploymentObjectMapper.getInstance().readValue(a.asString(), MAP_TYPE);
                    } catch (JsonProcessingException e) {
                        LOGGER.error("dapr topic metadata to path error in class:{},topicName:{}",
                                classInfo.name().toString(),
                                topicName, e);
                        return null;
                    }
                })
                .orElse(new HashMap<>(8));
        consumeMetadata.putAll(topicMetadata);
        return new TopicBuildItem(
                pubsubName,
                topicName,
                path,
                ruleMatch,
                rulePriority,
                consumeMetadata);
    }

}
