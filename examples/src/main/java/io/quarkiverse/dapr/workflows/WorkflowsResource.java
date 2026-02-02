package io.quarkiverse.dapr.workflows;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.workflows.client.WorkflowState;
import io.quarkiverse.dapr.workflows.rest.UserWorkflow;
import io.quarkiverse.dapr.workflows.simple.DemoChainWorkflow;
import io.quarkus.logging.Log;

@Path("/workflows")
public class WorkflowsResource {

    @Inject
    DaprWorkflowClient daprWorkflowClient;

    @Path("/uppercase")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uppercase(String text) {

        Log.info("DemoChainWorkflow.class: " + DemoChainWorkflow.class.getName());

        String instanceId = daprWorkflowClient.scheduleNewWorkflow(DemoChainWorkflow.class, text);

        Log.info("Starting DemoChainWorkflow with instance ID as " + instanceId);

        return Response.accepted()
                .header("Workflow-Instance-Id", instanceId)
                .build();
    }

    @Path("/user/{userId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response user(@PathParam("userId") String userId) {
        String instanceId = daprWorkflowClient.scheduleNewWorkflow(UserWorkflow.class, userId);
        Log.info("Starting UserWorkflow with instance ID as " + instanceId);
        return Response.accepted()
                .header("Workflow-Instance-Id", instanceId)
                .build();
    }

    @GET
    @Path("/{workflowId}/result")
    @Produces(MediaType.TEXT_PLAIN)
    public Response workflowId(@PathParam("workflowId") String workflowId) {

        WorkflowState state = daprWorkflowClient.getWorkflowState(workflowId, true);
        assert state != null;

        if (state.getRuntimeStatus().equals(WorkflowRuntimeStatus.COMPLETED)) {
            Log.info("Workflow completed successfully");
            return Response.ok(
                    state.getSerializedOutput()).build();
        }

        Log.info("Current status " + state.getRuntimeStatus());

        return Response.ok(state.readOutputAs(Object.class)).build();
    }
}
