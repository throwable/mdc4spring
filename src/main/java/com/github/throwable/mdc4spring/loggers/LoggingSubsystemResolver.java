package com.github.throwable.mdc4spring.loggers;

import java.lang.reflect.InvocationTargetException;

public class LoggingSubsystemResolver {

    /**
     * Resolve logging system using classpath or set up custom implementation specified in LoggerMDCAdapter.MDC_ADAPTER_SYSTEM_PROPERTY
     * system property.
     * @return logging system MDC adapter
     */
    public static LoggerMDCAdapter resolveMDCAdapter() {
        if (System.getProperty(LoggerMDCAdapter.MDC_ADAPTER_SYSTEM_PROPERTY) != null) {
            String adapterClazz = System.getProperty(LoggerMDCAdapter.MDC_ADAPTER_SYSTEM_PROPERTY);
            try {
                Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(adapterClazz);
                return (LoggerMDCAdapter) aClass.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Can not instantiate logger MDC adapter class: " + adapterClazz, e);
            } catch (InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        if (classExistsInClasspath("org.slf4j.Logger")) {
            return new Slf4JLoggerMDCAdapter();
        }
        else if (classExistsInClasspath("org.apache.logging.log4j.Logger")) {
            return new Log4J2LoggerMDCAdapter();
        }
        else if (classExistsInClasspath("org.apache.log4j.Logger")) {
            return new Log4JLoggerMDCAdapter();
        }
        else {
            System.err.println("MDC4Spring: no any logging subsystem was detected in classpath.");
            return new DummyLoggerMDCAdapter();
        }
    }

    private static boolean classExistsInClasspath(String className) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
