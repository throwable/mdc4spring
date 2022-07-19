package com.github.throwable.mdc4spring.loggers;

public class DummyLoggerMDCAdapter implements LoggerMDCAdapter {
    @Override
    public void put(String key, String value) {
    }

    @Override
    public void remove(String key) {
    }
}
