package com.github.throwable.mdc4spring.loggers;

public class LoggingSubsystemResolver {

    public static LoggerMDCAdapter resolveMDCAdapter() {
        if (System.getProperty(LoggerMDCAdapter.MDC_ADAPTER_SYSTEM_PROPERTY) != null) {
            String adapterClazz = System.getProperty(LoggerMDCAdapter.MDC_ADAPTER_SYSTEM_PROPERTY);
            try {
                Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(adapterClazz);
                return (LoggerMDCAdapter) aClass.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Can not instantiate logger MDC adapter class: " + adapterClazz, e);
            }
        }

        if (classExistsInClasspath("org.slf4j.Logger")) {
            debugSlf4J("MDC4Spring is configured to use with Slf4J");
            return new Slf4JLoggerMDCAdapter();
        }
        else if (classExistsInClasspath("org.apache.logging.log4j.Logger")) {
            log4J2Greeting();
            return new Log4J2LoggerMDCAdapter();
        }
        else if (classExistsInClasspath("org.apache.log4j.Logger")) {
            log4JGreeting();
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

    private static void log4JGreeting() {
        org.apache.log4j.LogManager.getLogger(LoggingSubsystemResolver.class).debug("MDC4Spring is configured to use with Log4J");
    }

    private static void log4J2Greeting() {
        org.apache.logging.log4j.LogManager.getLogger(LoggingSubsystemResolver.class).debug("MDC4Spring is configured to use with Log4J2");
    }

    private static void debugSlf4J(String message) {
        org.slf4j.LoggerFactory.getLogger(LoggingSubsystemResolver.class).debug(message);
    }
}
