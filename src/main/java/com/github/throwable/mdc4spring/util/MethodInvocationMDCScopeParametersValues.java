package com.github.throwable.mdc4spring.util;

import org.springframework.lang.Nullable;

import java.util.Map;

public class MethodInvocationMDCScopeParametersValues {

    @Nullable private final String beanScopeNamespace;
    private final Map<String, Object> beanScopeMDCParamValues;
    @Nullable private final String methodScopeNamespace;
    private final Map<String, Object> methodScopeMDCParamValues;

    public MethodInvocationMDCScopeParametersValues(@Nullable String beanScopeNamespace,
                                                    Map<String, Object> beanScopeMDCParamValues,
                                                    @Nullable String methodScopeNamespace,
                                                    Map<String, Object> methodScopeMDCParameters)
    {
        this.beanScopeNamespace = beanScopeNamespace;
        this.beanScopeMDCParamValues = beanScopeMDCParamValues;
        this.methodScopeNamespace = methodScopeNamespace;
        this.methodScopeMDCParamValues = methodScopeMDCParameters;
    }


    @Nullable
    public String getMethodScopeNamespace() {
        return methodScopeNamespace;
    }

    @Nullable
    public String getBeanScopeNamespace() {
        return beanScopeNamespace;
    }

    public Map<String, Object> getBeanScopeMDCParamValues() {
        return beanScopeMDCParamValues;
    }

    public Map<String, Object> getMethodScopeMDCParamValues() {
        return methodScopeMDCParamValues;
    }
}
