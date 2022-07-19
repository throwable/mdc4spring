package com.github.throwable.mdc4spring;

import com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter;

public interface MDC {
    static MDC current() {
        return ScopedMDC.current();
    }

    static MDC root() {
        return ScopedMDC.root();
    }

    static void setLoggerMDCAdapter(LoggerMDCAdapter loggerMDCAdapter) {
        ScopedMDC.setLoggerMDCAdapter(loggerMDCAdapter);
    }
    static LoggerMDCAdapter getLoggerMDCAdapter() {
        return ScopedMDC.getLoggerMDCAdapter();
    }

    static boolean hasCurrent() {
        return ScopedMDC.hasCurrent();
    }

    MDC getParent();

    static ScopedMDC create() {
        return ScopedMDC.create();
    }

    static ScopedMDC create(String namespace) {
        return ScopedMDC.create(namespace);
    }

    static void param(String key, Object value) throws IllegalArgumentException, IllegalStateException {
        ScopedMDC.param(key, value);
    }

    // TODO: newMDC with lambda

    MDC put(String key, Object val) throws IllegalArgumentException;

    Object get(String key) throws IllegalArgumentException;

    MDC remove(String key) throws IllegalArgumentException;
}
