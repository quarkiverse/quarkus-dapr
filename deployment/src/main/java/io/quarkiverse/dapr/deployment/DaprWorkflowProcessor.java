package io.quarkiverse.dapr.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;

import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.quarkiverse.dapr.config.DaprConfig;
import io.quarkiverse.dapr.deployment.items.WorkflowItemBuildItem;
import io.quarkiverse.dapr.workflows.WorkflowRuntimeBuilderRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;

public class DaprWorkflowProcessor {

    @BuildStep
    public void searchWorkflows(ApplicationIndexBuildItem appIndex, BuildProducer<WorkflowItemBuildItem> workflowItems,
            DaprConfig config) {

        if (!config.workflow().enabled()) {
            return;
        }

        Index index = appIndex.getIndex();
        Set<ClassInfo> workflows = index.getAllKnownImplementors(DotNames.WORKFLOW_DOTNAME);
        Set<ClassInfo> activities = index.getAllKnownImplementors(DotNames.WORKFLOW_ACTIVITY_DOTNAME);

        for (ClassInfo workflow : workflows) {
            workflowItems.produce(new WorkflowItemBuildItem(
                    workflow, WorkflowItemBuildItem.Type.WORKFLOW));
        }

        for (ClassInfo activity : activities) {
            workflowItems.produce(new WorkflowItemBuildItem(
                    activity, WorkflowItemBuildItem.Type.WORKFLOW_ACTIVITY));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public SyntheticBeanBuildItem produceSyntheticBean(List<WorkflowItemBuildItem> workflowItems,
            ShutdownContextBuildItem shutdownContext,
            WorkflowRuntimeBuilderRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem) {

        if (workflowItems.isEmpty()) {
            return null;
        }

        Set<String> workflowClasses = new HashSet<>();
        Set<String> workflowActivityClasses = new HashSet<>();
        for (WorkflowItemBuildItem workflowItem : workflowItems) {
            if (workflowItem.isWorkflow()) {
                workflowClasses.add(workflowItem.getClassInfo().name().toString());
            } else {
                workflowActivityClasses.add(workflowItem.getClassInfo().name().toString());
            }
        }

        return SyntheticBeanBuildItem.configure(
                WorkflowRuntimeBuilder.class)
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .supplier(recorder.build(workflowClasses, workflowActivityClasses, shutdownContext,
                        launchModeBuildItem.getLaunchMode().isDevOrTest()))
                .unremovable()
                .done();
    }

    @BuildStep
    void keepWorkflowAndActivities(
            List<WorkflowItemBuildItem> workflowItems,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {

        for (WorkflowItemBuildItem workflowItem : workflowItems) {
            unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(
                    workflowItem.getClassInfo().name().toString()));
        }
    }

}
