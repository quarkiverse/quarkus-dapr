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

        Log.info("Before, let's call GreetingService: ");

        greetingService.sayHello();

        String input = workflowActivityContext.getInput(String.class);

        if (input == null || input.isEmpty()) {
            return "";
        }

        return input.toUpperCase(Locale.ROOT);
    }
}
