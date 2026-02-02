package io.quarkiverse.dapr.workflows.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.workflows.simple.GreetingService;

@ApplicationScoped
public class UserWorkflow implements Workflow {

    @Inject
    GreetingService greetingService;

    @Override
    public WorkflowStub create() {
        return ctx -> {

            String instanceId = ctx.getInstanceId();

            ctx.getLogger().info("Workflow instance: {}", instanceId);

            User result = ctx
                    .callActivity(GetUserWorkflowActivity.class.getName(), ctx.getInput(String.class), User.class).await();

            ctx.getLogger().info("Workflow result (User): {}", result);

            ctx.complete(result);
        };
    }
}
