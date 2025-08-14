package io.quarkiverse.dapr.workflows.simple;

import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;

@ApplicationScoped
public class UppercaseWorkflowActivity implements WorkflowActivity {
    @Override
    public Object run(WorkflowActivityContext workflowActivityContext) {
        String input = workflowActivityContext.getInput(String.class);

        if (input == null || input.isEmpty()) {
            return "";
        }

        return input.toUpperCase(Locale.ROOT);
    }
}
