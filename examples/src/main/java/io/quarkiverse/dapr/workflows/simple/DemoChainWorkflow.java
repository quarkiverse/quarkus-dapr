package io.quarkiverse.dapr.workflows.simple;

import jakarta.enterprise.context.ApplicationScoped;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

@ApplicationScoped
public class DemoChainWorkflow implements Workflow {

    @Override
    public WorkflowStub create() {
        return ctx -> {

            String instanceId = ctx.getInstanceId();

            ctx.getLogger().info("Workflow instance: {}", instanceId);

            String result = ctx
                    .callActivity(UppercaseWorkflowActivity.class.getName(), ctx.getInput(String.class), String.class).await();

            ctx.getLogger().info("Workflow result: {}", result);

            ctx.complete(result);
        };
    }
}
