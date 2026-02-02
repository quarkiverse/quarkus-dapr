package io.quarkiverse.dapr.workflows.simple;

import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkus.logging.Log;

@ApplicationScoped
public class UppercaseWorkflowActivity implements WorkflowActivity {

    @Inject
    GreetingService greetingService;

    @Override
    public Object run(WorkflowActivityContext workflowActivityContext) {

        Log.info("Calling GreetingService: " + greetingService.saySomething());

        String input = workflowActivityContext.getInput(String.class);

        return input.toUpperCase(Locale.ROOT);
    }
}
