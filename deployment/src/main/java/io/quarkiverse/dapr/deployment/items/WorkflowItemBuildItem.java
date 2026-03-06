package io.quarkiverse.dapr.deployment.items;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class WorkflowItemBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;
    private final Type type;
    private final String registrationName;
    private final String version;
    private final Boolean isLatest;

    public WorkflowItemBuildItem(ClassInfo classInfo, Type type) {
        this(classInfo, type, null, null, null);
    }

    public WorkflowItemBuildItem(ClassInfo classInfo, Type type,
            String registrationName, String version, Boolean isLatest) {
        this.classInfo = classInfo;
        this.type = type;
        this.registrationName = registrationName;
        this.version = version;
        this.isLatest = isLatest;
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

    /**
     * Returns the custom registration name from the annotation, or {@code null}
     * if no annotation was present or the name was empty.
     */
    public String getRegistrationName() {
        return registrationName;
    }

    /**
     * Returns the version from {@code @WorkflowMetadata}, or {@code null}.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the isLatest flag from {@code @WorkflowMetadata}, or {@code null}.
     */
    public Boolean getIsLatest() {
        return isLatest;
    }
}
