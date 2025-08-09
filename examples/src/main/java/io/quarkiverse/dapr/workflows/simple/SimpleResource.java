package io.quarkiverse.dapr.workflows.simple;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.quarkus.logging.Log;

@Path("/workflows/simple")
public class SimpleResource {

    @Inject
    WorkflowRuntimeBuilder runtimeBuilder;

    @Inject
    DaprWorkflowClient daprWorkflowClient;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uppercase(String text) throws InterruptedException {

        String instanceId = daprWorkflowClient.scheduleNewWorkflow(SimpleWorkflow.class, text);

        Log.info("Starting SimpleWorkflow with instance ID as " + instanceId);

        Thread.sleep(4000);

        WorkflowInstanceStatus instanceState = daprWorkflowClient.getInstanceState(instanceId, true);

        return Response.ok().entity(instanceState.getSerializedOutput()).build();
    }
}
