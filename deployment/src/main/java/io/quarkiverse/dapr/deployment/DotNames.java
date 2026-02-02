package io.quarkiverse.dapr.deployment;

import org.jboss.jandex.DotName;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;

public class DotNames {

    public static final DotName WORKFLOW_DOTNAME = DotName.createSimple(Workflow.class);
    public static final DotName WORKFLOW_ACTIVITY_DOTNAME = DotName.createSimple(WorkflowActivity.class);
}
