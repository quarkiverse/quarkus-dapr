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
                .when().get("/pubsub")
                .then()
                .statusCode(200)
                .body(is("Hello, this is quarkus-dapr demo app1"));
    }

    @Test
    public void testTopicEndpoint() {
        given()
                .when().get("/dapr/subscribe")
                .then()
                .statusCode(200)
                .body(is(
                        "[{\"pubsubName\":\"messagebus\",\"topic\":\"topic2\",\"route\":\"/pubsub/topic2\",\"metadata\":{}},{\"pubsubName\":\"messagebus\",\"topic\":\"topic3\",\"route\":\"/pubsub/topic3\",\"metadata\":{}},{\"pubsubName\":\"messagebus\",\"topic\":\"topic4\",\"route\":\"/pubsub/topic4\",\"metadata\":{}}]"));
    }
}
