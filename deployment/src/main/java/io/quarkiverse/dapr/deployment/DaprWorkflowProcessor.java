package io.quarkiverse.dapr.deployment;

import java.util.ArrayList;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.quarkiverse.dapr.workflows.WorkflowRuntimeBuilderRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;

public class DaprWorkflowProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void produceSyntheticBean(BuildProducer<SyntheticBeanBuildItem> beans, ApplicationIndexBuildItem appIndex,
            WorkflowRuntimeBuilderRecorder recorder) {

        Index index = appIndex.getIndex();

        Set<ClassInfo> workflows = index.getAllKnownImplementors(DotNames.WORKFLOW_DOTNAME);
        Set<ClassInfo> activities = index.getAllKnownImplementors(DotNames.WORKFLOW_ACTIVITY_DOTNAME);

        ArrayList<Class<Workflow>> workflowClasses = new ArrayList<>();
        ArrayList<Class<WorkflowActivity>> workflowActivityClasses = new ArrayList<>();

        for (ClassInfo workflow : workflows) {
            try {
                Class<?> workflowClass = Class.forName(workflow.name().toString(), false,
                        Thread.currentThread().getContextClassLoader());
                workflowClasses.add((Class<Workflow>) workflowClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        for (ClassInfo activity : activities) {
            try {
                Class<?> workflowActivityClass = Class.forName(activity.name().toString(), false,
                        Thread.currentThread().getContextClassLoader());
                workflowActivityClasses.add((Class<WorkflowActivity>) workflowActivityClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        beans.produce(SyntheticBeanBuildItem.configure(
                WorkflowRuntimeBuilder.class)
                .scope(ApplicationScoped.class)
                .runtimeProxy(recorder.build(workflowClasses, workflowActivityClasses))
                .unremovable()
                .setRuntimeInit()
                .done());
    }

}
