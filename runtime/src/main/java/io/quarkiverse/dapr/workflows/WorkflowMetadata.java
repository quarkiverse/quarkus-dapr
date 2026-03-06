package io.quarkiverse.dapr.workflows;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation for {@link io.dapr.workflows.Workflow} implementations
 * to specify a custom registration name, version, and latest-version flag.
 * <p>
 * When absent, the workflow is registered using its fully-qualified class name
 * with no version information (the current default behavior).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WorkflowMetadata {

    /**
     * Custom registration name for the workflow.
     * When empty, the fully-qualified class name is used.
     */
    String name() default "";

    /**
     * Version of the workflow.
     * When specified, {@link #name()} must also be provided.
     */
    String version() default "";

    /**
     * Whether this version is the latest.
     * When {@code true}, {@link #name()} and {@link #version()} must be provided.
     */
    boolean isLatest() default false;
}
