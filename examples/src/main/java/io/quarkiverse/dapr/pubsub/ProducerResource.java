package io.quarkiverse.dapr.pubsub;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.ResponseStatus;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.PublishEventRequest;

@Path("/pubsub/events")
public class ProducerResource {

    DaprClient daprClient;

    public ProducerResource(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    @POST
    @ResponseStatus(value = 202)
    public void sendEvent() {
        OrderEvent order = new OrderEvent(UUID.randomUUID().toString(), List.of(
                new OrderEvent.OrderItem(UUID.randomUUID().toString(), "Mouse", 10, BigDecimal.valueOf(25.00))));

        this.daprClient.waitForSidecar(1000).block();

        this.daprClient.publishEvent(new PublishEventRequest(
                "pubsub", "orders.created", order)).block();
    }
}
