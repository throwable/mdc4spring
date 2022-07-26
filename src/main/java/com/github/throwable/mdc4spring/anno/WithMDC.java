package com.github.throwable.mdc4spring.anno;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Creates a new MDC for method invocation.
 * If current MDC is already defined in the execution scope, a new one will be created leaving current MDC as a "parent"
 * and inheriting all parameters defined inside it. After the method returns it closes the "child" MDC clearing all the parameters
 * defined inside it's scope, and switches again to the "parent" MDC.
 * <p>
 * The annotation is applicable to container managed beans, and indicates that the annotated method must be executed inside new MDC.
 * All parameters defined with <code>{@literal @}MDCParam</code> annotation or set explicitly with <code>MDC.param()</code>
 * inside the method's body will belong to the new MDC.
 * <p>
 * When the annotation is placed at class level it will create a new MDC for all its methods invocations.
 * <p>
 * <h3>Limitations</h3>
 * Due to the nature of Spring AOP the annotation will work only on invocations of proxied methods, so these considerations
 * must be token into account:
 * <ul>
 *     <li>The method must be invoked from outside the bean scope. All local calls will not be intercepted by Spring AOP and method annotations will be ignored.</li>
 *     <li>Spring AOP does not intercepts private methods, so if you have invoke an inner beans private method it will have no effect on it.</li>
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
