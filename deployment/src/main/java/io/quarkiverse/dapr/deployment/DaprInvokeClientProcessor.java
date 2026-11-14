package io.quarkiverse.dapr.deployment;

import java.util.Optional;

import jakarta.ws.rs.Priorities;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import io.quarkiverse.dapr.invoke.DaprInvokeClient;
import io.quarkiverse.dapr.invoke.DaprInvokeClientRequestFilter;
import io.quarkiverse.dapr.invoke.RegisterDaprInvokeClient;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.rest.client.reactive.deployment.RegisterProviderAnnotationInstanceBuildItem;
import io.quarkus.rest.client.reactive.deployment.RegisteredRestClientBuildItem;
import io.quarkus.rest.client.reactive.spi.RestClientAnnotationsTransformerBuildItem;

/**
 * Turns the interfaces annotated with {@link RegisterDaprInvokeClient} into Quarkus REST Clients that target the Dapr
 * sidecar: the REST Client extension does all the work, we only point it to the sidecar, add the {@code dapr-app-id}
 * header and qualify the resulting bean with {@link DaprInvokeClient}.
 */
public class DaprInvokeClientProcessor {

    private static final DotName REGISTER_DAPR_INVOKE_CLIENT = DotName.createSimple(RegisterDaprInvokeClient.class);
    private static final DotName DAPR_INVOKE_CLIENT = DotName.createSimple(DaprInvokeClient.class);
    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class);
    private static final DotName REGISTER_PROVIDER = DotName.createSimple(RegisterProvider.class);
    private static final DotName REQUEST_FILTER = DotName.createSimple(DaprInvokeClientRequestFilter.class);

    /**
     * Same resolution as the Dapr SDK ({@code DAPR_HTTP_ENDPOINT}, {@code DAPR_SIDECAR_IP} and {@code DAPR_HTTP_PORT}).
     */
    private static final String SIDECAR_HTTP_ENDPOINT = "${dapr.http.endpoint:http://${dapr.sidecar.ip:127.0.0.1}:${dapr.http.port:3500}}";
    private static final String APP_ID_HEADER = "dapr-app-id";

    @BuildStep
    AutoInjectAnnotationBuildItem autoInjectDaprInvokeClient() {
        return new AutoInjectAnnotationBuildItem(DAPR_INVOKE_CLIENT);
    }

    @BuildStep
    void registerDaprInvokeClients(CombinedIndexBuildItem index,
            BuildProducer<RegisteredRestClientBuildItem> restClients,
            BuildProducer<RegisterProviderAnnotationInstanceBuildItem> providers,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> configDefaults) {
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(REGISTER_DAPR_INVOKE_CLIENT)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo clientInterface = annotation.target().asClass();
            if (!clientInterface.isInterface()) {
                throw new IllegalStateException("@RegisterDaprInvokeClient can only be used on interfaces, but "
                        + clientInterface.name() + " is not an interface");
            }
            if (clientInterface.hasDeclaredAnnotation(REGISTER_REST_CLIENT)) {
                throw new IllegalStateException(clientInterface.name()
                        + " cannot be annotated with both @RegisterDaprInvokeClient and @RegisterRestClient");
            }
            AnnotationValue appIdValue = annotation.value("appId");
            String appId = appIdValue == null ? "" : appIdValue.asString().trim();
            if (appId.isEmpty()) {
                throw new IllegalStateException("@RegisterDaprInvokeClient on " + clientInterface.name()
                        + " must declare a non-empty appId");
            }
            String interfaceName = clientInterface.name().toString();

            // let the REST Client extension generate the client and its CDI bean
            restClients.produce(new RegisteredRestClientBuildItem(clientInterface, Optional.empty(), Optional.empty()));

            // target the sidecar, which routes the request to the application named by the dapr-app-id header
            String configPrefix = "quarkus.rest-client.\"" + interfaceName + "\".";
            configDefaults.produce(new RunTimeConfigurationDefaultBuildItem(configPrefix + "url", SIDECAR_HTTP_ENDPOINT));
            configDefaults.produce(new RunTimeConfigurationDefaultBuildItem(
                    configPrefix + "headers.\"" + APP_ID_HEADER + "\"", appId));

            // same effect as @RegisterProvider(DaprInvokeClientRequestFilter.class) on the interface
            providers.produce(new RegisterProviderAnnotationInstanceBuildItem(interfaceName,
                    AnnotationInstance.builder(REGISTER_PROVIDER)
                            .value(ClassType.create(REQUEST_FILTER))
                            .add("priority", Priorities.AUTHENTICATION)
                            .buildWithTarget(clientInterface)));
        }
    }

    /**
     * The annotations of the interface are copied to the bean generated by the REST Client extension, so this makes
     * it injectable with the {@link DaprInvokeClient} qualifier.
     */
    @BuildStep
    RestClientAnnotationsTransformerBuildItem qualifyDaprInvokeClients() {
        return new RestClientAnnotationsTransformerBuildItem(AnnotationTransformation.forClasses()
                .whenClass(clazz -> clazz.hasDeclaredAnnotation(REGISTER_DAPR_INVOKE_CLIENT))
                .transform(context -> context.add(DaprInvokeClient.class)));
    }
}
