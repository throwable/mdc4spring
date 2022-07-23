package com.github.throwable.mdc4spring.anno;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Creates a new MDC for method invocation.
 * The annotation is applicable to container managed beans, and indicates that the annotated method must be executed inside new MDC.
 * All parameters defined with <code>{@literal @}MDCParam</code> annotation or set explicitly with <code>MDC.param()</code>
 * inside the method's body will belong to the new MDC.
 * The new MDC inherits all parameters already defined in current MDC (if any), so logging traces will always contain
 * the complete set of parameters defined by all MDCs in the chain.
 * <p>
 * After the method returns, the MDC will be closed, all its parameters will be removed from later logging traces and
 * a parent MDC will be restored (if any).
 * <p>
 * When placed at class level the annotation will create a new MDC for all its public methods.
 * <p>
 * <b>Note that this annotation only works for bean public methods that are invoked from outside the bean scope, and
 * will have no effect on any local method invocation.</b>
 * @see MDCParam
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface WithMDC {
    /**
     * MDC name (optional).
     * The name will be used as a prefix for all parameters defined inside the MDC scope.
     */
    @AliasFor("value")
    String name() default "";

    /**
     * MDC name (optional). Alias for <code>name</code>.
     */
    @AliasFor("name")
    String value() default "";
}
