package io.quarkiverse.dapr.workflows.simple;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.dapr.workflows.runtime.WorkflowRuntimeStatus;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/workflows")
public class SimpleResource {

    @Inject
    WorkflowRuntimeBuilder runtimeBuilder;

    @Inject
    DaprWorkflowClient daprWorkflowClient;

    @Path("/uppercase")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uppercase(String text) throws InterruptedException {

        String instanceId = daprWorkflowClient.scheduleNewWorkflow(SimpleWorkflow.class, text);

        Log.info("Starting SimpleWorkflow with instance ID as " + instanceId);

        return Response.accepted()
                .header("X-Instance-Id", instanceId)
                .build();
    }

    @GET
    @Path("/{workflowId}/result")
    public Response workflowId(@PathParam("workflowId") String workflowId) {

        WorkflowInstanceStatus state = daprWorkflowClient.getInstanceState(workflowId, true);
        assert state != null;

        if (state.getRuntimeStatus().equals(WorkflowRuntimeStatus.COMPLETED)) {
            Log.info("Workflow completed successfully");
            return  Response.ok(
                    state.getSerializedOutput()
            ).build();
        }

        Log.info("Current status " + state.getRuntimeStatus());

        return Response.ok(state.readOutputAs(String.class)).build();
    }
 }
