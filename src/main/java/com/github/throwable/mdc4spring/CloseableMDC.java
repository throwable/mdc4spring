package com.github.throwable.mdc4spring;

import com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter;
import com.github.throwable.mdc4spring.loggers.LoggingSubsystemResolver;

import java.util.ArrayList;
import java.util.HashMap;

public class CloseableMDC implements AutoCloseable, MDC {

    private static final ThreadLocal<CloseableMDC> currentMdc = new ThreadLocal<>();
    private static LoggerMDCAdapter loggerMDCAdapter = LoggingSubsystemResolver.resolveMDCAdapter();

    private final CloseableMDC parent;
    private final String namePrefix;
    private HashMap<String, Object> mdcData;


    private CloseableMDC(CloseableMDC parent, String namePrefix) {
        this.parent = parent;
        this.namePrefix = namePrefix;
        mdcData = new HashMap<>();
    }

    static CloseableMDC current() throws IllegalStateException {
        CloseableMDC mdc = currentMdc.get();
        if (mdc == null)
            throw new IllegalStateException("No MDC was set for current execution scope");
        return mdc;
    }

    static boolean hasCurrent() {
        return currentMdc.get() != null;
    }

    static CloseableMDC root() throws IllegalStateException {
        CloseableMDC mdc = current();
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

    public static CloseableMDC create() {
        return create("");
    }

    public static CloseableMDC create(String namespace) {
        CloseableMDC current = currentMdc.get();
        String keyPrefix = current != null ? current.namePrefix : "";
        String newKeyPrefix;
        if (namespace != null && !"".equals(namespace))
            newKeyPrefix = keyPrefix + namespace + ".";
        else
            newKeyPrefix = keyPrefix;
        CloseableMDC mdc = new CloseableMDC(current, newKeyPrefix);
        currentMdc.set(mdc);
        return mdc;
    }

    @SuppressWarnings("resource")
    @Override
    public void close() {
        if (mdcData == null) throw new IllegalStateException("MDC is closed");
        for (String name : new ArrayList<>(mdcData.keySet())) {
            remove(name);
        }
        if (this.getParent() != null)
            currentMdc.set(this.getParent());
        else
            currentMdc.remove();
        mdcData = null;
    }

    @SuppressWarnings("resource")
    public static void param(String key, Object val) {
        current().put(key, val);
    }

    @Override
    public CloseableMDC getParent() {
        return parent;
    }

    @Override
    public CloseableMDC put(String name, Object value) {
        if (mdcData == null) throw new IllegalStateException("MDC is closed");
        if (name == null) throw new IllegalArgumentException("Name must not be null");
        mdcData.put(name, value);
        loggerMDCAdapter.put(namePrefix + name, value != null ? value.toString() : null);
        return this;
    }

    @Override
    public Object get(String name) {
        if (mdcData == null) throw new IllegalStateException("MDC is closed");
        if (name == null) throw new IllegalArgumentException("Name must not be null");
        return mdcData.get(name);
    }

    @Override
    public CloseableMDC remove(String name) {
        if (mdcData == null) throw new IllegalStateException("MDC is closed");
        if (name == null) throw new IllegalArgumentException("Name must not be null");
        mdcData.remove(name);
        loggerMDCAdapter.remove(namePrefix + name);
        if (getParent() != null)
            getParent().restore(namePrefix + name);
        return this;
    }

    /**
     * Child MDC may overwrite a parent's MDC parameter. So when it removes any of its params we must ensure that
     * the original value will be restored.
     * @param nameWithPrefix full parameter's name with prefix
     */
    private void restore(String nameWithPrefix) {
        if (nameWithPrefix.startsWith(this.namePrefix)) {
            String name = nameWithPrefix.substring(this.namePrefix.length());
            if (mdcData.containsKey(name)) {
                final Object value = mdcData.get(name);
                loggerMDCAdapter.put(nameWithPrefix, value != null ? value.toString() : null);
            } else {
                if (getParent() != null)
                    restore(nameWithPrefix);
            }
        }
    }
}
