package com.github.throwable.mdc4spring.spring;

import com.github.throwable.mdc4spring.spring.spel.SpelExpressionEvaluator;
import com.github.throwable.mdc4spring.util.ExpressionEvaluator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

/**
 * MDC4Spring autoconfiguration.
 * When using Spring Boot it is added to your ApplicationContext automatically.
 * If you are using Spring Framework you must import it manually.
 */
@Configuration
@ComponentScan
@EnableAspectJAutoProxy
public class MDCAutoConfiguration {
    @Bean
    ExpressionEvaluator spelExpressionEvaluator(Environment environment, ApplicationContext applicationContext) {
        return new SpelExpressionEvaluator(environment, applicationContext);
    }
}
