package com.github.throwable.mdc4spring.spring.spel;

import com.github.throwable.mdc4spring.util.ExpressionEvaluator;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.expression.*;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpelExpressionEvaluator implements ExpressionEvaluator {
    private static final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>();

    // SpEL parser is thead-safe
    private final ExpressionParser expressionParser;
    private final Environment environment;
    private final ApplicationContext applicationContext;

    private final BeanResolver applicationContextBeanResolver = new BeanResolver() {
        @Override
        public Object resolve(EvaluationContext context, String beanName) {
            return applicationContext.getBean(beanName);
        }
    };

    private final PropertyAccessor environmentPropertyAccessor = new PropertyAccessor() {
        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class[] {Environment.class};
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) {
            return true;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) {
            return new TypedValue(environment.getProperty(name));
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) {
        }
    };

    public SpelExpressionEvaluator(Environment environment, ApplicationContext applicationContext) {
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null));
    }

    @Override
    public Object evaluate(String expression, Object rootObject, @Nullable Map<String, Object> argumentValues) {
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

        return parsedExpression.getValue(context);
    }
}
