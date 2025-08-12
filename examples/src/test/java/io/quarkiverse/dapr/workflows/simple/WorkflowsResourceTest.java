package io.quarkiverse.dapr.workflows.simple;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.Response;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkiverse.dapr.workflows.rest.CreateUserRequest;
import io.quarkiverse.dapr.workflows.rest.WorkflowRestClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class WorkflowsResourceTest {

    @InjectMock
    @RestClient
    WorkflowRestClient workflowRestClient;

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

    @Test
    @DisplayName("should test create user workflow that uses rest client beans")
    void shouldExecuteCreateUserWorkflow() {
        Mockito.when(workflowRestClient.create(Mockito.any(CreateUserRequest.class)))
                .thenReturn(Response.created(URI.create("/api/v1/users/" + UUID.randomUUID())).build());

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Alice");
        request.setAge(30);

        String instanceId = RestAssured.given()
                .contentType("application/json")
                .body(request)
                .post("/workflows/users")
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
                        .body(Matchers.containsString("CREATED")));

    }
}
