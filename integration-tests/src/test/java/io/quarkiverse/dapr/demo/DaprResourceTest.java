package io.quarkiverse.dapr.demo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dapr.client.domain.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DaprResourceTest {

    @Inject
    ObjectMapper objectMapper;

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
    public void testCloudEventUnwrapsDataIntoPojo() throws Exception {
        given()
                .contentType(CloudEvent.CONTENT_TYPE)
                .body(objectMapper.writeValueAsString(orderCreatedEvent()))
                .when().post("/webhook/orders")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    public void testCloudEventUnwrapsDataIntoPojoWithNonPublicCtors() throws Exception {
        given()
                .contentType(CloudEvent.CONTENT_TYPE)
                .body(objectMapper.writeValueAsString(orderCreatedEvent()))
                .when().post("/webhook/orders-private")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    private static CloudEvent<OrderWebhookResource.Order> orderCreatedEvent() {
        OrderWebhookResource.Order order = new OrderWebhookResource.Order("fa985994-78ce-4013-9029-81dad4787a4a",
                List.of(new OrderItem(1L, "Quarkus Stickers", 19.99)));

        CloudEvent<OrderWebhookResource.Order> event = new CloudEvent<>(
                "10e48bf4-bc1b-4b0a-9d76-02dc9e095f55",
                "orders-api",
                "com.dapr.event.sent",
                "1.0",
                MediaType.APPLICATION_JSON,
                order);
        event.setPubsubName("rabbitmq");
        event.setTopic("order.created");
        event.setTime(OffsetDateTime.parse("2025-10-06T20:47:45Z"));
        return event;
    }

    @Test
    public void testCloudEventApplicationXml() {
        String xml = "<note>hello</note>";
        String body = "{\"id\":\"1\",\"source\":\"/tests\",\"specversion\":\"1.0\",\"type\":\"test\","
                + "\"datacontenttype\":\"application/xml\",\"data\":\"" + xml + "\"}";
        given()
                .contentType(CloudEvent.CONTENT_TYPE)
                .body(body)
                .when().post("/dapr/cloudevent")
                .then()
                .statusCode(200)
                .body(is(xml));
    }

    @Test
    public void testCloudEventTextXmlWithCharset() {
        String xml = "<note>hi</note>";
        String body = "{\"id\":\"2\",\"source\":\"/tests\",\"specversion\":\"1.0\",\"type\":\"test\","
                + "\"datacontenttype\":\"text/xml; charset=utf-8\",\"data\":\"" + xml + "\"}";
        given()
                .contentType(CloudEvent.CONTENT_TYPE)
                .body(body)
                .when().post("/dapr/cloudevent")
                .then()
                .statusCode(200)
                .body(is(xml));
    }

    @Test
    public void testCloudEventTextPlain() {
        String text = "hello dapr";
        String body = "{\"id\":\"3\",\"source\":\"/tests\",\"specversion\":\"1.0\",\"type\":\"test\","
                + "\"datacontenttype\":\"text/plain\",\"data\":\"" + text + "\"}";
        given()
                .contentType(CloudEvent.CONTENT_TYPE)
                .body(body)
                .when().post("/dapr/cloudevent")
                .then()
                .statusCode(200)
                .body(is(text));
    }
}
