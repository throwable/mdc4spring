package com.github.throwable.mdc4spring.util;

import org.springframework.lang.Nullable;

import java.util.Map;

public class MethodInvocationMDCParametersValues {

    private final String beanMDCNamespace;
    private final Map<String, Object> beanMDCParamValues;
    private final String methodMDCNamespace;
    private final Map<String, Object> methodMDCParamValues;

    public MethodInvocationMDCParametersValues(String beanMDCNamespace,
                                               Map<String, Object> beanMDCParamValues,
                                               String methodMDCNamespace,
                                               Map<String, Object> methodMDCParamValues)
    {
        this.beanMDCNamespace = beanMDCNamespace;
        this.beanMDCParamValues = beanMDCParamValues;
        this.methodMDCNamespace = methodMDCNamespace;
        this.methodMDCParamValues = methodMDCParamValues;
    }


    public String getMethodMDCNamespace() {
        return methodMDCNamespace;
    }

    public String getBeanMDCNamespace() {
        return beanMDCNamespace;
    }

    public Map<String, Object> getBeanMDCParamValues() {
        return beanMDCParamValues;
    }

    public Map<String, Object> getMethodMDCParamValues() {
        return methodMDCParamValues;
    }
}
