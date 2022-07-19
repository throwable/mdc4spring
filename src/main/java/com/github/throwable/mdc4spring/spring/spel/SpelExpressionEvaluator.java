package com.github.throwable.mdc4spring.spring.spel;

import com.github.throwable.mdc4spring.util.ExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.expression.*;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SpelExpressionEvaluator implements ExpressionEvaluator {
    // SpEL parser is thead-safe
    private final ExpressionParser expressionParser;
    private final Environment environment;
    private final ApplicationContext applicationContext;

    private final BeanResolver applicationContextBeanResolver = new BeanResolver() {
        @Override
        public Object resolve(EvaluationContext context, String beanName) throws AccessException {
            return applicationContext.getBean(beanName);
        }
    };

    private final PropertyAccessor environmentPropertyAccessor = new PropertyAccessor() {
        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class[] {Environment.class};
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
            return true;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
            return new TypedValue(environment.getProperty(name));
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        }
    };

    @Autowired
    public SpelExpressionEvaluator(Environment environment, ApplicationContext applicationContext) {
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null));
    }

    @Override
    public Object evaluate(String expression, Object rootObject, @Nullable Map<String, Object> argumentValues) {
        // TODO:
        //  - cache expression
        //  - create read-only context (find how)
        //  + create eagerly-compiled expression

        Expression parsedExpression = expressionParser.parseExpression(expression);
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);

        context.addPropertyAccessor(environmentPropertyAccessor);
        context.setVariable("environment", environment);
        context.setBeanResolver(applicationContextBeanResolver);
        if (argumentValues != null) {
            //context.setVariable("params", argumentValues);
            argumentValues.forEach(context::setVariable);
        }

        return parsedExpression.getValue(context);
    }
}
