package com.github.throwable.mdc4spring.util;

import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.WithMDC;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class AnnotatedMethodMDCParamsResolver {
    private static final ConcurrentHashMap<String, AnnotatedMethodConfig> annotatedMethodConfigCache = new ConcurrentHashMap<>();

    private final Function<Method, String[]> parameterNamesDiscoverer; // = new DefaultParameterNameDiscoverer();
    private final ExpressionEvaluator expressionEvaluator;

    public AnnotatedMethodMDCParamsResolver(Function<Method, String[]> parameterNamesDiscoverer,
                                            ExpressionEvaluator expressionEvaluator)
    {
        this.parameterNamesDiscoverer = parameterNamesDiscoverer;
        this.expressionEvaluator = expressionEvaluator;
    }

    public MethodInvocationMDCParametersValues resolveMethodInvocationMDCParamValues(Method method, Object target, Object[] args) {
        AnnotatedMethodConfig annotatedMethodConfig = resolveAnnotatedMethodConfig(method);
        if (annotatedMethodConfig == null)
            return new MethodInvocationMDCParametersValues(
                    null, Collections.emptyMap(), null, Collections.emptyMap());

        Map<String, Object> beanMDCParamValues = Collections.emptyMap();
        if (annotatedMethodConfig.getBeanMDCAnno() != null && annotatedMethodConfig.getBeanMDCAnno().parameters().length > 0) {
            beanMDCParamValues = new HashMap<>(annotatedMethodConfig.getBeanMDCAnno().parameters().length * 4 / 3 + 1);

            for (MDCParam parameter : annotatedMethodConfig.getBeanMDCAnno().parameters()) {
                String paramName = !parameter.name().isEmpty() ? parameter.name() : parameter.value();
                if (paramName.isEmpty() || !parameter.include())
                    continue;
                Object expressionResult = evaluateExpression(parameter.expression(), target, null);
                beanMDCParamValues.put(paramName, expressionResult);
            }
        }

        Map<String, Object> methodMDCParamValues = null;
        Set<String> excludeKeys = Collections.emptySet();

        if (!annotatedMethodConfig.getMdcParamByParameterName().isEmpty()) {
            int estimatedCapacity = annotatedMethodConfig.getMdcParamByParameterName().size() +
                    (annotatedMethodConfig.getMethodMDCAnno() != null ? annotatedMethodConfig.getMethodMDCAnno().parameters().length : 0);
            methodMDCParamValues = new HashMap<>(estimatedCapacity * 4 / 3 + 1);
            excludeKeys = new HashSet<>(annotatedMethodConfig.getMdcParamByParameterName().size() * 4 / 3 + 1);

            for (Map.Entry<String, MDCParam> argumentParam : annotatedMethodConfig.getMdcParamByParameterName().entrySet()) {
                String paramName = argumentParam.getKey();
                MDCParam parameter = argumentParam.getValue();
                Object argumentValue = args[annotatedMethodConfig.getParamIndex(paramName)];
                Object expressionResult;
                if (parameter.expression().isEmpty())
                    expressionResult = argumentValue;
                else
                    expressionResult = evaluateExpression(parameter.expression(), argumentValue, null);
                methodMDCParamValues.put(paramName, expressionResult);
                if (!parameter.include())
                    excludeKeys.add(paramName);
            }
        }

        if (annotatedMethodConfig.getMethodMDCAnno() != null && annotatedMethodConfig.getMethodMDCAnno().parameters().length > 0) {
            // In @WithMDC expression may access method arguments
            HashMap<String, Object> argumentValues = new HashMap<>(annotatedMethodConfig.getMethodMDCAnno().parameters().length * 4 / 3 + 1);

            if (methodMDCParamValues == null) {
                methodMDCParamValues = new HashMap<>(annotatedMethodConfig.getMethodMDCAnno().parameters().length * 4 / 3 + 1);
            }
            for (int i = 0; i < annotatedMethodConfig.getParameterNames().size(); i++) {
                String paramName = annotatedMethodConfig.getParameterNames().get(i);
                Object argumentValue;
                if (methodMDCParamValues.containsKey(paramName))
                    argumentValue = methodMDCParamValues.get(paramName);
                else argumentValue = args[i];
                argumentValues.put(paramName, argumentValue);
            }

            for (MDCParam parameter : annotatedMethodConfig.getMethodMDCAnno().parameters()) {
                String paramName = !parameter.name().isEmpty() ? parameter.name() : parameter.value();
                if (paramName.isEmpty() || !parameter.include())
                    continue;
                Object expressionResult = evaluateExpression(parameter.expression(), target, argumentValues);
                methodMDCParamValues.put(paramName, expressionResult);
            }
        }

        if (methodMDCParamValues == null)
            methodMDCParamValues = Collections.emptyMap();

        methodMDCParamValues.keySet().removeAll(excludeKeys);

        return new MethodInvocationMDCParametersValues(
                annotatedMethodConfig.getBeanMDCAnno() != null ? annotatedMethodConfig.getBeanMDCAnno().name() : null,
                beanMDCParamValues,
                annotatedMethodConfig.getMethodMDCAnno() != null ? annotatedMethodConfig.getMethodMDCAnno().name() : null,
                methodMDCParamValues
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
        String methodId = method.getDeclaringClass().getName() + "/" + method.getName();
        AnnotatedMethodConfig config = annotatedMethodConfigCache.get(methodId);

        if (config == null) {
            WithMDC methodMDCAnno = method.getAnnotation(WithMDC.class);
            WithMDC beanMDCAnno = method.getDeclaringClass().getAnnotation(WithMDC.class);
            if (methodMDCAnno == null && beanMDCAnno == null)
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

            config = new AnnotatedMethodConfig(beanMDCAnno, methodMDCAnno, parameterNames, mdcParamMap);
            final AnnotatedMethodConfig configUpdated = annotatedMethodConfigCache.putIfAbsent(methodId, config);
            if (configUpdated != null)
                config = configUpdated;
        }
        return config;
    }


    private static class AnnotatedMethodConfig {
        private final WithMDC beanMDCAnno;
        private final WithMDC methodMDCAnno;
        private final List<String> parameterNames;
        private final Map<String, MDCParam> mdcParamByParameterName;
        private final Map<String, Integer> paramIndex;

        private AnnotatedMethodConfig(WithMDC beanMDCAnno, WithMDC methodMDCAnno, List<String> parameterNames, Map<String, MDCParam> mdcParamByParameterName) {
            this.beanMDCAnno = beanMDCAnno;
            this.methodMDCAnno = methodMDCAnno;
            this.parameterNames = Collections.unmodifiableList(parameterNames);
            this.mdcParamByParameterName = Collections.unmodifiableMap(mdcParamByParameterName);
            paramIndex = new HashMap<>();
            for (int i = 0; i < parameterNames.size(); i++) {
                paramIndex.put(parameterNames.get(i), i);
            }
        }

        public WithMDC getBeanMDCAnno() {
            return beanMDCAnno;
        }

        public WithMDC getMethodMDCAnno() {
            return methodMDCAnno;
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
