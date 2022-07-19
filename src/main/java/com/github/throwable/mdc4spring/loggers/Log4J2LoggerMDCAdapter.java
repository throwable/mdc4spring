package com.github.throwable.mdc4spring.loggers;

import org.apache.logging.log4j.ThreadContext;

public class Log4J2LoggerMDCAdapter implements LoggerMDCAdapter {
    @Override
    public void put(String key, String value) {
        ThreadContext.put(key, value);
    }

    @Override
    public void remove(String key) {
        ThreadContext.remove(key);
    }
}
