package io.quarkiverse.dapr.workflows;

import java.util.ArrayList;
import java.util.List;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class WorkflowRuntimeBuilderRecorder {

    public WorkflowRuntimeBuilder build(List<Class<Workflow>> workflows,
            ArrayList<Class<WorkflowActivity>> workflowActivityClasses) {

        WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder();

        for (Class<Workflow> workflow : workflows) {
            builder.registerWorkflow(workflow);
        }

        for (Class<WorkflowActivity> activityClass : workflowActivityClasses) {
            builder.registerActivity(activityClass);
        }

        WorkflowRuntime runtime = builder.build();

        runtime.start(false);

        return builder;
    }
}
