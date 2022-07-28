package com.github.throwable.mdc4spring.anno;

import java.lang.annotation.*;

/**
 * Defines a parameter that will be added to current MDC after the method returns.
 * The parameter will be present in all log messages occurred after the method invocation.
 * @see WithMDC
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Repeatable(MDCOutParams.class)
public @interface MDCOutParam {
    /**
     * Output parameter name (required).
     * @return Output parameter name (required).
     */
    String name();

    /**
     * Expression to evaluate (optional). The expression is evaluated using method's return value as #root object.
     * If omitted a return value is returned as is.
     * For more information please refer to <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions">Spring Expression Language</a> documentation.
     * @return Expression to evaluate.
     */
    String eval() default "#this";
}
