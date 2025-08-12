package io.quarkiverse.dapr.workflows.rest;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

public class CreateUserWorkflow implements Workflow {

    @Override
    public WorkflowStub create() {
        return ctx -> {

            CreateUserRequest input = ctx.getInput(CreateUserRequest.class);

            String status = ctx.callActivity(DoRequestWorkflowActivity.class.getName(), input, String.class).await();

            ctx.complete(status);
        };
    }
}
