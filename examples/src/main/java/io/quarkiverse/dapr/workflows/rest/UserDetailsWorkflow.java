package io.quarkiverse.dapr.workflows.rest;

import jakarta.enterprise.context.ApplicationScoped;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

@ApplicationScoped
public class UserDetailsWorkflow implements Workflow {

    @Override
    public WorkflowStub create() {
        return ctx -> {

            String instanceId = ctx.getInstanceId();

            ctx.getLogger().info("Workflow instance: {}", instanceId);

            String userId = ctx.getInput(String.class);

            User user = ctx.callActivity(GetUserInfoWorkflowActivity.class.getName(), userId, User.class).await();

        };
    }
}
