package com.github.throwable.mdc4spring.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.*;

/**
 * MDC4Spring autoconfiguration for Spring Boot 3.x
 */
@AutoConfiguration
@Import(MDCConfiguration.class)
public class MDCAutoConfiguration {
}
