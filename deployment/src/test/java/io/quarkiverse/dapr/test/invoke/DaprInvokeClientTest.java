package io.quarkiverse.dapr.test.invoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.dapr.config.Properties;
import io.quarkiverse.dapr.invoke.DaprInvokeClient;
import io.quarkiverse.dapr.invoke.RegisterDaprInvokeClient;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

/**
 * The application under test also plays the role of the Dapr sidecar: the clients are pointed to it and the
 * {@link FakeSidecarResource} records what it receives.
 */
public class DaprInvokeClientTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ExtensionsService.class, ConfiguredService.class, ExtensionsResource.class,
                            FakeSidecarResource.class, FakePingResource.class)
                    .addAsResource(new StringAsset("configured.app-id=configured-app\n"
                            + "dapr.http.endpoint=http://localhost:${quarkus.http.test-port:8081}\n"
                            + "quarkus.dapr.devservices.enabled=false\n"),
                            "application.properties"));

    static volatile String lastAppId;
    static volatile String lastApiToken;
    static volatile String lastAuthorization;

    @DaprInvokeClient
    ExtensionsService extensionsService;

    @DaprInvokeClient
    ConfiguredService configuredService;

    @Inject
    ExtensionsResource extensionsResource;

    @BeforeEach
    @AfterEach
    void reset() {
        lastAppId = null;
        lastApiToken = null;
        lastAuthorization = null;
        System.clearProperty(Properties.API_TOKEN.getName());
    }

    @Test
    void shouldInvokeThroughSidecarWithAppIdHeader() {
        String result = extensionsService.getById("a b", List.of("x", "y"), "token");

        assertThat(result).isEqualTo("a b:[x, y]");
        assertThat(lastAppId).isEqualTo("extensions-app");
        assertThat(lastAuthorization).isEqualTo("token");
        assertThat(lastApiToken).isNull();
    }

    @Test
    void shouldSendDaprApiTokenWhenConfigured() {
        System.setProperty(Properties.API_TOKEN.getName(), "secret");

        extensionsService.getById("1", List.of(), null);

        assertThat(lastApiToken).isEqualTo("secret");
    }

    @Test
    void shouldSendAndReceiveJson() {
        Map<String, String> result = extensionsService.create(Map.of("name", "dapr"));

        assertThat(result).containsEntry("name", "dapr").containsEntry("id", "1");
        assertThat(lastAppId).isEqualTo("extensions-app");
    }

    @Test
    void shouldSupportRegexPathTemplates() {
        extensionsService.delete("12");

        assertThat(lastAppId).isEqualTo("extensions-app");
    }

    @Test
    void shouldSupportAsyncReturnTypes() throws Exception {
        assertThat(extensionsService.uni().await().atMost(Duration.ofSeconds(5))).isEqualTo("uni");
        assertThat(extensionsService.stage().toCompletableFuture().get(5, TimeUnit.SECONDS)).isEqualTo("stage");
    }

    @Test
    void shouldInvokeDefaultMethods() {
        assertThat(extensionsService.upperCase("abc")).isEqualTo("ABC:[]");
    }

    @Test
    void shouldResolveAppIdFromConfigAndInjectInBeans() {
        assertThat(configuredService.ping()).isEqualTo("pong");
        assertThat(lastAppId).isEqualTo("configured-app");

        assertThat(extensionsResource.get()).isEqualTo("1:[]");
        assertThat(lastAppId).isEqualTo("extensions-app");
    }

    @Path("/dapr-invoke-client")
    @RegisterDaprInvokeClient(appId = "extensions-app")
    public interface ExtensionsService {

        @GET
        @Path("/items/{id}")
        String getById(@PathParam("id") String id, @QueryParam("tag") List<String> tags,
                @HeaderParam("Authorization") String authorization);

        @POST
        @Path("create")
        Map<String, String> create(Map<String, String> payload);

        @DELETE
        @Path("{id: [0-9]{2}}")
        void delete(@PathParam("id") String id);

        @GET
        @Path("uni")
        Uni<String> uni();

        @GET
        @Path("stage")
        CompletionStage<String> stage();

        default String upperCase(String id) {
            return getById(id, List.of(), null).toUpperCase();
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
            return extensionsService.getById("1", List.of(), null);
        }
    }

    @Path("/dapr-invoke-client")
    public static class FakeSidecarResource {

        @HeaderParam("dapr-app-id")
        String appId;

        @HeaderParam("dapr-api-token")
        String apiToken;

        @HeaderParam("Authorization")
        String authorization;

        private void record() {
            lastAppId = appId;
            lastApiToken = apiToken;
            lastAuthorization = authorization;
        }

        @GET
        @Path("/items/{id}")
        public String getById(@PathParam("id") String id, @QueryParam("tag") List<String> tags) {
            record();
            return id + ":" + tags;
        }

        @POST
        @Path("create")
        public Map<String, String> create(Map<String, String> payload) {
            record();
            return Map.of("id", "1", "name", payload.get("name"));
        }

        @DELETE
        @Path("{id: [0-9]{2}}")
        public void delete(@PathParam("id") String id) {
            record();
        }

        @GET
        @Path("uni")
        public String uni() {
            return "uni";
        }

        @GET
        @Path("stage")
        public String stage() {
            return "stage";
        }
    }

    @Path("/ping")
    public static class FakePingResource {

        @GET
        public String ping(@HeaderParam("dapr-app-id") String appId) {
            lastAppId = appId;
            return "pong";
        }
    }
}
