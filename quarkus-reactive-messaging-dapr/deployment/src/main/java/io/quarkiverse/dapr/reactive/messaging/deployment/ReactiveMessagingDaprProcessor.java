package io.quarkiverse.dapr.reactive.messaging.deployment;

import static io.quarkus.arc.processor.DotNames.OBJECT;
import static io.quarkus.arc.processor.DotNames.STRING;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.reactive.messaging.dapr.DaprConfig;
import io.quarkus.reactive.messaging.dapr.DaprConnector;
import io.quarkus.reactive.messaging.dapr.ReactiveDaprHandlerBean;
import io.quarkus.reactive.messaging.dapr.RouteFunction;
import io.quarkus.reactive.messaging.dapr.config.ConfigReader;
import io.quarkus.reactive.messaging.dapr.runtime.ReactiveDaprRecorder;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ReactiveMessagingDaprProcessor {

    private static final String FEATURE = "quarkus-reactive-messaging-dapr";

    private static final DotName JSON_ARRAY = DotName.createSimple(JsonArray.class.getName());
    private static final DotName JSON_OBJECT = DotName.createSimple(JsonObject.class.getName());
    private static final DotName MESSAGE = DotName.createSimple(Message.class.getName());
    private static final DotName MULTI = DotName.createSimple(Multi.class.getName());
    private static final DotName PROCESSOR = DotName.createSimple(Processor.class.getName());
    private static final DotName PROCESSOR_BUILDER = DotName.createSimple(ProcessorBuilder.class.getName());
    private static final DotName PUBLISHER = DotName.createSimple(Publisher.class.getName());
    private static final DotName PUBLISHER_BUILDER = DotName.createSimple(PublisherBuilder.class.getName());
    private static final DotName SUBSCRIBER = DotName.createSimple(Subscriber.class.getName());
    private static final DotName SUBSCRIBER_BUILDER = DotName.createSimple(SubscriberBuilder.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void addBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(
                new AdditionalBeanBuildItem(DaprConnector.class, ReactiveDaprHandlerBean.class, ConfigReader.class));
    }

    @BuildStep
    @Consume(CombinedIndexBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void generateRoutes(BuildProducer<RouteBuildItem> routes, ReactiveDaprRecorder recorder,
            BodyHandlerBuildItem bodyHandler) {
        List<DaprConfig> daprConfigs = ConfigReader.readIncomingHttpConfigs();

        if (!daprConfigs.isEmpty()) {
            Handler<RoutingContext> httpHandler = recorder.createHttpHandler();
            daprConfigs.stream().map(DaprConfig::path).distinct().forEach(path -> {
                routes.produce(RouteBuildItem.builder()
                        .routeFunction(path, new RouteFunction(path, bodyHandler.getHandler()))
                        .handler(bodyHandler.getHandler()).build());

                routes.produce(RouteBuildItem.builder().routeFunction(path, new RouteFunction(
                        path, httpHandler)).build());
            });
        }
    }

    @BuildStep
    void registerMessagePayloadClassesForReflection(BeanArchiveIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        Set<String> payloadClasses = new HashSet<>();
        for (AnnotationInstance incoming : index.getIndex()
                .getAnnotations(DotName.createSimple(Incoming.class.getName()))) {
            MethodInfo methodInfo = incoming.target().asMethod();
            List<Type> parameters = methodInfo.parameterTypes();

            if (parameters.size() == 1) {
                Type type = parameters.get(0);
                // payload can be consumed as Publisher[Builder]<PayloadObject> or Publisher[Builder]<Message<PayloadObject>>
                // or Multi<PayloadObject>, Multi<Message<PayloadObject>>
                DotName typeName = type.name();
                if (type.kind() == Type.Kind.PARAMETERIZED_TYPE && (typeName.equals(PUBLISHER_BUILDER) || typeName.equals(
                        PUBLISHER) || typeName.equals(MULTI))) {
                    List<Type> arguments = type.asParameterizedType().arguments();
                    if (arguments.size() > 0) {
                        collectPayloadType(payloadClasses, arguments.get(0));
                    }
                } else {
                    collectPayloadType(payloadClasses, type);
                }
            } else if (parameters.size() == 0) {
                // @Incoming method can also return a Subscriber[Builder] or Processor[Builder] for message payloads:
                Type returnType = methodInfo.returnType();
                if ((returnType.name().equals(SUBSCRIBER_BUILDER) || returnType.name().equals(PROCESSOR_BUILDER)
                        || returnType.name().equals(SUBSCRIBER) || returnType.name().equals(PROCESSOR))
                        && returnType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType parameterizedType = returnType.asParameterizedType();
                    List<Type> arguments = parameterizedType.arguments();
                    if (arguments.size() > 0) {
                        collectPayloadType(payloadClasses, arguments.get(0));
                    }
                }
            }
        }

        payloadClasses.removeAll(
                asList(JSON_OBJECT.toString(), OBJECT.toString(), JSON_ARRAY.toString(), STRING.toString()));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, payloadClasses.toArray(new String[] {})));
    }

    private void collectPayloadType(Set<String> payloadClasses, Type type) {
        if (type.kind() != Type.Kind.CLASS && type.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return;
        }
        if (type.name().equals(MESSAGE)) {
            // wrapped in a message
            if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                Type payloadType = type.asParameterizedType().arguments().get(0);
                if (payloadType.kind() == Type.Kind.CLASS) {
                    payloadClasses.add(payloadType.name().toString());
                }
            }
        } else {
            // or used directly
            payloadClasses.add(type.name().toString());
        }
    }
}
