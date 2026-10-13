package io.quarkiverse.dapr.invoke;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Qualifier used to inject an interface annotated with {@link RegisterDaprInvokeClient}.
 * <p>
 * {@code @Inject} is optional when this qualifier is present on a field.
 */
@Qualifier
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DaprInvokeClient {

    final class Literal extends AnnotationLiteral<DaprInvokeClient> implements DaprInvokeClient {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;
    }
}
