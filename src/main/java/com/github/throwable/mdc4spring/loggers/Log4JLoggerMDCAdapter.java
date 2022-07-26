package com.github.throwable.mdc4spring.loggers;

import org.apache.log4j.MDC;

public class Log4JLoggerMDCAdapter implements LoggerMDCAdapter {

    public Log4JLoggerMDCAdapter() {
        org.apache.log4j.LogManager.getLogger(LoggingSubsystemResolver.class)
                .debug("MDC4Spring is configured to use with Log4J");
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
