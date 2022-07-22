package com.github.throwable.mdc4spring.anno;

import java.lang.annotation.*;

/**
 *
 * The annotation is used on managed bean's public methods and indicates that bean's method must be executed inside new MDC.
 * All parameters defined in "parameters" attribute or set inside the method's scope will belong to current MDC.
 * After the method returns the MDC will be closed and all defined parameters will be removed from later traces.
 * Any parameter defined in outer methods' scopes will still persist in traces.
 * When placed at class level the annotation will define the same MDC for all its public methods.
 * <p>
 * Sample usage:
 * <blockquote><pre>
 * {@literal @}WithMDC(name = "orders", parameters = {
 *     {@literal @}MDCParam(name = "transactionId", eval = "#order.transactionId"),
 *     {@literal @}MDCParam(name = "clientId", eval = "#order.clientId")
 * })
 * public void createOrder(Order order, {@literal @}MDCParam(eval = "name") Queue queue, {@literal @}MDCParam Priority priority)
 * </pre></blockquote>
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
    String name() default "";

    /**
     * List of predefined parameters for current MDC.
     * An {@literal @}MDCParam must define a unique name and expression that evaluates its value.
     * <p>
     * The evaluation context will contain references to:
     * <ul>
     *     <li>Local bean as a <code>#root</code> object. You have an access to bean's private fields and properties.</li>
     *     <li>All method's arguments using <code>#argumentName</code> variable</li>
     *     <li>Spring configuration properties available with <code>#environment</code> map</li>
     *     <li>System properties at <code>#systemProperties</code> variable</li>
     * </ul>
     * <p>
     * For more information please refer to <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions">Spring Expression Language</a> documentation.
     * @see MDCParam
     */
    MDCParam[] parameters() default {};
}
