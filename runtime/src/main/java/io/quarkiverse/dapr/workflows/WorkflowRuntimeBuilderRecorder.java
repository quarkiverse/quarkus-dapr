package io.quarkiverse.dapr.workflows;

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
import io.quarkus.arc.Arc;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class WorkflowRuntimeBuilderRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowRuntimeBuilderRecorder.class);

    public Supplier<WorkflowRuntimeBuilder> build(Set<String> workflows,
            Set<String> workflowActivityClasses, ShutdownContext shutdownContext, boolean isDevOrTest) {

        WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder();

        for (String workflow : workflows) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> aClass = Class.forName(workflow, true, contextClassLoader);
                Workflow w = (Workflow) Arc.container().instance(aClass, Any.Literal.INSTANCE).get();
                builder.registerTaskOrchestrationFactory(workflow, new TaskOrchestrationFactory() {
                    @Override
                    public String getName() {
                        return workflow;
                    }

                    @Override
                    public TaskOrchestration create() {
                        return ctx -> w.run(new DefaultWorkflowContext(ctx, w.getClass()));
                    }

                    @Override
                    public String getVersionName() {
                        return "";
                    }

                    @Override
                    public Boolean isLatestVersion() {
                        return null;
                    }
                });
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to register the Workflow called" + workflow, e);
            }
        }

        for (String workflowActivity : workflowActivityClasses) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> aClass = Class.forName(workflowActivity, true, contextClassLoader);
                WorkflowActivity wa = (WorkflowActivity) Arc.container().instance(aClass, Any.Literal.INSTANCE).get();

                builder.registerTaskActivityFactory(workflowActivity, new TaskActivityFactory() {
                    @Override
                    public String getName() {
                        return workflowActivity;
                    }

                    @Override
                    public TaskActivity create() {
                        return ctx -> wa.run(new DefaultWorkflowActivityContext(ctx, aClass));
                    }
                });

            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to register the WorkflowActivity called" + workflowActivity, e);
            }
        }

        WorkflowRuntime runtime = builder.build();

        runtime.start(false);

        if (!isDevOrTest) {
            shutdownContext.addShutdownTask(() -> {
                LOG.info("Closing Dapr Workflow runtime resources. This may take a while.");
                runtime.close();
            });
        }
        return new Supplier<WorkflowRuntimeBuilder>() {
            @Override
            public WorkflowRuntimeBuilder get() {
                return builder;
            }
        };
    }
}
