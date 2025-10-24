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
                .when().get("/dapr")
                .then()
                .statusCode(200)
                .body(is("Hello dapr"));
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
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("pubsub.six");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("topic-6");
                    Assertions.assertThat(subscription.getRoutes()).isNotNull();
                    Assertions.assertThat(subscription.getRoutes().getRules()).hasSize(1);
                    Assertions.assertThat(subscription.getRoutes().getRules().get(0).getMatch())
                            .isEqualTo("event.type='found'");
                    Assertions.assertThat(subscription.getRoutes().getRules().get(0).getPath()).isEqualTo("/dapr/topic6");
                }).anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("pubsub");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("topic-5");
                    Assertions.assertThat(subscription.getRoute()).isEqualTo("/dapr/topic5");
                }).anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("messagebus");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("topic-4");
                    Assertions.assertThat(subscription.getRoute()).isEqualTo("/dapr/topic4");
                    Assertions.assertThat(subscription.getMetadata()).containsEntry("test", "aaa");
                }).anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("messagebus");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("test-topic2");
                    Assertions.assertThat(subscription.getRoute()).isEqualTo("/dapr");
                    Assertions.assertThat(subscription.getMetadata()).containsEntry("test", "aaa");
                }).anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("messagebus");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("test-topic3");
                    Assertions.assertThat(subscription.getRoute()).isEqualTo("/dapr/topic3");
                    Assertions.assertThat(subscription.getMetadata()).containsEntry("test", "aaa");
                }).anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("pubsub.six");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("topic-6");
                    Assertions.assertThat(subscription.getRoutes()).isNotNull();
                    Assertions.assertThat(subscription.getRoutes().getRules()).hasSize(1);
                    Assertions.assertThat(subscription.getRoutes().getRules().get(0).getMatch())
                            .isEqualTo("event.type='found'");
                    Assertions.assertThat(subscription.getRoutes().getRules().get(0).getPath()).isEqualTo("/dapr/topic6");
                }).anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("pubsub");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("topic-5");
                    Assertions.assertThat(subscription.getRoute()).isEqualTo("/dapr/topic5");
                }).anySatisfy(subscription -> {
                    Assertions.assertThat(subscription.getPubsubName()).isEqualTo("messagebus");
                    Assertions.assertThat(subscription.getTopic()).isEqualTo("topic-4");
                    Assertions.assertThat(subscription.getRoute()).isEqualTo("/dapr/topic4");
                    Assertions.assertThat(subscription.getMetadata()).containsEntry("test", "aaa");
                });

    }
}
