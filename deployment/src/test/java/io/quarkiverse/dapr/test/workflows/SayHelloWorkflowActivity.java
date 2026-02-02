package io.quarkiverse.dapr.test.workflows;

import jakarta.enterprise.context.ApplicationScoped;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;

@ApplicationScoped
public class SayHelloWorkflowActivity implements WorkflowActivity {

    @Override
    public Object run(WorkflowActivityContext ctx) {
        return "hello";
    }
}
