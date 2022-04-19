package io.quarkiverse.dapr.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

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
                .body(is("[{\"pubsubName\":\"rocketmq\",\"topic\":\"test-topic2\",\"route\":\"/dapr\",\"metadata\":{}},{\"pubsubName\":\"rocketmq\",\"topic\":\"test-topic3\",\"route\":\"/dapr/topic3\",\"metadata\":{}}]"));
    }
}
