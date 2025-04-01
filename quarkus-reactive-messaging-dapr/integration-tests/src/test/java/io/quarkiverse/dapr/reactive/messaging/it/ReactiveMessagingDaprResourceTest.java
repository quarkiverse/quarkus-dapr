package io.quarkiverse.dapr.reactive.messaging.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ReactiveMessagingDaprResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/reactive-messaging-dapr")
                .then()
                .statusCode(200)
                .body(is("Hello reactive-messaging-dapr"));
    }
}
