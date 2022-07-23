package com.github.throwable.mdc4spring;

import com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter;

/**
 * A basic class to manage MDC programmatically.
 * Initially an execution flow must open a new MDC using MDC.create() method in try-with-resources statement.
 * Inside the MDC scope you can set MDC parameters with their values that will be transmitted to an underlying logging system.
 * After leaving scope all the defined parameters will be cleared automatically.
 * You can also define multiple nested MDC scopes. In this case the logging trace inside nested scope will include all
 * parameters defined in all parent scopes. When leaving nested scope only parameters defined inside it will be cleared.
 * <p>
 * Example:
 * <pre>
 * try (CloseableMDC mdc = MDC.create()) {
 *     mdc.put("param1", "value1");
 *     log.info("Logging trace has param1");
 *     try (CloseableMDC mdcNested = MDC.create() {
 *         mdcNested.put("param2", "value2");
 *         log.info("Included param1 and param2);
 *     }
 *     log.info("param1 is still here while param2 is already out of scope");
 * }
 * </pre>
 *
 */
public interface MDC {
    /**
     * Get a "current" MDC instance: the closest one to current execution scope.
     * If MDC is not defined at current execution scope a method will throw IllegalStateException.
     * @return current MDC instance
     * @throws IllegalStateException if no MDC defined at current execution scope
     */
    static MDC current() {
        return CloseableMDC.current();
    }

    /**
     * Get a root MDC instance: the first MDC opened by current execution flow.
     * @return root MDC instance
     * @throws IllegalStateException if no MDC defined at current execution scope
     */
    static MDC root() {
        return CloseableMDC.root();
    }

    /**
     * Set LoggerMDCAdapter implementation.
     * @param loggerMDCAdapter new logger MDC adapter implementation
     */
    static void setLoggerMDCAdapter(LoggerMDCAdapter loggerMDCAdapter) {
        CloseableMDC.setLoggerMDCAdapter(loggerMDCAdapter);
    }

    /**
     * Get current LoggerMDCAdapter implementation.
     * @return current loggerMDCAdapter implementation.
     */
    static LoggerMDCAdapter getLoggerMDCAdapter() {
        return CloseableMDC.getLoggerMDCAdapter();
    }

    /**
     * Check if MDC was defined at current execution scope.
     * @return true if MDC was defined, false otherwise
     */
    static boolean hasCurrent() {
        return CloseableMDC.hasCurrent();
    }

    /**
     * Define new MDC (root or nested). This method must be used with try-with-resources statement to ensure its correct cleanup.
     * <pre>
     * try (CloseableMDC mdc = MDC.create()) {
     *      ...
     * }
     * </pre>
     * @return closeable MDC resource
     */
    static CloseableMDC create() {
        return CloseableMDC.create();
    }

    /**
     * Define new MDC (root or nested) using namespace prefix. All parameters defined inside this MDC will have
     * prefix specified in namespace.
     * This method must be used with try-with-resources statement to ensure its correct cleanup.
     * <pre>
     * try (CloseableMDC mdc = MDC.create("myComponent")) {
     *      mdc.param("param1", "value1");
     *      log.info("The final name of parameter in logging trace will be 'myComponent.param1'");
     * }
     * </pre>
     * @param namespace namespace prefix
     * @return closeable MDC resource
     */
    static CloseableMDC create(String namespace) {
        return CloseableMDC.create(namespace);
    }

    /**
     * Set parameter value in closest MDC. The method is equivalent to <code>MDC.current().put(name, value)</code>.
     * @param name parameter's name
     * @param value parameter's value
     * @throws IllegalArgumentException if parameter name is null
     * @throws IllegalStateException if no MDC defined at current execution scope.
     */
    static void param(String name, Object value) throws IllegalArgumentException, IllegalStateException {
        CloseableMDC.param(name, value);
    }

    /**
     * Set parameter value in root MDC. The method is equivalent to <code>MDC.root().put(name, value)</code>.
     * @param name parameter's name
     * @param value parameter's value
     * @throws IllegalArgumentException if parameter name is null
     * @throws IllegalStateException if no MDC defined at current execution scope.
     */
    static void rootParam(String name, Object value) throws IllegalArgumentException, IllegalStateException {
        CloseableMDC.rootParam(name, value);
    }

    /**
     * If current MDC is a nested one return its direct parent.
     * @return parent MDC or null if current MDC is a root
     */
    MDC getParent();

    /**
     * Set parameter's value.
     * @param name parameter's name
     * @param value parameter's value
     * @throws IllegalArgumentException if parameter's name is null
     */
    MDC put(String name, Object value) throws IllegalArgumentException;

    /**
     * Get parameter's value.
     * @param name parameter's name
     * @throws IllegalArgumentException if parameter's name is null
     */
    Object get(String name) throws IllegalArgumentException;

    /**
     * Remove parameter from MDC.
     * @param name parameter's name
     * @throws IllegalArgumentException if parameter's name is null
     */
    MDC remove(String name) throws IllegalArgumentException;
}
