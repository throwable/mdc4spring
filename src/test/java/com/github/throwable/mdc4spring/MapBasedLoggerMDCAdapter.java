package com.github.throwable.mdc4spring;

import com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter;

import java.util.HashMap;
import java.util.Map;

public class MapBasedLoggerMDCAdapter implements LoggerMDCAdapter {
    private HashMap<String, String> map = new HashMap<>();

    @Override
    public void put(String key, String value) {
        map.put(key, value);
    }
    @Override
    public void remove(String key) {
        map.remove(key);
    }

    public Map<String, String> getMap() {
        return map;
    }
}
