package com.github.throwable.mdc4spring.util;

import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.MDCParams;
import com.github.throwable.mdc4spring.anno.WithMDC;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Class that resolves a method MDC configuration and evaluates MDC parameters for a method invocation.
 */
public class AnnotatedMethodMDCParamsEvaluator {
    private static final ConcurrentHashMap<String, AnnotatedMethodConfig> annotatedMethodConfigCache = new ConcurrentHashMap<>();

    private final Function<Method, String[]> argumentsNamesDiscoverer; // = new DefaultParameterNameDiscoverer();
    private final ExpressionEvaluator expressionEvaluator;

    public AnnotatedMethodMDCParamsEvaluator(Function<Method, String[]> argumentsNamesDiscoverer,
                                             ExpressionEvaluator expressionEvaluator)
    {
        this.argumentsNamesDiscoverer = argumentsNamesDiscoverer;
        this.expressionEvaluator = expressionEvaluator;
    }

    /**
     * Evaluate method MDC parameters for a particular method invocation.
     * @param method method to invoke
     * @param target target object instance
     * @param args method arguments values
     * @return method's MDC configuration and evaluated parameters with their values
     */
    @Nullable
    public MethodInvocationMDCParametersValues evalMethodInvocationMDCParamValues(
            Method method, Object target, Object[] args)
    {
        AnnotatedMethodConfig annotatedMethodConfig = resolveAnnotatedMethodConfig(method);
        if (annotatedMethodConfig == null)
            return null;

        Map<String, Object> beanMDCParamValues = Collections.emptyMap();
        if (!annotatedMethodConfig.getBeanMDCParamAnnotations().isEmpty()) {
            beanMDCParamValues = new HashMap<>(annotatedMethodConfig.getBeanMDCParamAnnotations().size() * 4 / 3 + 1);

            for (MDCParam parameter : annotatedMethodConfig.getBeanMDCParamAnnotations()) {
                String paramName = !parameter.name().isEmpty() ? parameter.name() : parameter.value();
                if (paramName.isEmpty()/* || !parameter.include()*/)
                    continue;
                Object expressionResult = evaluateExpression(parameter.eval(), target, null,
                        annotatedMethodConfig.getExpressionStaticVariables());
                beanMDCParamValues.put(paramName, expressionResult);
            }
        }

        Map<String, Object> methodMDCParamValues = null;

        if (!annotatedMethodConfig.getMdcParamByArgumentName().isEmpty()) {
            int estimatedCapacity = annotatedMethodConfig.getMdcParamByArgumentName().size() +
                    (annotatedMethodConfig.getMethodMDCParamAnnotations().size());
            methodMDCParamValues = new HashMap<>(estimatedCapacity * 4 / 3 + 1);

            for (Map.Entry<String, MDCParam> argumentParam : annotatedMethodConfig.getMdcParamByArgumentName().entrySet()) {
                String paramName = argumentParam.getKey();
                MDCParam parameter = argumentParam.getValue();
                Object argumentValue = args[annotatedMethodConfig.getArgumentParamIndex(paramName)];
                Object expressionResult;
                if (parameter.eval().isEmpty())
                    expressionResult = argumentValue;
                else
                    expressionResult = evaluateExpression(parameter.eval(), argumentValue, null,
                            annotatedMethodConfig.getExpressionStaticVariables());
                methodMDCParamValues.put(paramName, expressionResult);
            }
        }

        if (!annotatedMethodConfig.getMethodMDCParamAnnotations().isEmpty()) {
            // In @WithMDC expression may access method arguments
            HashMap<String, Object> argumentValues = new HashMap<>(annotatedMethodConfig.getMethodMDCParamAnnotations().size() * 4 / 3 + 1);

            if (methodMDCParamValues == null) {
                methodMDCParamValues = new HashMap<>(annotatedMethodConfig.getMethodMDCParamAnnotations().size() * 4 / 3 + 1);
            }
            for (int i = 0; i < annotatedMethodConfig.getArgumentNames().size(); i++) {
                String paramName = annotatedMethodConfig.getArgumentNames().get(i);
                Object argumentValue;
                if (methodMDCParamValues.containsKey(paramName))
                    argumentValue = methodMDCParamValues.get(paramName);
                else argumentValue = args[i];
                argumentValues.put(paramName, argumentValue);
            }

            for (MDCParam parameter : annotatedMethodConfig.getMethodMDCParamAnnotations()) {
                String paramName = !parameter.name().isEmpty() ? parameter.name() : parameter.value();
                if (paramName.isEmpty()/* || !parameter.include()*/)
                    continue;
                Object expressionResult = evaluateExpression(parameter.eval(), target, argumentValues,
                        annotatedMethodConfig.getExpressionStaticVariables());
                methodMDCParamValues.put(paramName, expressionResult);
            }
        }

        if (methodMDCParamValues == null)
            methodMDCParamValues = Collections.emptyMap();

        //methodMDCParamValues.keySet().removeAll(excludeKeys);

        return new MethodInvocationMDCParametersValues(
                annotatedMethodConfig.getBeanMDCAnno() != null ? annotatedMethodConfig.getBeanMDCAnno().name() : null,
                beanMDCParamValues,
                annotatedMethodConfig.getMethodMDCAnno() != null ? annotatedMethodConfig.getMethodMDCAnno().name() : null,
                methodMDCParamValues
        );
    }

    private Object evaluateExpression(String expression, Object root,
                                      @Nullable Map<String, Object> argumentValues,
                                      Map<String, Object> environmentVariables) {
        try {
            return expressionEvaluator.evaluate(expression, root, argumentValues, environmentVariables);
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
            final MDCParam methodMDCParamAnno = method.getAnnotation(MDCParam.class);
            final MDCParam beanMDCParamAnno = method.getDeclaringClass().getAnnotation(MDCParam.class);
            final MDCParams methodMDCParamsAnno = method.getAnnotation(MDCParams.class);
            final MDCParams beanMDCParamsAnno = method.getDeclaringClass().getAnnotation(MDCParams.class);

            final ArrayList<MDCParam> beanMDCParamAnnotations = new ArrayList<>();
            if (beanMDCParamAnno != null)
                beanMDCParamAnnotations.add(beanMDCParamAnno);
            if (beanMDCParamsAnno != null)
                beanMDCParamAnnotations.addAll(Arrays.asList(beanMDCParamsAnno.value()));

            final ArrayList<MDCParam> methodMDCParamAnnotations = new ArrayList<>();
            if (methodMDCParamAnno != null)
                methodMDCParamAnnotations.add(methodMDCParamAnno);
            if (methodMDCParamsAnno != null)
                methodMDCParamAnnotations.addAll(Arrays.asList(methodMDCParamsAnno.value()));

            Annotation[][] argumentsAnnotations = method.getParameterAnnotations();
            ArrayList<String> argumentsNames = new ArrayList<>();
            Map<String, MDCParam> mdcParamMap = new HashMap<>();

            // Please note that for successful arguments' names resolution project must me compuled with
            // javac -parameters or using Spring Boot plugin
            String[] argumentsNamesAsDeclared = argumentsNamesDiscoverer.apply(method);

            for (int i = 0; i < argumentsAnnotations.length; i++) {
                Annotation[] annotations = argumentsAnnotations[i];
                String parameterName = argumentsNamesAsDeclared[i];
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
                argumentsNames.add(parameterName);
                if (mdcParam != null)
                    mdcParamMap.put(parameterName, mdcParam);
            }

            final HashMap<String, Object> expressionStaticVariables = new HashMap<>();
            expressionStaticVariables.put("methodName", method.getName());
            expressionStaticVariables.put("className", method.getDeclaringClass().getName());

            config = new AnnotatedMethodConfig(beanMDCAnno, methodMDCAnno, beanMDCParamAnnotations,
                    methodMDCParamAnnotations, argumentsNames, mdcParamMap, expressionStaticVariables);
            final AnnotatedMethodConfig configUpdated = annotatedMethodConfigCache.putIfAbsent(methodId, config);
            if (configUpdated != null)
                config = configUpdated;
        }
        return config;
    }


    private static class AnnotatedMethodConfig {
        @Nullable
        private final WithMDC beanMDCAnno;
        @Nullable
        private final WithMDC methodMDCAnno;

        private final List<MDCParam> beanMDCParamAnnotations;
        private final List<MDCParam> methodMDCParamAnnotations;
        private final List<String> argumentNames;
        private final Map<String, MDCParam> mdcParamByArgumentName;
        private final Map<String, Integer> argumentIndexByParamName;
        private final Map<String, Object> expressionStaticVariables;

        private AnnotatedMethodConfig(WithMDC beanMDCAnno, WithMDC methodMDCAnno,
                                      List<MDCParam> beanMDCParamAnnotations, List<MDCParam> methodMDCParamAnnotations,
                                      List<String> argumentNames, Map<String, MDCParam> mdcParamByArgumentName,
                                      Map<String, Object> expressionStaticVariables) {
            this.beanMDCAnno = beanMDCAnno;
            this.methodMDCAnno = methodMDCAnno;
            this.beanMDCParamAnnotations = Collections.unmodifiableList(beanMDCParamAnnotations);
            this.methodMDCParamAnnotations = Collections.unmodifiableList(methodMDCParamAnnotations);
            this.argumentNames = Collections.unmodifiableList(argumentNames);
            this.mdcParamByArgumentName = Collections.unmodifiableMap(mdcParamByArgumentName);
            this.expressionStaticVariables = Collections.unmodifiableMap(expressionStaticVariables);
            argumentIndexByParamName = new HashMap<>();
            for (int i = 0; i < argumentNames.size(); i++) {
                argumentIndexByParamName.put(argumentNames.get(i), i);
            }
        }

        public WithMDC getBeanMDCAnno() {
            return beanMDCAnno;
        }

        public WithMDC getMethodMDCAnno() {
            return methodMDCAnno;
        }

        public List<MDCParam> getBeanMDCParamAnnotations() {
            return beanMDCParamAnnotations;
        }

        public List<MDCParam> getMethodMDCParamAnnotations() {
            return methodMDCParamAnnotations;
        }

        public List<String> getArgumentNames() {
            return argumentNames;
        }

        public Map<String, MDCParam> getMdcParamByArgumentName() {
            return mdcParamByArgumentName;
        }

        public int getArgumentParamIndex(String paramName) {
            Integer idx = argumentIndexByParamName.get(paramName);
            if (idx == null)
                throw new IllegalArgumentException("Wrong parameter name: " + paramName);
            return idx;
        }

        public Map<String, Object> getExpressionStaticVariables() {
            return expressionStaticVariables;
        }
    }
}
