package io.quarkiverse.dapr.workflows;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation for {@link io.dapr.workflows.WorkflowActivity} implementations
 * to specify a custom registration name.
 * <p>
 * When absent, the activity is registered using its fully-qualified class name
 * (the current default behavior).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivityMetadata {

    /**
     * Custom registration name for the activity.
     * When empty, the fully-qualified class name is used.
     */
    String name() default "";
}
