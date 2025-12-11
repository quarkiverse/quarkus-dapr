package io.quarkiverse.dapr.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;

public final class WorkflowItemBuildItem extends MultiBuildItem {

    public enum Type {
        WORKFLOW,
        WORKFLOW_ACTIVITY
    }

    private final Class<?> clazz;
    private final Type type;

    public WorkflowItemBuildItem(Class<?> clazz, Type type) {
        this.clazz = clazz;
        this.type = type;
    }

    public Class<?> getClassName() {
        return clazz;
    }

    public Type getType() {
        return type;
    }

    public boolean isWorkflow() {
        return this.type.equals(Type.WORKFLOW);
    }

    public boolean isWorkflowActivity() {
        return this.type.equals(Type.WORKFLOW_ACTIVITY);
    }
}
