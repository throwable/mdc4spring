package com.github.throwable.mdc4spring.spring;

import com.github.throwable.mdc4spring.CloseableMDC;
import com.github.throwable.mdc4spring.MDC;
import com.github.throwable.mdc4spring.util.AnnotatedMethodMDCParamsEvaluator;
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
    private final AnnotatedMethodMDCParamsEvaluator annotatedMethodMDCParamsEvaluator;

    @Autowired
    public WithMDCAspect(ExpressionEvaluator expressionEvaluator) {
        DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        this.annotatedMethodMDCParamsEvaluator = new AnnotatedMethodMDCParamsEvaluator(
                parameterNameDiscoverer::getParameterNames, expressionEvaluator);
    }

    // https://www.faqcode4u.com/faq/214039/aspectj-pointcut-expression-match-parameter-annotations-at-any-position
    @Around("@annotation(com.github.throwable.mdc4spring.anno.WithMDC) || " +
            "@annotation(com.github.throwable.mdc4spring.anno.MDCParam) || " +
            "@annotation(com.github.throwable.mdc4spring.anno.MDCParams) || " +
            "@within(com.github.throwable.mdc4spring.anno.WithMDC) || " +
            "@within(com.github.throwable.mdc4spring.anno.MDCParam) || " +
            "@within(com.github.throwable.mdc4spring.anno.MDCParams) ||" +
            "execution(public * *(.., @com.github.throwable.mdc4spring.anno.MDCParam (*), ..))"
    )
    public Object invokeWithMDC(ProceedingJoinPoint joinPoint) throws Throwable {
        Object unproxiedTarget = joinPoint.getTarget();
        while (AopUtils.isAopProxy(unproxiedTarget) && unproxiedTarget instanceof Advised) {
            unproxiedTarget = ((Advised) unproxiedTarget).getTargetSource().getTarget();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        MethodInvocationMDCParametersValues methodInvocationMdcParamValues =
                annotatedMethodMDCParamsEvaluator.evalMethodInvocationMDCParamValues(
                        signature.getMethod(), unproxiedTarget, joinPoint.getArgs());

        if (methodInvocationMdcParamValues == null)
            // Not supposed to be here: wrong PointCut configuration?
            return joinPoint.proceed();

        if (methodInvocationMdcParamValues.getBeanMDCNamespace() == null && methodInvocationMdcParamValues.getMethodMDCNamespace() == null) {
            if (MDC.hasCurrent()) {
                return invokeInCurrentMDC(joinPoint, methodInvocationMdcParamValues);
            } else {
                return invokeInNewMDC(joinPoint, methodInvocationMdcParamValues);
            }
        }

        if (!Objects.equals(
                methodInvocationMdcParamValues.getBeanMDCNamespace(),
                methodInvocationMdcParamValues.getMethodMDCNamespace()
            ))
        {
            // Bean and method scope namespaces are different.
            // Create two separate MDCs: one for bean-level and another one for method-level,
            // for each one add their corresponding parameters.
            return invokeInSeparateMDCs(joinPoint, methodInvocationMdcParamValues);
        } else {
            // Bean and method scope namespaces are the same.
            // Create a unique MDCs containing both bean and method parameters.
            return invokeInNewMDC(joinPoint, methodInvocationMdcParamValues);
        }
    }

    private Object invokeInNewMDC(ProceedingJoinPoint joinPoint, MethodInvocationMDCParametersValues methodInvocationMdcParamValues) throws Throwable {
        String namespace = methodInvocationMdcParamValues.getBeanMDCNamespace() != null ?
                methodInvocationMdcParamValues.getBeanMDCNamespace() :
                (methodInvocationMdcParamValues.getMethodMDCNamespace() != null ?
                        methodInvocationMdcParamValues.getMethodMDCNamespace() : "");

        try (CloseableMDC mdc = MDC.create(namespace)) {
            methodInvocationMdcParamValues.getBeanMDCParamValues()
                    .forEach(mdc::put);
            methodInvocationMdcParamValues.getMethodMDCParamValues()
                    .forEach(mdc::put);

            return joinPoint.proceed();
        }
    }

    private Object invokeInSeparateMDCs(ProceedingJoinPoint joinPoint, MethodInvocationMDCParametersValues methodInvocationMdcParamValues) throws Throwable {
        try (CloseableMDC beanMdc = MDC.create(methodInvocationMdcParamValues.getBeanMDCNamespace())) {
            methodInvocationMdcParamValues.getBeanMDCParamValues()
                    .forEach(beanMdc::put);
            try (CloseableMDC methodMdc = MDC.create(methodInvocationMdcParamValues.getMethodMDCNamespace())) {
                methodInvocationMdcParamValues.getMethodMDCParamValues()
                        .forEach(methodMdc::put);

                return joinPoint.proceed();
            }
        }
    }

    private Object invokeInCurrentMDC(ProceedingJoinPoint joinPoint, MethodInvocationMDCParametersValues methodInvocationMdcParamValues) throws Throwable {
        // ??? remove parameters after method returns
        methodInvocationMdcParamValues.getBeanMDCParamValues()
                .forEach(MDC::param);
        methodInvocationMdcParamValues.getMethodMDCParamValues()
                .forEach(MDC::param);
        return joinPoint.proceed();
    }
}
