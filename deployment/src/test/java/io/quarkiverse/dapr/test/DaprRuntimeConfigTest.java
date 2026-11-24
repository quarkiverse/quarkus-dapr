package io.quarkiverse.dapr.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.sun.net.httpserver.HttpServer;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.config.Properties;
import io.quarkiverse.dapr.core.SyncDaprClient;
import io.quarkus.test.QuarkusUnitTest;

public class DaprRuntimeConfigTest {

    private static final int SIDECAR_HTTP_PORT = freePort();
    private static final String API_TOKEN = "my-token";

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.dapr.devservices.enabled", "false")
            .overrideConfigKey("quarkus.dapr.sidecar-ip", "127.0.0.1")
            .overrideConfigKey("quarkus.dapr.http-port", String.valueOf(SIDECAR_HTTP_PORT))
            .overrideConfigKey("quarkus.dapr.grpc-port", "50099")
            .overrideConfigKey("quarkus.dapr.api-token", API_TOKEN);

    @ConfigProperty(name = "quarkus.dapr.http-port")
    int httpPort;

    @Inject
    Properties properties;

    @Inject
    DaprClient daprClient;

    @Inject
    SyncDaprClient syncDaprClient;

    @Test
    void shouldExposeConfiguredProperties() {
        assertThat(properties.getValue(Properties.SIDECAR_IP)).isEqualTo("127.0.0.1");
        assertThat(properties.getValue(Properties.HTTP_PORT)).isEqualTo(httpPort);
        assertThat(properties.getValue(Properties.GRPC_PORT)).isEqualTo(50099);
        assertThat(properties.getValue(Properties.API_TOKEN)).isEqualTo(API_TOKEN);
    }

    @Test
    void daprClientShouldUseConfiguredPortAndToken() throws Exception {
        assertThat(callSidecar(() -> daprClient.invokeMethod("app", "method", null, HttpExtension.GET, Map.class)
                .block())).isEqualTo(API_TOKEN);
    }

    @Test
    void syncDaprClientShouldUseConfiguredPortAndToken() throws Exception {
        assertThat(callSidecar(() -> syncDaprClient.invokeMethod("app", "method", null, HttpExtension.GET, Map.class)))
                .isEqualTo(API_TOKEN);
    }

    /**
     * Starts a fake sidecar on the configured HTTP port, runs the call and returns the received API token.
     */
    private String callSidecar(Runnable call) throws Exception {
        CompletableFuture<String> receivedToken = new CompletableFuture<>();
        HttpServer sidecar = HttpServer.create(new InetSocketAddress("127.0.0.1", httpPort), 0);
        sidecar.createContext("/", exchange -> {
            receivedToken.complete(exchange.getRequestHeaders().getFirst("dapr-api-token"));
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        sidecar.start();
        try {
            call.run();
            return receivedToken.get(10, TimeUnit.SECONDS);
        } finally {
            sidecar.stop(0);
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
