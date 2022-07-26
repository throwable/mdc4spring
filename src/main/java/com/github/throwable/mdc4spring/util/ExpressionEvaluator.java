package com.github.throwable.mdc4spring.util;

import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Abstract expression evaluator
 */
@FunctionalInterface
public interface ExpressionEvaluator {
    /**
     * Evaluate expression for an MDC parameter
     * @param expression expression string to evaluate
     * @param rootObject in case of annotated method argument a value of that argument, in case of annotated method
     *                   a local bean instance (unproxied)
     * @param argumentValues null when evaluating an annotated method argument or all argument values when evaluating annotated
     *                       method
     * @return expression evaluation result
     */
    Object evaluate(String expression, Object rootObject, @Nullable Map<String, Object> argumentValues);
}
