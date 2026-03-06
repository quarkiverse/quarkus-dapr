package io.quarkiverse.dapr.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;

import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.quarkiverse.dapr.config.DaprConfig;
import io.quarkiverse.dapr.deployment.items.WorkflowItemBuildItem;
import io.quarkiverse.dapr.workflows.WorkflowRuntimeBuilderRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
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
            String regName = null;
            String version = null;
            Boolean isLatest = null;
            AnnotationInstance meta = workflow.annotation(DotNames.WORKFLOW_METADATA_DOTNAME);
            if (meta != null) {
                regName = stringValueOrNull(meta, "name");
                version = stringValueOrNull(meta, "version");
                AnnotationValue isLatestVal = meta.value("isLatest");
                if (isLatestVal != null) {
                    isLatest = isLatestVal.asBoolean();
                }
            }
            workflowItems.produce(new WorkflowItemBuildItem(
                    workflow, WorkflowItemBuildItem.Type.WORKFLOW, regName, version, isLatest));
        }

        for (ClassInfo activity : activities) {
            String regName = null;
            AnnotationInstance meta = activity.annotation(DotNames.ACTIVITY_METADATA_DOTNAME);
            if (meta != null) {
                regName = stringValueOrNull(meta, "name");
            }
            workflowItems.produce(new WorkflowItemBuildItem(
                    activity, WorkflowItemBuildItem.Type.WORKFLOW_ACTIVITY, regName, null, null));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public SyntheticBeanBuildItem produceSyntheticBean(List<WorkflowItemBuildItem> workflowItems,
            WorkflowRuntimeBuilderRecorder recorder) {

        if (workflowItems.isEmpty()) {
            return null;
        }

        Set<String> workflowClasses = new HashSet<>();
        Set<String> workflowActivityClasses = new HashSet<>();
        Map<String, String> workflowNames = new HashMap<>();
        Map<String, String> workflowVersions = new HashMap<>();
        Map<String, Boolean> workflowIsLatest = new HashMap<>();
        Map<String, String> activityNames = new HashMap<>();

        for (WorkflowItemBuildItem workflowItem : workflowItems) {
            String fqcn = workflowItem.getClassInfo().name().toString();
            if (workflowItem.isWorkflow()) {
                workflowClasses.add(fqcn);
                if (workflowItem.getRegistrationName() != null) {
                    workflowNames.put(fqcn, workflowItem.getRegistrationName());
                }
                if (workflowItem.getVersion() != null) {
                    workflowVersions.put(fqcn, workflowItem.getVersion());
                }
                if (workflowItem.getIsLatest() != null) {
                    workflowIsLatest.put(fqcn, workflowItem.getIsLatest());
                }
            } else {
                workflowActivityClasses.add(fqcn);
                if (workflowItem.getRegistrationName() != null) {
                    activityNames.put(fqcn, workflowItem.getRegistrationName());
                }
            }
        }

        return SyntheticBeanBuildItem.configure(
                WorkflowRuntimeBuilder.class)
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .supplier(recorder.build(workflowClasses, workflowActivityClasses,
                        workflowNames, workflowVersions, workflowIsLatest, activityNames))
                .unremovable()
                .done();
    }

    /**
     * Starts the workflow runtime after all RUNTIME_INIT synthetic beans
     * (including OpenTelemetry) have been initialized. This prevents the
     * Dapr SDK's DurableTaskGrpcWorker from calling GlobalOpenTelemetry.get()
     * before Quarkus OTel has initialized GlobalOpenTelemetry.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    public void startWorkflowRuntime(List<WorkflowItemBuildItem> workflowItems,
            ShutdownContextBuildItem shutdownContext,
            WorkflowRuntimeBuilderRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem) {

        if (workflowItems.isEmpty()) {
            return;
        }

        recorder.startRuntime(shutdownContext, launchModeBuildItem.getLaunchMode().isDevOrTest());
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

    private static String stringValueOrNull(AnnotationInstance annotation, String name) {
        AnnotationValue value = annotation.value(name);
        if (value == null) {
            return null;
        }
        String s = value.asString();
        return (s == null || s.isEmpty()) ? null : s;
    }

}
