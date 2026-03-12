package io.quarkiverse.dapr.demo;

import static io.restassured.RestAssured.given;
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
        String resp = "[{\"pubsubName\":\"pubsub.six\",\"topic\":\"topic-6\",\"routes\":{\"rules\":[{\"match\":\"event.type='found'\",\"path\":\"/dapr/topic6\"}]},\"metadata\":{}},{\"pubsubName\":\"pubsub\",\"topic\":\"topic-5\",\"route\":\"/dapr/topic5\",\"metadata\":{}},{\"pubsubName\":\"rabbitmq\",\"topic\":\"order.created\",\"route\":\"/webhook/orders\",\"metadata\":{}},{\"pubsubName\":\"messagebus\",\"topic\":\"topic-4\",\"route\":\"/dapr/topic4\",\"metadata\":{\"test\":\"aaa\"}},{\"pubsubName\":\"messagebus\",\"topic\":\"test-topic2\",\"route\":\"/dapr\",\"metadata\":{\"test\":\"aaa\"}},{\"pubsubName\":\"messagebus\",\"topic\":\"test-topic3\",\"route\":\"/dapr/topic3\",\"metadata\":{\"test\":\"aaa\"}}]";
        given()
                .when().get("/dapr/subscribe")
                .then()
                .statusCode(200)
                .body(is(resp));
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
}
