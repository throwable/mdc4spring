package com.github.throwable.mdc4spring.spring.spel;

import com.github.throwable.mdc4spring.util.ExpressionEvaluator;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.expression.*;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpelExpressionEvaluator implements ExpressionEvaluator {
    private static final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>();

    // SpEL parser is thead-safe
    private final ExpressionParser expressionParser;
    private final Environment environment;
    private final ApplicationContext applicationContext;
    private final boolean tolerateNPEs;


    private final BeanResolver applicationContextBeanResolver = new BeanResolver() {
        @Override
        public @NonNull Object resolve(@NonNull EvaluationContext context, @NonNull String beanName) {
            return applicationContext.getBean(beanName);
        }
    };

    private final PropertyAccessor environmentPropertyAccessor = new PropertyAccessor() {
        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class[] {Environment.class};
        }

        @Override
        public boolean canRead(@NonNull EvaluationContext context, Object target, @NonNull String name) {
            return true;
        }

        @NonNull
        @Override
        public TypedValue read(@NonNull EvaluationContext context, Object target, @NonNull String name) {
            return new TypedValue(environment.getProperty(name));
        }

        @Override
        public boolean canWrite(@NonNull EvaluationContext context, Object target, @NonNull String name) {
            return false;
        }

        @Override
        public void write(@NonNull EvaluationContext context, Object target, @NonNull String name, Object newValue) {
        }
    };

    public SpelExpressionEvaluator(Environment environment, ApplicationContext applicationContext) {
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null));
        this.tolerateNPEs = "true".equalsIgnoreCase(System.getProperty(ExpressionEvaluator.TOLERATE_NPE_SYSTEM_PROPERTY, "true"));
    }

    @Override
    public Object evaluate(String expression, Object rootObject,
                           @Nullable Map<String, Object> argumentValues,
                           Map<String, Object> expressionVariables)
    {
        Expression parsedExpression = expressionCache.get(expression);
        if (parsedExpression == null) {
            parsedExpression = expressionParser.parseExpression(expression);
            final Expression expressionUpdated = expressionCache.putIfAbsent(expression, parsedExpression);
            if (expressionUpdated != null)
                parsedExpression = expressionUpdated;
        }

        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);
        context.addPropertyAccessor(environmentPropertyAccessor);
        // Ugly: detect if we are evaluating expression on root=localBean give full access to its private properties
        if (argumentValues != null)
            context.addPropertyAccessor(new PrivateFieldPropertyAccessor(rootObject.getClass()));
        context.setBeanResolver(applicationContextBeanResolver);
        context.setVariable("environment", environment);
        context.setVariable("systemProperties", System.getProperties());

        if (argumentValues != null) {
            //context.setVariable("params", argumentValues);
            argumentValues.forEach(context::setVariable);
        }

        expressionVariables.forEach(context::setVariable);

        try {
            return parsedExpression.getValue(context);
        } catch (SpelEvaluationException e) {
            // EL1012E: Cannot index into a null value
            if (tolerateNPEs && e.getMessage().startsWith("EL1012E:"))
                return null;
            throw e;
        }
    }
}
