package io.quarkiverse.dapr.demo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

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
    public void testTopicEndpoint() {
        String resp = "[{\"pubsubName\":\"messagebus\",\"topic\":\"test-topic2\",\"route\":\"/dapr\",\"metadata\":{\"test\":\"aaa\"}},{\"pubsubName\":\"messagebus\",\"topic\":\"test-topic3\",\"route\":\"/dapr/topic3\",\"metadata\":{\"test\":\"aaa\"}}]";
        given()
                .when().get("/dapr/subscribe")
                .then()
                .statusCode(200)
                .body(is(resp));
    }
}
