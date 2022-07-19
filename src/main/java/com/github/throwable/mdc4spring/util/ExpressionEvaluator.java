package com.github.throwable.mdc4spring.util;

import org.springframework.lang.Nullable;

import java.util.Map;

@FunctionalInterface
public interface ExpressionEvaluator {
    Object evaluate(String expression, Object rootObject, @Nullable Map<String, Object> argumentValues);
}
