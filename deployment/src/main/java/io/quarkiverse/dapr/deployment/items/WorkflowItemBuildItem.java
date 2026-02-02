package io.quarkiverse.dapr.deployment.items;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class WorkflowItemBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;
    private final Type type;

    public WorkflowItemBuildItem(ClassInfo classInfo, Type type) {
        this.classInfo = classInfo;
        this.type = type;
    }

    public enum Type {
        WORKFLOW,
        WORKFLOW_ACTIVITY
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public Type getType() {
        return type;
    }

    public boolean isWorkflow() {
        return this.type == Type.WORKFLOW;
    }

    public boolean isWorkflowActivity() {
        return this.type == Type.WORKFLOW_ACTIVITY;
    }
}
