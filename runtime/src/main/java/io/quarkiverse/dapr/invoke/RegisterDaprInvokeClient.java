package io.quarkiverse.dapr.invoke;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers an interface as a Dapr service invocation client.
 * <p>
 * The interface methods are mapped to Dapr service invocation calls using the Jakarta REST annotations:
 * {@code @Path} (on the interface and/or the method) defines the invoked method name, {@code @GET}, {@code @POST}, etc.
 * define the HTTP verb ({@code POST} by default), {@code @PathParam}, {@code @QueryParam} and {@code @HeaderParam}
 * are applied to the request, and a single non-annotated parameter is sent as the request body.
 * <p>
 * Methods may return a plain type (blocking call), {@code void}, {@code reactor.core.publisher.Mono},
 * {@code io.smallrye.mutiny.Uni} or {@code java.util.concurrent.CompletionStage}.
 * <p>
 * The client can be injected with the {@link DaprInvokeClient} qualifier:
 *
 * <pre>
 * &#64;Path("/orders")
 * &#64;RegisterDaprInvokeClient(appId = "order-service")
 * public interface OrderService {
 *
 *     &#64;GET
 *     &#64;Path("/{id}")
 *     Order getById(&#64;PathParam("id") String id);
 * }
 *
 * &#64;ApplicationScoped
 * public class OrderResource {
 *
 *     &#64;DaprInvokeClient
 *     OrderService orderService;
 * }
 * </pre>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterDaprInvokeClient {

    /**
     * The Dapr app id of the target application. Property expressions such as {@code ${order-service.app-id}} are
     * resolved from the application configuration at runtime.
     */
    String appId();
}
