package com.github.throwable.mdc4spring.anno;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Defines an MDC parameter for current method's invocation scope. The parameter will be present in all logging traces
 * occurred inside the method. After the method returns all defined parameters will be automatically removed from current MDC
 * and later logging traces.
 * <p>
 * The annotation may be set at method level, at bean level or for any of method's arguments.
 * <p>
 * <h3>Annotating method arguments</h3>
 * When <code>{@literal @}MDCParam</code> annotates method argument, a new parameter will be included to the method MDC
 * during method invocation. By default, the parameter will have the same name as the argument and the value the method invoked with.
 * Alternatively you can specify a custom name for the parameter or define an expression that converts its original value.
 * <p>
 * <h3>Annotating methods</h3>
 * When annotating a method with <code>{@literal @}MDCParam</code> you can define additional parameters that will be
 * included to MDC during the method invocation. For each parameter you need to specify a unique name and an expression
 * that will be evaluated to obtain parameter's value.
 * The evaluation context contains references to:
 * <ul>
 *     <li>Local bean as <code>#root</code> object. You have access to bean's private fields and properties.</li>
 *     <li>All method's invocation values referenced by <code>#argumentName</code> variables.</li>
 *     <li>Spring configuration properties available in <code>#environment</code> map.</li>
 *     <li>System properties referenced by <code>#systemProperties</code> variable.</li>
 * </ul>
 * <p>
 * For more information please refer to <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions">Spring Expression Language</a> documentation.
 * <p>
 * <h3>Annotating beans</h3>
 * The <code>{@literal @}MDCParam</code> annotation set at bean level has the same effect as it will be applied to all bean's public methods.
 * <p>
 * <b>Note that this annotation only works for bean public methods that are invoked from outside the bean scope, and
 * will have no effect on any local method invocation.</b>
 * <p>
 * Sample usage:
 * <blockquote><pre>
 * {@literal @}MDCParam(name = "transaction.id", eval = "#order.transactionId"),
 * {@literal @}MDCParam(name = "client.id", eval = "#order.clientId")
 *  public void createOrder(Order order,
 *                         {@literal @}MDCParam(eval = "name") Queue queue,
 *                         {@literal @}MDCParam Priority priority,
 *                         {@literal @}MDCParam("user.id") String userId)
 * </pre></blockquote>
 * @see WithMDC
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Repeatable(MDCParams.class)
public @interface MDCParam {
    /**
     * Parameter's name. Optional for method argument annotation, required for method and bean-level annotations.
     */
    @AliasFor("value")
    String name() default "";

    /**
     * Parameter's name. Alias for <code>name</code> attribute.
     */
    @AliasFor("name")
    String value() default "";

    /**
     * Expression to evaluate.
     * For more information please refer to <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions">Spring Expression Language</a> documentation.
     */
    String eval() default "";
}
