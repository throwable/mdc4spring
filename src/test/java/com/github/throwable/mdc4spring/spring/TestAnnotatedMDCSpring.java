package com.github.throwable.mdc4spring.spring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.throwable.mdc4spring.InMemoryLoggingEventsAppender;
import com.github.throwable.mdc4spring.MDC;
import com.github.throwable.mdc4spring.spring.cmp.BeanMDCComponent;
import com.github.throwable.mdc4spring.spring.cmp.SampleMDCComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = "sample.property=Environment property value"
)
public class TestAnnotatedMDCSpring {
    @Autowired
    SampleMDCComponent sampleMDCComponent;
    @Autowired
    BeanMDCComponent beanMDCComponent;

    @BeforeEach
    public void clearMdc() {
        InMemoryLoggingEventsAppender.clearLoggingEvents();
    }

    @Test
    public void simpleMDC() {
        sampleMDCComponent.execWithSimpleMDC();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(1)
                .containsEntry("sampleKey", "Some Value");
    }

    @Test
    public void namedMDC() {
        sampleMDCComponent.execWithNamedMDC();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(1)
                .containsEntry("component1.sampleKey", "Some Value");
    }

    @Test
    public void nestedMDCs() {
        sampleMDCComponent.execWithNestedMDCs();

        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(3);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(1)
                .containsEntry("component1.sampleKey", "Some Value");
        assertThat(traces.get(1).getMDCPropertyMap())
                .as("Nested MDC must contain keys from all parent scopes")
                .hasSize(2)
                .containsEntry("component1.sampleKey", "Some Value")
                .containsEntry("component1.component2.nestedKey", "NestedKeyValue");
        assertThat(traces.get(2).getMDCPropertyMap())
                .as("By exiting nested MDC all inner keys must be removed")
                .hasSize(1)
                .containsEntry("component1.sampleKey", "Some Value");
    }

    @Test
    public void nestedMDCSameName() {
        sampleMDCComponent.execWithNestedSameNameMDC();

        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(3);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(1)
                .containsEntry("component1.sampleKey", "Some Value");
        assertThat(traces.get(1).getMDCPropertyMap())
                .as("Nested MDC must contain keys from all parent scopes")
                .hasSize(2)
                .containsEntry("component1.sampleKey", "Some Value")
                .containsEntry("component1.nestedKey", "NestedKeyValue");
        assertThat(traces.get(2).getMDCPropertyMap())
                .as("By exiting nested all inner keys must be removed")
                .hasSize(1)
                .containsEntry("component1.sampleKey", "Some Value");
    }

    @Test
    public void nestedBeanWithInvocationInCurrentMDC() {
        sampleMDCComponent.execInvocationInCurrentMDC();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(2);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(2)
                .containsEntry("param1", "value1")
                .containsEntry("param2", "value2");
        assertThat(traces.get(1).getMDCPropertyMap())
                .as("Current MDC parameters should not be cleared after the method call")
                .hasSize(2)
                .containsEntry("param1", "value1")
                .containsEntry("param2", "value2");
    }

    @Test
    public void paramMethodCallWithoutMDCDefined() {
        sampleMDCComponent.execParamMethodWithoutMDC();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .as("MDC must implicitly be created")
                .hasSize(1)
                .containsEntry("keyParam1", "Sample string");
        assertThat(MDC.hasCurrent()).isFalse();
    }

    @Test
    public void mdcWithMethodOnlyMDCParameter() {
        sampleMDCComponent.execWithMethodOnlyMDCParameter();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(1)
                .containsEntry("keyParam1", "Sample string");
    }

    @Test
    public void mdcWithFixedParameters() {
        sampleMDCComponent.execWithFixedMDCParameters();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(2)
                .containsEntry("keyParam1", "Sample string")
                .containsEntry("keyParam2", "Number 5");
    }

    @Test
    public void mdcWithParametersReferencingContext() {
        sampleMDCComponent.execWithMDCParametersReferencingContext();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(6)
                .containsEntry("localFieldParam", "Sample local field value")
                .containsEntry("localAccessorParam", "Sample accessor value")
                .containsEntry("localMethodParam", "Transformed: SAMPLE LOCAL FIELD VALUE")
                .containsEntry("environmentProperty", "Environment property value")
                .containsKey("systemProperty")
                .containsEntry("externalParameterBeanValue", "Sample external bean value");
    }


    @Test
    public void mdcMethodArgumentAsAParameter() {
        sampleMDCComponent.execWithMethodArgumentAsMDCParameter(
                "Param1 value", "Param2 value");
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(1)
                .containsEntry("param1", "Param1 value")
                .doesNotContainKey("param2");
    }

    @Test
    public void mdcMethodArgumentsAsParameters() {
        sampleMDCComponent.execWithMethodArgumentsAsMDCParameters(
                "Param1 value", 42, new BigDecimal(65536), BigDecimal.class, "ParamNotIncluded");
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(5)
                .containsEntry("param1", "Param1 value")
                .containsEntry("param2", "42")
                .containsEntry("param3", "65536")
                .containsEntry("clazz", "java.math.BigDecimal")
                .containsEntry("concatAllArgumentsParam", "Param1 value4265536java.math.BigDecimalParamNotIncluded")
                .doesNotContainKey("notIncluded");
    }


    @Test
    public void beanMDCParamsMethodCall() {
        beanMDCComponent.execWithBeanMDCParams();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(2)
                .containsEntry("environmentProperty", "Environment property value")
                .containsEntry("staticParam", "Static Value");
    }


    @Test
    public void beanMDCWithArgumentsAsParams() {
        beanMDCComponent.execWithBeanMDCAndArgumentsAsParams("Value 1", "Value 2");

        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(3)
                .containsEntry("environmentProperty", "Environment property value")
                .containsEntry("staticParam", "Static Value")
                .containsEntry("param1", "Value 1");
    }

    @Test
    public void beanParamsAndMethodParamsCombined() {
        beanMDCComponent.execWithBeanParamsAndMethodParamsCombined("Value 1", "Value 2");
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(4)
                .containsEntry("environmentProperty", "Environment property value")
                .containsEntry("staticParam", "Static Value")
                .containsEntry("param1", "Value 1")
                .containsEntry("anotherProperty", "Fixed value");
    }

    @Test
    public void beanMDCAndNestedMethodMDCCombined() {
        beanMDCComponent.execWithBeanMDCAndNestedMethodMDCCombined("Value 1", "Value 2");
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(4)
                .containsEntry("environmentProperty", "Environment property value")
                .containsEntry("staticParam", "Static Value")
                .containsEntry("nestedScope.param1", "Value 1")
                .containsEntry("nestedScope.anotherProperty", "Fixed value");
    }

    @Test
    public void callsToLocalNonPublicMethods() {
        sampleMDCComponent.execLocalNonPublicMethods();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(3);
        assertThat(traces.get(0).getMDCPropertyMap())
                .as("Local calls are not instrumented by Spring AOP")
                .isEmpty();
        assertThat(traces.get(1).getMDCPropertyMap())
                .as("Local calls are not instrumented by Spring AOP")
                .isEmpty();
        assertThat(traces.get(2).getMDCPropertyMap())
                .as("Local calls are not instrumented by Spring AOP")
                .isEmpty();
    }

    @Test
    public void callsToRemoteNonPublicMethods() {
        sampleMDCComponent.execRemoteNonPublicMethods();
        List<ILoggingEvent> traces = InMemoryLoggingEventsAppender.getLoggingEvents();
        assertThat(traces).hasSize(3);
        assertThat(traces.get(0).getMDCPropertyMap())
                .hasSize(1)
                .containsEntry("scope", "package-private");
        assertThat(traces.get(1).getMDCPropertyMap())
                .hasSize(1)
                .containsEntry("scope", "protected");
        assertThat(traces.get(2).getMDCPropertyMap())
                .as("Spring AOP does not instrument private methods")
                .isEmpty();
    }
}
