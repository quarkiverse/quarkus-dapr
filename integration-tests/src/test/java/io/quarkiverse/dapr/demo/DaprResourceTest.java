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
        String resp = "[{\"pubsubName\":\"pubsub.six\",\"topic\":\"topic-6\",\"routes\":{\"rules\":[{\"match\":\"event.type='found'\",\"path\":\"/dapr/topic6\"}]},\"metadata\":{}},{\"pubsubName\":\"pubsub\",\"topic\":\"topic-5\",\"route\":\"/dapr/topic5\",\"metadata\":{}},{\"pubsubName\":\"messagebus\",\"topic\":\"topic-4\",\"route\":\"/dapr/topic4\",\"metadata\":{\"test\":\"aaa\"}},{\"pubsubName\":\"messagebus\",\"topic\":\"test-topic2\",\"route\":\"/dapr\",\"metadata\":{\"test\":\"aaa\"}},{\"pubsubName\":\"messagebus\",\"topic\":\"test-topic3\",\"route\":\"/dapr/topic3\",\"metadata\":{\"test\":\"aaa\"}}]";
        given()
                .when().get("/dapr/subscribe")
                .then()
                .statusCode(200)
                .body(is(resp));
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
}
