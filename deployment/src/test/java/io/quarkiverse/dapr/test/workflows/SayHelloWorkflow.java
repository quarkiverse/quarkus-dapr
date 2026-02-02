package io.quarkiverse.dapr.test.workflows;

import jakarta.enterprise.context.ApplicationScoped;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

@ApplicationScoped
public class SayHelloWorkflow implements Workflow {

    @Override
    public WorkflowStub create() {
        return ctx -> {
            String await = ctx.callActivity(SayHelloWorkflowActivity.class.getName(), String.class).await();
            ctx.complete(await);
        };
    }
}
