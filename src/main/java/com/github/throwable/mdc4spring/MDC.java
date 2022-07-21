package com.github.throwable.mdc4spring;

import com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter;

public interface MDC {
    static MDC current() {
        return CloseableMDC.current();
    }

    static MDC root() {
        return CloseableMDC.root();
    }

    static void setLoggerMDCAdapter(LoggerMDCAdapter loggerMDCAdapter) {
        CloseableMDC.setLoggerMDCAdapter(loggerMDCAdapter);
    }
    static LoggerMDCAdapter getLoggerMDCAdapter() {
        return CloseableMDC.getLoggerMDCAdapter();
    }

    static boolean hasCurrent() {
        return CloseableMDC.hasCurrent();
    }

    MDC getParent();

    static CloseableMDC create() {
        return CloseableMDC.create();
    }

    static CloseableMDC create(String namespace) {
        return CloseableMDC.create(namespace);
    }

    static void param(String key, Object value) throws IllegalArgumentException, IllegalStateException {
        CloseableMDC.param(key, value);
    }

    // TODO: newMDC with lambda

    MDC put(String key, Object val) throws IllegalArgumentException;

    Object get(String key) throws IllegalArgumentException;

    MDC remove(String key) throws IllegalArgumentException;
}
