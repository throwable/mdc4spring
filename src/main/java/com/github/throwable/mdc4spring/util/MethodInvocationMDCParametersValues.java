package com.github.throwable.mdc4spring.util;

import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Evaluated MDC parameters for method invocation
 */
public class MethodInvocationMDCParametersValues {
    @Nullable private final String beanMDCNamespace;
    private final Map<String, Object> beanMDCParamValues;
    @Nullable private final String methodMDCNamespace;
    private final Map<String, Object> methodMDCParamValues;
    private final boolean hasMDCParamOut;

    public MethodInvocationMDCParametersValues(@Nullable String beanMDCNamespace,
                                               Map<String, Object> beanMDCParamValues,
                                               @Nullable String methodMDCNamespace,
                                               Map<String, Object> methodMDCParamValues,
                                               boolean hasMDCParamOut)
    {
        this.beanMDCNamespace = beanMDCNamespace;
        this.beanMDCParamValues = beanMDCParamValues;
        this.methodMDCNamespace = methodMDCNamespace;
        this.methodMDCParamValues = methodMDCParamValues;
        this.hasMDCParamOut = hasMDCParamOut;
    }

    @Nullable
    public String getMethodMDCNamespace() {
        return methodMDCNamespace;
    }

    @Nullable
    public String getBeanMDCNamespace() {
        return beanMDCNamespace;
    }

    public Map<String, Object> getBeanMDCParamValues() {
        return beanMDCParamValues;
    }

    public Map<String, Object> getMethodMDCParamValues() {
        return methodMDCParamValues;
    }

    public boolean isHasMDCParamOut() {
        return hasMDCParamOut;
    }
}
