package io.quarkiverse.dapr.demo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.dapr.client.domain.CloudEvent;
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
    public void testTopicEndpoint() {
        given()
                .when().get("/dapr/subscribe")
                .then()
                .statusCode(200)
                .body("", hasSize(6))
                .body("find { it.pubsubName == 'pubsub.six' && it.topic == 'topic-6' }.routes.rules[0].match",
                        is("event.type='found'"))
                .body("find { it.pubsubName == 'pubsub.six' && it.topic == 'topic-6' }.routes.rules[0].path",
                        is("/dapr/topic6"))
                .body("find { it.pubsubName == 'pubsub.six' && it.topic == 'topic-6' }.metadata", anEmptyMap())
                .body("find { it.pubsubName == 'pubsub' && it.topic == 'topic-5' }.route", is("/dapr/topic5"))
                .body("find { it.pubsubName == 'pubsub' && it.topic == 'topic-5' }.metadata", anEmptyMap())
                .body("find { it.pubsubName == 'rabbitmq' && it.topic == 'order.created' }.route",
                        anyOf(is("/webhook/orders-private"), is("/webhook/orders")))
                .body("find { it.pubsubName == 'rabbitmq' && it.topic == 'order.created' }.metadata", anEmptyMap())
                .body("find { it.pubsubName == 'messagebus' && it.topic == 'topic-4' }.route", is("/dapr/topic4"))
                .body("find { it.pubsubName == 'messagebus' && it.topic == 'topic-4' }.metadata.test", is("aaa"))
                .body("find { it.pubsubName == 'messagebus' && it.topic == 'test-topic2' }.route", is("/dapr"))
                .body("find { it.pubsubName == 'messagebus' && it.topic == 'test-topic2' }.metadata.test", is("aaa"))
                .body("find { it.pubsubName == 'messagebus' && it.topic == 'test-topic3' }.route", is("/dapr/topic3"))
                .body("find { it.pubsubName == 'messagebus' && it.topic == 'test-topic3' }.metadata.test", is("aaa"));
    }

    @Test
    public void testCloudEventUnwrapsDataIntoPojo() {
        String event = "{"
                + "\"data\":{"
                + "\"id\":\"fa985994-78ce-4013-9029-81dad4787a4a\","
                + "\"items\":[{\"id\":1,\"name\":\"Quarkus Stickers\",\"price\":19.99}]"
                + "},"
                + "\"datacontenttype\":\"application/json\","
                + "\"id\":\"10e48bf4-bc1b-4b0a-9d76-02dc9e095f55\","
                + "\"pubsubname\":\"rabbitmq\","
                + "\"source\":\"orders-api\","
                + "\"specversion\":\"1.0\","
                + "\"time\":\"2025-10-06T20:47:45Z\","
                + "\"topic\":\"order.created\","
                + "\"type\":\"com.dapr.event.sent\""
                + "}";

        given()
                .contentType(CloudEvent.CONTENT_TYPE)
                .body(event)
                .when().post("/webhook/orders")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    public void testCloudEventUnwrapsDataIntoPojoWithNonPublicCtors() {
        String event = "{"
                + "\"data\":{"
                + "\"id\":\"fa985994-78ce-4013-9029-81dad4787a4a\","
                + "\"items\":[{\"id\":1,\"name\":\"Quarkus Stickers\",\"price\":19.99}]"
                + "},"
                + "\"datacontenttype\":\"application/json\","
                + "\"id\":\"10e48bf4-bc1b-4b0a-9d76-02dc9e095f55\","
                + "\"pubsubname\":\"rabbitmq\","
                + "\"source\":\"orders-api\","
                + "\"specversion\":\"1.0\","
                + "\"time\":\"2025-10-06T20:47:45Z\","
                + "\"topic\":\"order.created\","
                + "\"type\":\"com.dapr.event.sent\""
                + "}";

        given()
                .contentType(CloudEvent.CONTENT_TYPE)
                .body(event)
                .when().post("/webhook/orders-private")
                .then()
                .statusCode(200)
                .body(is("1"));
    }
}
