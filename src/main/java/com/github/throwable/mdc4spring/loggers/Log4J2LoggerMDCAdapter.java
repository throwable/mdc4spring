package com.github.throwable.mdc4spring.loggers;

import org.apache.logging.log4j.ThreadContext;

public class Log4J2LoggerMDCAdapter implements LoggerMDCAdapter {

    public Log4J2LoggerMDCAdapter() {
        org.apache.logging.log4j.LogManager.getLogger(LoggingSubsystemResolver.class)
                .debug("MDC4Spring is configured to use with Log4J2");
    }

    @Override
    public void put(String key, String value) {
        ThreadContext.put(key, value);
    }

    @Override
    public void remove(String key) {
        ThreadContext.remove(key);
    }
}
