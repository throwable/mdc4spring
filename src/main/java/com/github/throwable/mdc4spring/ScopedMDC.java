package com.github.throwable.mdc4spring;

import com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter;
import com.github.throwable.mdc4spring.loggers.LoggingSubsystemResolver;

import java.util.HashMap;

// TODO: copy MDC traces to another context
public class ScopedMDC implements AutoCloseable, MDC {

    private static final ThreadLocal<ScopedMDC> currentMdc = new ThreadLocal<>();
    private static LoggerMDCAdapter loggerMDCAdapter = LoggingSubsystemResolver.resolveMDCAdapter();

    private final ScopedMDC parent;
    private final String keyPrefix;
    private HashMap<String, Object> mdcData;


    private ScopedMDC(ScopedMDC parent, String namespace, String keyPrefix) {
        this.parent = parent;
        this.keyPrefix = keyPrefix;
        mdcData = new HashMap<>();
    }

    static ScopedMDC current() throws IllegalStateException {
        ScopedMDC mdc = currentMdc.get();
        if (mdc == null)
            throw new IllegalStateException("No MDC was set for current execution scope");
        return mdc;
    }

    static boolean hasCurrent() {
        return currentMdc.get() != null;
    }

    static ScopedMDC root() throws IllegalStateException {
        ScopedMDC mdc = current();
        while (mdc.getParent() != null)
            mdc = mdc.getParent();
        return mdc;
    }

    static void setLoggerMDCAdapter(LoggerMDCAdapter mdcAdapter) {
        loggerMDCAdapter = mdcAdapter;
    }

    static LoggerMDCAdapter getLoggerMDCAdapter() {
        return loggerMDCAdapter;
    }

    public ScopedMDC getParent() {
        return parent;
    }

    public static ScopedMDC create() {
        return create("");
    }

    public static ScopedMDC create(String namespace) {
        ScopedMDC current = currentMdc.get();
        String keyPrefix = current != null ? current.keyPrefix : "";
        String newKeyPrefix;
        if (namespace != null && !"".equals(namespace))
            newKeyPrefix = keyPrefix + namespace + ".";
        else
            newKeyPrefix = keyPrefix;
        ScopedMDC mdc = new ScopedMDC(current, namespace, newKeyPrefix);
        currentMdc.set(mdc);
        return mdc;
    }

    @Override
    public void close() {
        if (mdcData == null) throw new IllegalStateException("MDC is closed");
        for (String key : mdcData.keySet()) {
            loggerMDCAdapter.remove(keyPrefix + key);
        }
        if (this.getParent() != null)
            currentMdc.set(this.getParent());
        else
            currentMdc.remove();
        mdcData = null;
    }

    public static void param(String key, Object val) {
        current().put(key, val);
    }

    // TODO: parent-child scope parameter clutching
    public ScopedMDC put(String key, Object val) {
        if (mdcData == null) throw new IllegalStateException("MDC is closed");
        if (key == null) throw new IllegalArgumentException("Key must not be null");
        mdcData.put(key, val);
        loggerMDCAdapter.put(keyPrefix + key, val != null ? val.toString() : null);
        return this;
    }

    public Object get(String key) {
        if (mdcData == null) throw new IllegalStateException("MDC is closed");
        if (key == null) throw new IllegalArgumentException("Key must not be null");
        return mdcData.get(key);
    }

    public ScopedMDC remove(String key) {
        if (mdcData == null) throw new IllegalStateException("MDC is closed");
        if (key == null) throw new IllegalArgumentException("Key must not be null");
        mdcData.remove(key);
        loggerMDCAdapter.remove(keyPrefix + key);
        return this;
    }
}
