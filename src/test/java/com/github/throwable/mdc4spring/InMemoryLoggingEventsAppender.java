package com.github.throwable.mdc4spring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;


public class InMemoryLoggingEventsAppender extends AppenderBase<Object> {
    @Override
    protected void append(Object event) {
        events.add((ILoggingEvent) event);
    }

    private final static List<ILoggingEvent> events = new ArrayList<>();

    public static List<ILoggingEvent> getLoggingEvents() {
        return new ArrayList<>(events);
    }

    public static void clearLoggingEvents() {
        events.clear();
    }
}
