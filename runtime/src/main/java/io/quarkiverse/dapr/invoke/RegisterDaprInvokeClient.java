package io.quarkiverse.dapr.invoke;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers an interface as a Dapr service invocation client.
 * <p>
 * The interface is a regular Quarkus REST Client (as if it was annotated with {@code @RegisterRestClient}), so
 * everything supported by the REST Client is available. The only differences are that the requests are sent to the
 * Dapr sidecar, with the {@code dapr-app-id} header set to {@link #appId()}, and that the client is injected with
 * the {@link DaprInvokeClient} qualifier:
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
