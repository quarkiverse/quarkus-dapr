package io.quarkiverse.dapr.workflows;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class WorkflowsResourceTest {

    @Test
    void shouldExecuteUserWorkflow() {
        String workflowInstanceId = RestAssured.given()
                .header("Content-Type", "application/json")
                .post("/workflows/user/1")
                .then()
                .statusCode(202)
                .extract()
                .header("Workflow-Instance-Id");

        Awaitility.await()
                .untilAsserted(() -> RestAssured.given()
                        .get(
                                "/workflows/{workflowInstanceId}/result",
                                workflowInstanceId)
                        .then()
                        .statusCode(200)
                        .body(Matchers.containsString("John Doe")));

    }

    @Test
    void shouldExecuteDemoChainWorkflow() {
        String workflowInstanceId = RestAssured.given()
                .contentType("text/plain")
                .body("hello world")
                .post("/workflows/uppercase")
                .then()
                .statusCode(202)
                .extract()
                .header("Workflow-Instance-Id");

        Awaitility.await()
                .untilAsserted(() -> RestAssured.given()
                        .get(
                                "/workflows/{workflowInstanceId}/result",
                                workflowInstanceId)
                        .then()
                        .statusCode(200)
                        .body(Matchers.containsString("HELLO WORLD")));
    }
}
