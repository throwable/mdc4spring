package com.github.throwable.mdc4spring.loggers;

import org.slf4j.MDC;

public class Slf4JLoggerMDCAdapter implements LoggerMDCAdapter {
    @Override
    public void put(String key, String value) {
        MDC.put(key, value);
    }

    @Override
    public void remove(String key) {
        MDC.remove(key);
    }
}
