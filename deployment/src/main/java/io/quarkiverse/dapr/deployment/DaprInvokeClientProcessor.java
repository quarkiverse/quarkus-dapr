package io.quarkiverse.dapr.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.dapr.client.DaprClient;
import io.quarkiverse.dapr.invoke.DaprInvokeClient;
import io.quarkiverse.dapr.invoke.DaprInvokeClientRecorder;
import io.quarkiverse.dapr.invoke.RegisterDaprInvokeClient;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;

/**
 * Registers the interfaces annotated with {@link RegisterDaprInvokeClient} as beans qualified with
 * {@link DaprInvokeClient}.
 */
public class DaprInvokeClientProcessor {

    private static final DotName REGISTER_DAPR_INVOKE_CLIENT = DotName.createSimple(RegisterDaprInvokeClient.class);
    private static final DotName DAPR_INVOKE_CLIENT = DotName.createSimple(DaprInvokeClient.class);
    private static final DotName DAPR_CLIENT = DotName.createSimple(DaprClient.class);
    private static final Set<DotName> ASYNC_TYPES = Set.of(
            DotName.createSimple("reactor.core.publisher.Mono"),
            DotName.createSimple("io.smallrye.mutiny.Uni"),
            DotName.createSimple("java.util.concurrent.CompletionStage"),
            DotName.createSimple("java.util.concurrent.CompletableFuture"));
    private static final Set<DotName> NON_BODY_PARAMS = Set.of(
            DotName.createSimple("jakarta.ws.rs.PathParam"),
            DotName.createSimple("jakarta.ws.rs.QueryParam"),
            DotName.createSimple("jakarta.ws.rs.HeaderParam"));

    @BuildStep
    AutoInjectAnnotationBuildItem autoInjectDaprInvokeClient() {
        return new AutoInjectAnnotationBuildItem(DAPR_INVOKE_CLIENT);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerDaprInvokeClients(CombinedIndexBuildItem index,
            DaprInvokeClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies) {
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(REGISTER_DAPR_INVOKE_CLIENT)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo clientInterface = annotation.target().asClass();
            if (!clientInterface.isInterface()) {
                throw new IllegalStateException("@RegisterDaprInvokeClient can only be used on interfaces, but "
                        + clientInterface.name() + " is not an interface");
            }
            AnnotationValue appIdValue = annotation.value("appId");
            String appId = appIdValue == null ? "" : appIdValue.asString().trim();
            if (appId.isEmpty()) {
                throw new IllegalStateException("@RegisterDaprInvokeClient on " + clientInterface.name()
                        + " must declare a non-empty appId");
            }
            String interfaceName = clientInterface.name().toString();

            syntheticBeans.produce(SyntheticBeanBuildItem.configure(clientInterface.name())
                    .addType(clientInterface.name())
                    .addQualifier(AnnotationInstance.builder(DAPR_INVOKE_CLIENT).build())
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .addInjectionPoint(ClassType.create(DAPR_CLIENT))
                    .createWith(recorder.createClient(interfaceName, appId))
                    .done());

            proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(interfaceName));
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(interfaceName)
                    .methods(true)
                    .reason(getClass().getName())
                    .build());
            for (Type type : payloadTypes(clientInterface)) {
                reflectiveHierarchies.produce(ReflectiveHierarchyBuildItem.builder(type)
                        .source(getClass().getSimpleName() + " > " + interfaceName)
                        .build());
            }
        }
    }

    /**
     * Collects the request body and response types of the client methods, so that they can be (de)serialized in
     * native mode.
     */
    private static List<Type> payloadTypes(ClassInfo clientInterface) {
        List<Type> types = new ArrayList<>();
        for (MethodInfo method : clientInterface.methods()) {
            if (!Modifier.isAbstract(method.flags())) {
                continue;
            }
            Type returnType = method.returnType();
            if (returnType.kind() == Type.Kind.PARAMETERIZED_TYPE && ASYNC_TYPES.contains(returnType.name())) {
                returnType = returnType.asParameterizedType().arguments().get(0);
            }
            addPayloadType(types, returnType);
            for (MethodParameterInfo parameter : method.parameters()) {
                boolean body = method.annotations().stream()
                        .filter(a -> a.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER
                                && a.target().asMethodParameter().position() == parameter.position())
                        .noneMatch(a -> NON_BODY_PARAMS.contains(a.name()));
                if (body) {
                    addPayloadType(types, parameter.type());
                }
            }
        }
        return types;
    }

    private static void addPayloadType(List<Type> types, Type type) {
        if (type.kind() == Type.Kind.CLASS || type.kind() == Type.Kind.PARAMETERIZED_TYPE
                || type.kind() == Type.Kind.ARRAY) {
            types.add(type);
        }
    }
}
