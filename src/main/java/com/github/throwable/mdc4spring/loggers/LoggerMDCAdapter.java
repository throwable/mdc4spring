package com.github.throwable.mdc4spring.loggers;

/**
 * Bridge with the underlying logging system MDC implementation
 */
public interface LoggerMDCAdapter {
    String MDC_ADAPTER_SYSTEM_PROPERTY = "com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter";

    void put(String key, String value);
    void remove(String key);
}
