package io.quarkiverse.dapr.test.invoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprHttp;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import io.quarkiverse.dapr.invoke.DaprInvokeClient;
import io.quarkiverse.dapr.invoke.RegisterDaprInvokeClient;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import reactor.core.publisher.Mono;

public class DaprInvokeClientTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ExtensionsService.class, ConfiguredService.class, ExtensionsResource.class,
                            FakeDaprClientProducer.class, Invocation.class)
                    .addAsResource(new StringAsset("configured.app-id=configured-app\n"
                            + "quarkus.dapr.devservices.enabled=false\n"),
                            "application.properties"));

    static volatile Invocation lastInvocation;
    static volatile Object nextResponse;

    @DaprInvokeClient
    ExtensionsService extensionsService;

    @DaprInvokeClient
    ConfiguredService configuredService;

    @jakarta.inject.Inject
    ExtensionsResource extensionsResource;

    @BeforeEach
    void reset() {
        lastInvocation = null;
        nextResponse = null;
    }

    @Test
    void shouldInvokeGetWithPathAndQueryParams() {
        nextResponse = "found";

        String result = extensionsService.getById("a b", List.of("x", "y"), "token");

        assertThat(result).isEqualTo("found");
        assertThat(lastInvocation.appId).isEqualTo("extensions-app");
        assertThat(lastInvocation.method).isEqualTo("dapr-invoke-client/items/a%20b");
        assertThat(lastInvocation.httpExtension.getMethod()).isEqualTo(DaprHttp.HttpMethods.GET);
        assertThat(lastInvocation.httpExtension.getQueryParams()).containsEntry("tag", List.of("x", "y"));
        assertThat(lastInvocation.httpExtension.getHeaders()).containsEntry("Authorization", "token");
        assertThat(lastInvocation.body).isNull();
        assertThat(lastInvocation.type.getType()).isEqualTo(String.class);
    }

    @Test
    void shouldSendBodyAndDefaultToPost() {
        nextResponse = Map.of("id", "1");

        Map<String, String> result = extensionsService.create(Map.of("name", "dapr"));

        assertThat(result).containsEntry("id", "1");
        assertThat(lastInvocation.method).isEqualTo("dapr-invoke-client/create");
        assertThat(lastInvocation.httpExtension.getMethod()).isEqualTo(DaprHttp.HttpMethods.POST);
        assertThat(lastInvocation.body).isEqualTo(Map.of("name", "dapr"));
    }

    @Test
    void shouldSupportVoidAndReactiveReturnTypes() throws Exception {
        extensionsService.delete("1");
        assertThat(lastInvocation.httpExtension.getMethod()).isEqualTo(DaprHttp.HttpMethods.DELETE);
        assertThat(lastInvocation.method).isEqualTo("dapr-invoke-client/1");
        assertThat(lastInvocation.type).isEqualTo(TypeRef.VOID);

        nextResponse = "mono";
        assertThat(extensionsService.mono("v").block()).isEqualTo("mono");
        assertThat(lastInvocation.httpExtension.getMethod()).isEqualTo(DaprHttp.HttpMethods.PUT);

        lastInvocation = null;
        nextResponse = "uni";
        Uni<String> uni = extensionsService.uni();
        assertThat(lastInvocation).as("Uni must be lazy").isNull();
        assertThat(uni.await().indefinitely()).isEqualTo("uni");

        nextResponse = "stage";
        assertThat(extensionsService.stage().toCompletableFuture().get(5, TimeUnit.SECONDS)).isEqualTo("stage");
    }

    @Test
    void shouldInvokeDefaultMethods() {
        nextResponse = "found";
        assertThat(extensionsService.upperCase("1")).isEqualTo("FOUND");
    }

    @Test
    void shouldResolveAppIdFromConfigAndInjectInBeans() {
        nextResponse = "ok";
        assertThat(configuredService.ping()).isEqualTo("ok");
        assertThat(lastInvocation.appId).isEqualTo("configured-app");
        assertThat(lastInvocation.method).isEqualTo("ping");

        nextResponse = "from-bean";
        assertThat(extensionsResource.get()).isEqualTo("from-bean");
    }

    @Path("/dapr-invoke-client")
    @RegisterDaprInvokeClient(appId = "extensions-app")
    public interface ExtensionsService {

        @GET
        @Path("/items/{id}")
        String getById(@PathParam("id") String id, @QueryParam("tag") List<String> tags,
                @HeaderParam("Authorization") String authorization);

        @Path("create")
        Map<String, String> create(Map<String, String> payload);

        @DELETE
        @Path("{id: [0-9]+}")
        void delete(@PathParam("id") String id);

        @PUT
        Mono<String> mono(String value);

        @GET
        Uni<String> uni();

        @POST
        CompletionStage<String> stage();

        default String upperCase(String id) {
            return getById(id, null, null).toUpperCase();
        }
    }

    @RegisterDaprInvokeClient(appId = "${configured.app-id}")
    public interface ConfiguredService {

        @GET
        @Path("ping")
        String ping();
    }

    @ApplicationScoped
    public static class ExtensionsResource {

        @DaprInvokeClient
        ExtensionsService extensionsService;

        String get() {
            return extensionsService.getById("1", null, null);
        }
    }

    public static class Invocation {
        String appId;
        String method;
        Object body;
        HttpExtension httpExtension;
        TypeRef<?> type;
    }

    public static class FakeDaprClientProducer {

        @Produces
        @Singleton
        DaprClient daprClient() {
            return (DaprClient) Proxy.newProxyInstance(DaprClient.class.getClassLoader(),
                    new Class<?>[] { DaprClient.class }, (proxy, method, args) -> {
                        if (method.getName().equals("invokeMethod") && args.length == 6) {
                            Invocation invocation = new Invocation();
                            invocation.appId = (String) args[0];
                            invocation.method = (String) args[1];
                            invocation.body = args[2];
                            invocation.httpExtension = (HttpExtension) args[3];
                            invocation.type = (TypeRef<?>) args[5];
                            return Mono.fromCallable(() -> {
                                lastInvocation = invocation;
                                return nextResponse;
                            });
                        }
                        if (method.getName().equals("close")) {
                            return null;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
