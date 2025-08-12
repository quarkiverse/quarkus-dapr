package io.quarkiverse.dapr.workflows;

import java.util.Set;

import jakarta.enterprise.inject.spi.CDI;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.quarkus.arc.ClientProxy;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class WorkflowRuntimeBuilderRecorder {

    public WorkflowRuntimeBuilder build(Set<Class> workflows,
            Set<Class> workflowActivityClasses) {

        WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder();

        for (Class<Workflow> workflow : workflows) {
            Workflow workflowInstance = ClientProxy.unwrap(CDI.current().select(workflow).get());
            builder.registerWorkflow(workflowInstance);
        }

        for (Class<WorkflowActivity> activityClass : workflowActivityClasses) {
            WorkflowActivity activityInstance = ClientProxy.unwrap(CDI.current().select(activityClass).get());
            builder.registerActivity(activityInstance);
        }

        WorkflowRuntime runtime = builder.build();

        runtime.start(false);

        return builder;
    }
}
