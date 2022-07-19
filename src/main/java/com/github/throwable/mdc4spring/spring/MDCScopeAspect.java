package com.github.throwable.mdc4spring.spring;

import com.github.throwable.mdc4spring.ScopedMDC;
import com.github.throwable.mdc4spring.MDC;
import com.github.throwable.mdc4spring.spring.spel.SpelExpressionEvaluator;
import com.github.throwable.mdc4spring.util.AnnotatedMethodMDCParamsResolver;
import com.github.throwable.mdc4spring.util.MethodInvocationMDCScopeParametersValues;
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

/**
 * TODO:
 *  [x] MDCScope: namespace
 *  [x] MDCParam: include method arguments
 *  [x] MDCParam: include auto-discovered method arguments
 *  [x] MDCScope: includes method
 *  [-] MDCScope: include params by default
 *  [-] Add properties by default on MDC creation (correlationId, hostName, etc...)
 *      - configurable (EL-based?)
 *  [ ] MDCScope: join=true: do not open new scope and join a previous scope (in case if any).
 *      Evaluate MDC parameters but apply them to previous scope (do not remove them)
 *  [ ] MDCScope on bean: opens scope, evaluates parameters
 *      If any bean's method also has an MDCScope, simply create a new method's scope. The existing bean's scope stays as it's parent.
 */
@Aspect
@Component
public class MDCScopeAspect {
    private final AnnotatedMethodMDCParamsResolver annotatedMethodMDCParamsResolver;

    @Autowired
    public MDCScopeAspect(SpelExpressionEvaluator expressionEvaluator) {
        DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        //SpelExpressionEvaluator expressionEvaluator = new SpelExpressionEvaluator();
        this.annotatedMethodMDCParamsResolver = new AnnotatedMethodMDCParamsResolver(
                parameterNameDiscoverer::getParameterNames, expressionEvaluator);
    }

    @Around("@annotation(com.github.throwable.mdc4spring.anno.MDCScope) || @within(com.github.throwable.mdc4spring.anno.MDCScope)")
    public Object createMDC(ProceedingJoinPoint joinPoint) throws Throwable {
        Object unproxiedTarget = joinPoint.getTarget();
        while (AopUtils.isAopProxy(unproxiedTarget) && unproxiedTarget instanceof Advised) {
            unproxiedTarget = ((Advised) unproxiedTarget).getTargetSource().getTarget();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        MethodInvocationMDCScopeParametersValues methodInvocationMdcScopeParametersValues =
                annotatedMethodMDCParamsResolver.resolveMethodInvocationScopeValues(
                        signature.getMethod(), unproxiedTarget, joinPoint.getArgs());

        if (methodInvocationMdcScopeParametersValues.getBeanScopeNamespace() != null &&
                methodInvocationMdcScopeParametersValues.getMethodScopeNamespace() != null &&
                !Objects.equals(
                    methodInvocationMdcScopeParametersValues.getBeanScopeNamespace(),
                    methodInvocationMdcScopeParametersValues.getMethodScopeNamespace()
                )
        ) {
            // Bean and method scope namespaces are different.
            // Create two separate MDCScopes: one for bean-level and another one for method-level,
            // add corresponding parameters
            try (ScopedMDC beanMdc = MDC.create(methodInvocationMdcScopeParametersValues.getBeanScopeNamespace())) {
                methodInvocationMdcScopeParametersValues.getBeanScopeMDCParamValues()
                        .forEach(beanMdc::put);
                try (ScopedMDC methodMdc = MDC.create(methodInvocationMdcScopeParametersValues.getMethodScopeNamespace())) {
                    methodInvocationMdcScopeParametersValues.getMethodScopeMDCParamValues()
                            .forEach(methodMdc::put);

                    return joinPoint.proceed();
                }
            }
        } else {
            // Either bean or method scope was defined or they both are using the same namespace.
            // Create one MDCScope for both.
            String namespace = methodInvocationMdcScopeParametersValues.getBeanScopeNamespace() != null ?
                    methodInvocationMdcScopeParametersValues.getBeanScopeNamespace() :
                    methodInvocationMdcScopeParametersValues.getMethodScopeNamespace();
            assert namespace != null;

            try (ScopedMDC mdc = MDC.create(namespace)) {
                methodInvocationMdcScopeParametersValues.getBeanScopeMDCParamValues()
                        .forEach(mdc::put);
                methodInvocationMdcScopeParametersValues.getMethodScopeMDCParamValues()
                        .forEach(mdc::put);

                return joinPoint.proceed();
            }
        }
    }
}
