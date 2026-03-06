package io.quarkiverse.dapr.deployment;

import org.jboss.jandex.DotName;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.quarkiverse.dapr.workflows.ActivityMetadata;
import io.quarkiverse.dapr.workflows.WorkflowMetadata;

public class DotNames {

    public static final DotName WORKFLOW_DOTNAME = DotName.createSimple(Workflow.class);
    public static final DotName WORKFLOW_ACTIVITY_DOTNAME = DotName.createSimple(WorkflowActivity.class);
    public static final DotName WORKFLOW_METADATA_DOTNAME = DotName.createSimple(WorkflowMetadata.class);
    public static final DotName ACTIVITY_METADATA_DOTNAME = DotName.createSimple(ActivityMetadata.class);
}
