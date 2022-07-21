package com.github.throwable.mdc4spring.spring;

import com.github.throwable.mdc4spring.CloseableMDC;
import com.github.throwable.mdc4spring.MDC;
import com.github.throwable.mdc4spring.util.AnnotatedMethodMDCParamsResolver;
import com.github.throwable.mdc4spring.util.ExpressionEvaluator;
import com.github.throwable.mdc4spring.util.MethodInvocationMDCParametersValues;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Component
public class WithMDCAspect {
    private final AnnotatedMethodMDCParamsResolver annotatedMethodMDCParamsResolver;

    @Autowired
    public WithMDCAspect(ExpressionEvaluator expressionEvaluator) {
        DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        //SpelExpressionEvaluator expressionEvaluator = new SpelExpressionEvaluator();
        this.annotatedMethodMDCParamsResolver = new AnnotatedMethodMDCParamsResolver(
                parameterNameDiscoverer::getParameterNames, expressionEvaluator);
    }

    @Around("@annotation(com.github.throwable.mdc4spring.anno.WithMDC) || " +
            "@within(com.github.throwable.mdc4spring.anno.WithMDC)")
    public Object invokeWithMDC(ProceedingJoinPoint joinPoint) throws Throwable {
        Object unproxiedTarget = joinPoint.getTarget();
        while (AopUtils.isAopProxy(unproxiedTarget) && unproxiedTarget instanceof Advised) {
            unproxiedTarget = ((Advised) unproxiedTarget).getTargetSource().getTarget();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        MethodInvocationMDCParametersValues methodInvocationMdcParamValues =
                annotatedMethodMDCParamsResolver.resolveMethodInvocationMDCParamValues(
                        signature.getMethod(), unproxiedTarget, joinPoint.getArgs());

        if (methodInvocationMdcParamValues.getBeanMDCNamespace() != null &&
                methodInvocationMdcParamValues.getMethodMDCNamespace() != null &&
                !Objects.equals(
                    methodInvocationMdcParamValues.getBeanMDCNamespace(),
                    methodInvocationMdcParamValues.getMethodMDCNamespace()
                )
        ) {
            // Bean and method scope namespaces are different.
            // Create two separate MDCs: one for bean-level and another one for method-level,
            // add corresponding parameters
            try (CloseableMDC beanMdc = MDC.create(methodInvocationMdcParamValues.getBeanMDCNamespace())) {
                methodInvocationMdcParamValues.getBeanMDCParamValues()
                        .forEach(beanMdc::put);
                try (CloseableMDC methodMdc = MDC.create(methodInvocationMdcParamValues.getMethodMDCNamespace())) {
                    methodInvocationMdcParamValues.getMethodMDCParamValues()
                            .forEach(methodMdc::put);

                    return joinPoint.proceed();
                }
            }
        } else {
            // Either bean or method scope was defined or they both are using the same namespace.
            // Create one MDCs for both.
            String namespace = methodInvocationMdcParamValues.getBeanMDCNamespace() != null ?
                    methodInvocationMdcParamValues.getBeanMDCNamespace() :
                    methodInvocationMdcParamValues.getMethodMDCNamespace();
            assert namespace != null;

            try (CloseableMDC mdc = MDC.create(namespace)) {
                methodInvocationMdcParamValues.getBeanMDCParamValues()
                        .forEach(mdc::put);
                methodInvocationMdcParamValues.getMethodMDCParamValues()
                        .forEach(mdc::put);

                return joinPoint.proceed();
            }
        }
    }
}
