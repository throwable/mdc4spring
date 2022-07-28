package com.github.throwable.mdc4spring;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Builds new MDC and invokes task inside it
 */
public class MDCInvocationBuilder {
    private final String namespace;
    private Map<String, Object> parameters;

    MDCInvocationBuilder(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Add new parameter to newly created MDC
     * @param name parameter's name
     * @param value parameter's value
     * @throws IllegalArgumentException if parameter name is null
     * @return this builder instance
     */
    public MDCInvocationBuilder param(String name, Object value) {
        if (name == null) throw new IllegalArgumentException("Name must not be null");
        if (parameters == null)
            parameters = new HashMap<>();
        parameters.put(name, value);
        return this;
    }

    /**
     * Creates new MDC and runs a task inside it.
     * @param task task to run
     */
    public void run(Runnable task) {
        try (CloseableMDC mdc = MDC.create(namespace)) {
            if (parameters != null)
                parameters.forEach(mdc::put);
            task.run();
        }
    }

    /**
     * Creates new MDC and runs inside it a task that returns some value.
     * @param task task to run
     * @param <T> return type
     * @return a value returned by task
     */
    public <T> T run(Supplier<T> task) {
        try (CloseableMDC mdc = MDC.create(namespace)) {
            if (parameters != null)
                parameters.forEach(mdc::put);
            return task.get();
        }
    }

    /**
     * Creates new MDC and runs a callable task inside it.
     * @param task task to run
     * @param <T> return type
     * @return a value returned by callable task
     * @throws Exception exception thrown by callable task
     */
    public <T> T call(Callable<T> task) throws Exception {
        try (CloseableMDC mdc = MDC.create(namespace)) {
            if (parameters != null)
                parameters.forEach(mdc::put);
            return task.call();
        }
    }
}
