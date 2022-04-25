package io.quarkiverse.dapr.it;

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
                .body(is("Hello, this is quarkus-dapr demo app2"));
    }

    @Test
    public void testTopicEndpoint() {
        given()
                .when().get("/dapr/subscribe")
                .then()
                .statusCode(200)
                .body(is(
                        "[{\"pubsubName\":\"messagebus\",\"topic\":\"topic1\",\"route\":\"/pubsub/topic1\",\"metadata\":{}}]"));
    }
}
