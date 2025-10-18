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
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.quarkiverse.dapr.workflows.rest.UserDetailsWorkflow;
import io.quarkiverse.dapr.workflows.simple.DemoChainWorkflow;
import io.quarkus.logging.Log;

@Path("/workflows")
public class WorkflowsResource {

    @Inject
    DaprWorkflowClient daprWorkflowClient;

    @GET
    @Path("/users/{userId}/details")
    public Response users(@PathParam("userId") String userId) {

        String instanceId = daprWorkflowClient.scheduleNewWorkflow(UserDetailsWorkflow.class, userId);

        Log.info("Starting UserDetailsWorkflow with instance ID as " + instanceId);

        return Response.accepted()
                .header("Workflow-Instance-Id", instanceId)
                .build();

    }

    @Path("/uppercase")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uppercase(String text) {

        String instanceId = daprWorkflowClient.scheduleNewWorkflow(DemoChainWorkflow.class, text);

        Log.info("Starting SimpleWorkflow with instance ID as " + instanceId);

        return Response.accepted()
                .header("Workflow-Instance-Id", instanceId)
                .build();
    }

    @GET
    @Path("/{workflowId}/result")
    public Response workflowId(@PathParam("workflowId") String workflowId) {

        WorkflowInstanceStatus state = daprWorkflowClient.getInstanceState(workflowId, true);
        assert state != null;

        if (state.getRuntimeStatus().equals(WorkflowRuntimeStatus.COMPLETED)) {
            Log.info("Workflow completed successfully");
            return Response.ok(
                    state.getSerializedOutput()).build();
        }

        Log.info("Current status " + state.getRuntimeStatus());

        return Response.ok(state.readOutputAs(String.class)).build();
    }
}
