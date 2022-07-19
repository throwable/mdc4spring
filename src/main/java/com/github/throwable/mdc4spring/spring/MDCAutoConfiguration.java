package com.github.throwable.mdc4spring.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = MDCScopeAspect.class)
public class MDCAutoConfiguration {
    @Bean
    public TestBean testBean() {
        return new TestBean();
    }
}
