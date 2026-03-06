package io.quarkiverse.dapr.workflows;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Any;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dapr.durabletask.TaskActivity;
import io.dapr.durabletask.TaskActivityFactory;
import io.dapr.durabletask.TaskOrchestration;
import io.dapr.durabletask.orchestration.TaskOrchestrationFactory;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.runtime.DefaultWorkflowActivityContext;
import io.dapr.workflows.runtime.DefaultWorkflowContext;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class WorkflowRuntimeBuilderRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowRuntimeBuilderRecorder.class);

    /**
     * Registers workflow and activity factories on a {@link WorkflowRuntimeBuilder}.
     * This runs during RUNTIME_INIT as a synthetic bean supplier. It must NOT call
     * {@code builder.build()} because that creates a {@code DurableTaskGrpcWorker}
     * which eagerly calls {@code GlobalOpenTelemetry.get()}, and at this point the
     * Quarkus OTel extension has not yet initialized GlobalOpenTelemetry.
     */
    public Supplier<WorkflowRuntimeBuilder> build(Set<String> workflows,
            Set<String> workflowActivityClasses,
            Map<String, String> workflowNames,
            Map<String, String> workflowVersions,
            Map<String, Boolean> workflowIsLatest,
            Map<String, String> activityNames) {

        WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder();

        for (String workflow : workflows) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> aClass = Class.forName(workflow, true, contextClassLoader);
                Workflow w = (Workflow) Arc.container().instance(aClass, Any.Literal.INSTANCE).get();

                String customName = workflowNames.get(workflow);
                String version = workflowVersions.getOrDefault(workflow, "");
                Boolean isLatest = workflowIsLatest.get(workflow);

                String name = customName != null ? customName : workflow;
                registerOrchestration(builder, name, version, isLatest, w);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to register the Workflow called " + workflow, e);
            }
        }

        for (String workflowActivity : workflowActivityClasses) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> aClass = Class.forName(workflowActivity, true, contextClassLoader);
                WorkflowActivity wa = (WorkflowActivity) Arc.container().instance(aClass, Any.Literal.INSTANCE).get();

                String customName = activityNames.get(workflowActivity);

                String name = customName != null ? customName : workflowActivity;
                registerActivity(builder, name, wa, aClass);

            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to register the WorkflowActivity called " + workflowActivity, e);
            }
        }

        return new Supplier<WorkflowRuntimeBuilder>() {
            @Override
            public WorkflowRuntimeBuilder get() {
                return builder;
            }
        };
    }

    private static void registerOrchestration(WorkflowRuntimeBuilder builder,
            String name, String version, Boolean isLatest, Workflow w) {
        builder.registerTaskOrchestrationFactory(name, new TaskOrchestrationFactory() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public TaskOrchestration create() {
                return ctx -> w.run(new DefaultWorkflowContext(ctx, w.getClass()));
            }

            @Override
            public String getVersionName() {
                return version;
            }

            @Override
            public Boolean isLatestVersion() {
                return isLatest;
            }
        });
    }

    private static void registerActivity(WorkflowRuntimeBuilder builder,
            String name, WorkflowActivity wa, Class<?> aClass) {
        builder.registerTaskActivityFactory(name, new TaskActivityFactory() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public TaskActivity create() {
                return ctx -> wa.run(new DefaultWorkflowActivityContext(ctx, aClass));
            }
        });
    }

    /**
     * Starts the workflow runtime. Called from a separate build step that runs
     * after all RUNTIME_INIT synthetic beans (including OpenTelemetry) have been
     * initialized. Bridges the Quarkus CDI OpenTelemetry bean to
     * GlobalOpenTelemetry so the Dapr SDK and WorkflowTracing get a real tracer.
     */
    public void startRuntime(ShutdownContext shutdownContext, boolean isDevOrTest) {
        // Quarkus manages the OpenTelemetry SDK instance via CDI and does NOT call
        // GlobalOpenTelemetry.set(). Both WorkflowTracing and the Dapr SDK's
        // DurableTaskGrpcWorker read from GlobalOpenTelemetry, so we must bridge
        // the CDI bean to the global static before the workflow runtime is built.
        InstanceHandle<OpenTelemetry> otelHandle = Arc.container().instance(OpenTelemetry.class);
        if (otelHandle.isAvailable()) {
            OpenTelemetry otel = otelHandle.get();
            try {
                GlobalOpenTelemetry.set(otel);
                LOG.debug("Bridged Quarkus OpenTelemetry CDI bean to GlobalOpenTelemetry");
            } catch (IllegalStateException ignored) {
                // Already set — happens on dev mode live-reload or test restarts
                // because GlobalOpenTelemetry enforces set-once semantics.
                LOG.debug("GlobalOpenTelemetry already set, skipping bridge");
            }
        }

        InstanceHandle<WorkflowRuntimeBuilder> builderHandle = Arc.container().instance(WorkflowRuntimeBuilder.class);
        if (!builderHandle.isAvailable()) {
            return;
        }

        WorkflowRuntimeBuilder builder = builderHandle.get();
        WorkflowRuntime runtime = builder.build();

        runtime.start(false);

        if (!isDevOrTest) {
            shutdownContext.addShutdownTask(() -> {
                LOG.info("Closing Dapr Workflow runtime resources. This may take a while.");
                runtime.close();
            });
        }
    }
}
