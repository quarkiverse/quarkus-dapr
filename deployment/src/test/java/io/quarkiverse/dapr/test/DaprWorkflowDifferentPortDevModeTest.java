package io.quarkiverse.dapr.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.dapr.test.workflows.HelloResource;
import io.quarkiverse.dapr.test.workflows.SayHelloWorkflow;
import io.quarkiverse.dapr.test.workflows.SayHelloWorkflowActivity;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DaprWorkflowDifferentPortDevModeTest {

    // Start hot reload (DevMode) test with your extension loaded
    @RegisterExtension
    static final QuarkusDevModeTest devModeTest = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SayHelloWorkflow.class, SayHelloWorkflowActivity.class, HelloResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.http.port=8888"), "application.properties"));

    @Test
    public void workflowDevMode() {

        RestAssured.given()
                .post("http://localhost:8888/hello")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("hello"));

        devModeTest.modifyResourceFile("application.properties",
                s -> "quarkus.log.category.\"io.quarkiverse.dapr.workflows\".min-level=DEBUG\n");

        RestAssured.given()
                .post("http://localhost:8888/hello")                .then()
                .statusCode(200)
                .body(Matchers.containsString("hello"));

    }
}
