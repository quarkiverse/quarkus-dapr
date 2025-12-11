package io.quarkiverse.dapr.deployment;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;

import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.quarkiverse.dapr.config.DaprConfig;
import io.quarkiverse.dapr.deployment.items.WorkflowItemBuildItem;
import io.quarkiverse.dapr.workflows.WorkflowRuntimeBuilderRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;

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
            try {
                Class<?> workflowClass = Class.forName(workflow.name().toString(), false,
                        Thread.currentThread().getContextClassLoader());
                workflowItems.produce(new WorkflowItemBuildItem(workflowClass, WorkflowItemBuildItem.Type.WORKFLOW));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        for (ClassInfo activity : activities) {
            try {
                Class<?> workflowActivityClass = Class.forName(activity.name().toString(), false,
                        Thread.currentThread().getContextClassLoader());
                workflowItems.produce(
                        new WorkflowItemBuildItem(workflowActivityClass, WorkflowItemBuildItem.Type.WORKFLOW_ACTIVITY));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @BuildStep
    public void produceUnremovableBeans(BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            List<WorkflowItemBuildItem> workflowItems) {
        for (WorkflowItemBuildItem workflowItem : workflowItems) {
            unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(workflowItem.getClassName()));
        }
    }

    @BuildStep
    public void produceBeans(BuildProducer<AdditionalBeanBuildItem> beans, List<WorkflowItemBuildItem> workflowItems) {
        for (WorkflowItemBuildItem workflowItem : workflowItems) {
            beans.produce(new AdditionalBeanBuildItem(workflowItem.getClassName()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public SyntheticBeanBuildItem produceSyntheticBean(List<WorkflowItemBuildItem> workflowItems,
            WorkflowRuntimeBuilderRecorder recorder) {

        Set<Class> workflowsClasses = workflowItems.stream().filter(WorkflowItemBuildItem::isWorkflow)
                .map(WorkflowItemBuildItem::getClassName)
                .collect(Collectors.toSet());
        Set<Class> workflowActivityClasses = workflowItems.stream().filter(WorkflowItemBuildItem::isWorkflowActivity)
                .map(WorkflowItemBuildItem::getClassName)
                .collect(Collectors.toSet());

        return SyntheticBeanBuildItem.configure(
                WorkflowRuntimeBuilder.class)
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .runtimeProxy(recorder.build(workflowsClasses, workflowActivityClasses))
                .unremovable()
                .done();
    }

}
