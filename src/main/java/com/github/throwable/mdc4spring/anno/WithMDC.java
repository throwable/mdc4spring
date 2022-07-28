package com.github.throwable.mdc4spring.anno;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Creates a new MDC for method invocation.
 * <p>
 * The annotation is applicable to container managed beans, and indicates that the annotated method must be executed inside new MDC.
 * All parameters defined with <code>{@literal @}MDCParam</code> annotation or set explicitly with <code>MDC.param()</code>
 * inside the method's body will belong to this MDC and will automatically be removed after the method returns.
 * <p>
 * If any method annotated with ```WithMDC``` calls another method that has ```@WithMDC``` annotation too,
 * it will create a new 'nested' MDC that will be closed after the method returns removing only parameters defined inside it.
 * Any parameter defined in outer MDC will be also included in log messages.
 * <p>
 * When the annotation is placed at class level it will create a new MDC for all its methods invocations.
 * <h3>Limitations</h3>
 * The library uses Spring AOP to intercept annotated method invocations so these considerations must be token into account:
 * <ul>
 *     <li>The method must be invoked from outside the bean scope. Local calls are not intercepted by Spring AOP, thus any method annotation will be ignored in this case.</li>
 *     <li>Spring AOP does not intercept private methods, so if you invoke an inner bean private method, it will have no effect on it.</li>
 * </ul>
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
