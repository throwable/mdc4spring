package com.github.throwable.mdc4spring.util;

import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.MDCScope;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

public class AnnotatedMethodMDCParamsResolver {

    private final Function<Method, String[]> parameterNamesDiscoverer; // = new DefaultParameterNameDiscoverer();
    private final ExpressionEvaluator expressionEvaluator;

    public AnnotatedMethodMDCParamsResolver(Function<Method, String[]> parameterNamesDiscoverer,
                                            ExpressionEvaluator expressionEvaluator)
    {
        this.parameterNamesDiscoverer = parameterNamesDiscoverer;
        this.expressionEvaluator = expressionEvaluator;
    }

    public MethodInvocationMDCScopeParametersValues resolveMethodInvocationScopeValues(Method method, Object target, Object[] args) {
        AnnotatedMethodConfig annotatedMethodConfig = resolveAnnotatedMethodConfig(method);
        if (annotatedMethodConfig == null)
            return new MethodInvocationMDCScopeParametersValues(
                    null, Collections.emptyMap(), null, Collections.emptyMap());

        Map<String, Object> beanScopeMDCParamValues = Collections.emptyMap();
        if (annotatedMethodConfig.getBeanMDCScope() != null && annotatedMethodConfig.getBeanMDCScope().parameters().length > 0) {
            beanScopeMDCParamValues = new HashMap<>(annotatedMethodConfig.getBeanMDCScope().parameters().length * 4 / 3 + 1);

            for (MDCParam parameter : annotatedMethodConfig.getBeanMDCScope().parameters()) {
                String paramName = !parameter.name().isEmpty() ? parameter.name() : parameter.value();
                if (paramName.isEmpty() || !parameter.include())
                    continue;
                Object expressionResult = evaluateExpression(parameter.expression(), target, null);
                beanScopeMDCParamValues.put(paramName, expressionResult);
            }
        }

        Map<String, Object> methodScopeMDCParamValues = null;
        Set<String> excludeKeys = Collections.emptySet();

        if (!annotatedMethodConfig.getMdcParamByParameterName().isEmpty()) {
            int estimatedCapacity = annotatedMethodConfig.getMdcParamByParameterName().size() +
                    (annotatedMethodConfig.getMethodMDCScope() != null ? annotatedMethodConfig.getMethodMDCScope().parameters().length : 0);
            methodScopeMDCParamValues = new HashMap<>(estimatedCapacity * 4 / 3 + 1);
            excludeKeys = new HashSet<>(annotatedMethodConfig.getMdcParamByParameterName().size() * 4 / 3 + 1);

            for (Map.Entry<String, MDCParam> argumentParam : annotatedMethodConfig.getMdcParamByParameterName().entrySet()) {
                String paramName = argumentParam.getKey();
                MDCParam parameter = argumentParam.getValue();
                Object argumentValue = args[annotatedMethodConfig.getParamIndex(paramName)];
                Object expressionResult;
                if (parameter.expression().isEmpty())
                    expressionResult = argumentValue;
                else
                    expressionResult = evaluateExpression(parameter.expression(), argumentValue, Collections.emptyMap());
                methodScopeMDCParamValues.put(paramName, expressionResult);
                if (!parameter.include())
                    excludeKeys.add(paramName);
            }
        }

        if (annotatedMethodConfig.getMethodMDCScope() != null && annotatedMethodConfig.getMethodMDCScope().parameters().length > 0) {
            // In MDCScope expression may access method arguments
            HashMap<String, Object> argumentValues = new HashMap<>(annotatedMethodConfig.getMethodMDCScope().parameters().length * 4 / 3 + 1);

            if (methodScopeMDCParamValues == null) {
                methodScopeMDCParamValues = new HashMap<>(annotatedMethodConfig.getMethodMDCScope().parameters().length * 4 / 3 + 1);
            }
            for (int i = 0; i < annotatedMethodConfig.getParameterNames().size(); i++) {
                String paramName = annotatedMethodConfig.getParameterNames().get(i);
                Object argumentValue;
                if (methodScopeMDCParamValues.containsKey(paramName))
                    argumentValue = methodScopeMDCParamValues.get(paramName);
                else argumentValue = args[i];
                argumentValues.put(paramName, argumentValue);
            }

            for (MDCParam parameter : annotatedMethodConfig.getMethodMDCScope().parameters()) {
                String paramName = !parameter.name().isEmpty() ? parameter.name() : parameter.value();
                if (paramName.isEmpty() || !parameter.include())
                    continue;
                Object expressionResult = evaluateExpression(parameter.expression(), target, argumentValues);
                methodScopeMDCParamValues.put(paramName, expressionResult);
            }
        }

        if (methodScopeMDCParamValues == null)
            methodScopeMDCParamValues = Collections.emptyMap();

        methodScopeMDCParamValues.keySet().removeAll(excludeKeys);

        return new MethodInvocationMDCScopeParametersValues(
                annotatedMethodConfig.getBeanMDCScope() != null ? annotatedMethodConfig.getBeanMDCScope().name() : null,
                beanScopeMDCParamValues,
                annotatedMethodConfig.getMethodMDCScope() != null ? annotatedMethodConfig.getMethodMDCScope().name() : null,
                methodScopeMDCParamValues
        );
    }

    private Object evaluateExpression(String expression, Object root, @Nullable Map<String, Object> argumentValues) {
        try {
            return expressionEvaluator.evaluate(expression, root, argumentValues);
        } catch (Exception e) {
            return "#EVALUATION ERROR#: " + e.getMessage();
        }
    }


    private AnnotatedMethodConfig resolveAnnotatedMethodConfig(Method method) {
        // TODO: cache
        MDCScope methodMDCScopeAnno = method.getAnnotation(MDCScope.class);
        MDCScope beanMDCScopeAnno = method.getDeclaringClass().getAnnotation(MDCScope.class);
        if (methodMDCScopeAnno == null && beanMDCScopeAnno == null)
            return null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        ArrayList<String> parameterNames = new ArrayList<>();
        Map<String, MDCParam> mdcParamMap = new HashMap<>();
        String[] parameterNamesDeclared = parameterNamesDiscoverer.apply(method);

        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            String parameterName = parameterNamesDeclared[i];
            MDCParam mdcParam = null;

            for (Annotation annotation : annotations) {
                if (MDCParam.class.equals(annotation.annotationType())) {
                    mdcParam = (MDCParam) annotation;
                    String paramName = !mdcParam.name().isEmpty() ? mdcParam.name() : mdcParam.value();
                    if (!paramName.isEmpty()) {
                        parameterName = paramName;
                    }
                }
            }
            parameterNames.add(parameterName);
            if (mdcParam != null)
                mdcParamMap.put(parameterName, mdcParam);
        }

        return new AnnotatedMethodConfig(beanMDCScopeAnno, methodMDCScopeAnno, parameterNames, mdcParamMap);
    }


    private static class AnnotatedMethodConfig {
        private final MDCScope beanMDCScope;
        private final MDCScope methodMDCScope;
        private final List<String> parameterNames;
        private final Map<String, MDCParam> mdcParamByParameterName;
        private final Map<String, Integer> paramIndex;

        private AnnotatedMethodConfig(MDCScope beanMDCScope, MDCScope methodMDCScope, List<String> parameterNames, Map<String, MDCParam> mdcParamByParameterName) {
            this.beanMDCScope = beanMDCScope;
            this.methodMDCScope = methodMDCScope;
            this.parameterNames = Collections.unmodifiableList(parameterNames);
            this.mdcParamByParameterName = Collections.unmodifiableMap(mdcParamByParameterName);
            paramIndex = new HashMap<>();
            for (int i = 0; i < parameterNames.size(); i++) {
                paramIndex.put(parameterNames.get(i), i);
            }
        }

        public MDCScope getBeanMDCScope() {
            return beanMDCScope;
        }

        public MDCScope getMethodMDCScope() {
            return methodMDCScope;
        }

        public List<String> getParameterNames() {
            return parameterNames;
        }

        public Map<String, MDCParam> getMdcParamByParameterName() {
            return mdcParamByParameterName;
        }

        public int getParamIndex(String paramName) {
            Integer idx = paramIndex.get(paramName);
            if (idx == null)
                throw new IllegalArgumentException("Wrong parameter name: " + paramName);
            return idx;
        }
    }
}
