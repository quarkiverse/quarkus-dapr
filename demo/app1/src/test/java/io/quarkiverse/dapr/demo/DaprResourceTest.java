package io.quarkiverse.dapr.demo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DaprResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/pubsub")
                .then()
                .statusCode(200)
                .body(is("Hello, this is quarkus-dapr demo app1"));
    }

    @Test
    public void testSubscribeEndpoint() {

        Subscription[] subscriptions = given()
                .when()
                .get("/dapr/subscribe")
                .body()
                .as(Subscription[].class);

        Assertions.assertThat(subscriptions)
                .anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("messagebus");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("topic3");
                    Assertions.assertThat(subscription.getRoutes()).isNull();
                    Assertions.assertThat(subscription.getRoute()).isEqualTo("/pubsub/topic3");
                    Assertions.assertThat(subscription.getMetadata()).isEmpty();
                }).anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("messagebus");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("topic4");
                    Assertions.assertThat(subscription.getRoutes()).isNull();
                    Assertions.assertThat(subscription.getRoute()).isEqualTo("/pubsub/topic4");
                    Assertions.assertThat(subscription.getMetadata()).isEmpty();
                }).anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("messagebus");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("topic2");
                    Assertions.assertThat(subscription.getRoutes()).isNull();
                    Assertions.assertThat(subscription.getRoute()).isEqualTo("/pubsub/topic2");
                    Assertions.assertThat(subscription.getMetadata()).isEmpty();
                });
    }
}
