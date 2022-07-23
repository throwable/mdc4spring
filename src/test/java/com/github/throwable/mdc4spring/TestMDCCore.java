package com.github.throwable.mdc4spring;

import com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;


public class TestMDCCore {
    static MapBasedLoggerMDCAdapter mdcAdapter = new MapBasedLoggerMDCAdapter();
    static LoggerMDCAdapter originalMDCLoggerAdapter;

    @BeforeAll
    public static void setupMDCAdapter() {
        originalMDCLoggerAdapter = MDC.getLoggerMDCAdapter();
        MDC.setLoggerMDCAdapter(mdcAdapter);
    }
    @AfterAll
    public static void restoreMDCAdapter() {
        MDC.setLoggerMDCAdapter(originalMDCLoggerAdapter);
    }
    @BeforeEach
    public void clearMdc() {
        mdcAdapter.getMap().clear();
    }

    @Test
    public void testManualMDCOperation() {
        assertThat(MDC.hasCurrent()).isFalse();

        try (CloseableMDC mdc = MDC.create()
                .put("property1", 1)
                .put("property2", "test"))
        {
            assertThat(MDC.hasCurrent()).isTrue();

            MDC.current().put("property3", "rest");
            MDC.param("property4", "property4value");

            try (CloseableMDC mdc1 = MDC.create("pfx")
                    .put("property4", "nested"))
            {
                assertThat(mdcAdapter.getMap())
                        .containsEntry("property1", "1")
                        .containsEntry("pfx.property4", "nested");
                assertThat(MDC.root()).isSameAs(mdc);
            }

            mdc.put("property5", "property5value");

            assertThat(mdcAdapter.getMap())
                    .containsEntry("property1", "1")
                    .containsEntry("property2", "test")
                    .containsEntry("property3", "rest")
                    .containsEntry("property4", "property4value")
                    .containsEntry("property5", "property5value")
                    .hasSize(5);

            mdc.remove("property5").remove("property4");
            assertThat(mdcAdapter.getMap())
                    .doesNotContainKey("property5")
                    .doesNotContainKey("property4")
                    .containsKey("property3");
        }

        assertThatThrownBy(() -> MDC.current().put("param", "test"))
                .as("Must be no active MDC")
                .isInstanceOf(IllegalStateException.class);
        assertThat(mdcAdapter.getMap()).isEmpty();
    }

    @Test
    public void parameterOverwrites() {
        try (CloseableMDC mdc1 = MDC.create()) {
            MDC.param("some.prefix.param1", "value1");
            try (CloseableMDC mdc2 = MDC.create("some")) {
                MDC.param("prefix.param1", "value2");
                try (CloseableMDC mdc3 = MDC.create("prefix")) {
                    MDC.param("param1", "value3");
                    assertThat(mdcAdapter.getMap())
                            .containsEntry("some.prefix.param1", "value3");
                }
                assertThat(mdcAdapter.getMap())
                        .containsEntry("some.prefix.param1", "value2");
            }
            assertThat(mdcAdapter.getMap())
                    .containsEntry("some.prefix.param1", "value1");
        }
    }

    @Test
    public void noActiveMDCShouldFail() {
        assertThatThrownBy(() -> MDC.current().put("param", "test")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void nullKeyMustNotBeAccepted() {
        assertThatThrownBy(() -> {
            try (CloseableMDC mdc = MDC.create()) {
                mdc.put(null, 1);
            }
        })
                .as("Must not accept null keys")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void doubleCloseMDCShouldFail() {
        assertThatThrownBy(() -> {
            try (CloseableMDC mdc = MDC.create()) {
                //noinspection RedundantExplicitClose
                mdc.close();
            }
        })
                .as("Must fail when closing already closed MDC")
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void rootParamTest() {
        try (CloseableMDC rootMDC = MDC.create()) {
            try (CloseableMDC mdc2 = MDC.create()) {
                try (CloseableMDC mdc3 = MDC.create()) {
                    MDC.param("localParam", "localValue");
                    MDC.rootParam("rootParam", "rootParamValue");
                }
            }
            assertThat(mdcAdapter.getMap())
                    .containsEntry("rootParam", "rootParamValue")
                    .doesNotContainKey("localParam");
        }
    }
}
