package io.quarkiverse.dapr.workflows.simple;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class WorkflowsResourceTest {

    @Test
    void shouldUseUppercaseWorkflow() {
        String instanceId = RestAssured.given()
                .contentType("text/plain")
                .body("hello from quarkus")
                .post("/workflows/uppercase")
                .then()
                .statusCode(202)
                .extract()
                .header("X-Instance-Id");

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> RestAssured.given()
                        .contentType("text/plain")
                        .get(String.format("/workflows/%s/result", instanceId))
                        .then()
                        .body(Matchers.containsString("HELLO FROM QUARKUS")));
    }
}
