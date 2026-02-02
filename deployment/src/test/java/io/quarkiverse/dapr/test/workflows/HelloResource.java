package io.quarkiverse.dapr.test.workflows;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.smallrye.common.annotation.Blocking;

@Path("/hello")
public class HelloResource {

    @Inject
    DaprWorkflowClient client;

    @POST
    @Blocking
    public String hello() throws InterruptedException {
        String id = client.scheduleNewWorkflow(SayHelloWorkflow.class);
        while (true) {
            WorkflowInstanceStatus workflowState = client.getInstanceState(id, true);
            boolean isCompleted = workflowState.isCompleted();

            if (isCompleted) {
                return workflowState.getSerializedOutput();
            }

            Thread.sleep(1000);
        }
    }

}
