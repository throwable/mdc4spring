package com.github.throwable.mdc4spring.loggers;

import org.slf4j.MDC;

public class Slf4JLoggerMDCAdapter implements LoggerMDCAdapter {

    public Slf4JLoggerMDCAdapter() {
        org.slf4j.LoggerFactory.getLogger(LoggingSubsystemResolver.class)
                .debug("MDC4Spring is configured to use with Slf4J");
    }

    @Override
    public void put(String key, String value) {
        MDC.put(key, value);
    }

    @Override
    public void remove(String key) {
        MDC.remove(key);
    }
}
