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
                .put("property2", "test")) {
            assertThat(MDC.hasCurrent()).isTrue();

            MDC.current().put("property3", "rest");
            MDC.param("property4", "property4value");

            try (CloseableMDC ignored = MDC.create("pfx")
                    .put("property4", "nested")) {
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
        try (CloseableMDC ignored = MDC.create()) {
            MDC.param("some.prefix.param1", "value1");
            try (CloseableMDC ignored1 = MDC.create("some")) {
                MDC.param("prefix.param1", "value2");
                try (CloseableMDC ignored2 = MDC.create("prefix")) {
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
        try (CloseableMDC ignored = MDC.create()) {
            try (CloseableMDC ignored1 = MDC.create()) {
                try (CloseableMDC ignored2 = MDC.create()) {
                    MDC.param("localParam", "localValue");
                    MDC.rootParam("rootParam", "rootParamValue");
                }
            }
            assertThat(mdcAdapter.getMap())
                    .containsEntry("rootParam", "rootParamValue")
                    .doesNotContainKey("localParam");
        }
    }

    @Test
    public void mdcInvocationBuilder() throws Exception {
        MDC.with("component")
                .param("param1", "value1")
                .param("param2", "value2")
                .run(() -> assertThat(mdcAdapter.getMap())
                        .containsEntry("component.param1", "value1")
                        .containsEntry("component.param2", "value2"));

        final String result = MDC.with()
                .param("param1", "value1")
                .apply(() -> {
                    assertThat(mdcAdapter.getMap())
                            .hasSize(1)
                            .containsEntry("param1", "value1");
                    return "Result";
                });
        assertThat(result).isEqualTo("Result");

        final String callResult = MDC.with()
                .param("param1", "value1")
                .call(() -> {
                    assertThat(mdcAdapter.getMap())
                            .hasSize(1)
                            .containsEntry("param1", "value1");
                    return "Result";
                });
        assertThat(callResult).isEqualTo("Result");

        assertThatThrownBy(() -> MDC.with()
                .param("param1", "value1")
                .call(() -> {
                    assertThat(mdcAdapter.getMap())
                            .hasSize(1)
                            .containsEntry("param1", "value1");
                    throw new UnsupportedOperationException();
                })
        ).isInstanceOf(UnsupportedOperationException.class);
    }
}
