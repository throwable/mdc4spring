package com.github.throwable.mdc4spring.spring.cmp;

import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.MDCScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.github.throwable.mdc4spring.MDC.current;

@Service
public class SampleScopedComponent {
    private final static Logger log = LoggerFactory.getLogger(SampleScopedComponent.class);

    @Autowired
    NestedScopedComponent nestedScopedComponent;

    public String sampleFieldValue = "Sample local field value";

    public String getSampleAccessorValue() {
        return "Sample accessor value";
    }

    public String sampleMethodValue(String argument) {
        return argument.toUpperCase();
    }

    @MDCScope
    public void simpleMDCScope() {
        current().put("sampleKey", "Some Value");
        log.info("Simple MDC Scope trace");
    }

    @MDCScope(name = "component1")
    public void prefixedMDCScope() {
        current().put("sampleKey", "Some Value");
        log.info("Prefixed MDC keys");
    }

    @MDCScope(name = "component1")
    public void nestedScopes() {
        current().put("sampleKey", "Some Value");
        log.info("Before nested scope");
        nestedScopedComponent.newScope();
        log.info("After nested scope");
    }

    @MDCScope(name = "component1")
    public void nonPrefixedNestedScopes() {
        current().put("sampleKey", "Some Value");
        log.info("Before nested scope");
        nestedScopedComponent.newNonPrefixedScope();
        log.info("After nested scope");
    }

    @MDCScope(parameters = {
            @MDCParam(name = "keyParam1", expression = "'Sample string'"),
            @MDCParam(name = "keyParam2", expression = "'Number ' + 5")
    })
    public void mdcWithFixedEvaluatedParameters() {
        log.info("Fixed parameters");
    }

    @MDCScope(parameters = {
            @MDCParam(name = "localFieldParam", expression = "sampleFieldValue"),
            @MDCParam(name = "localAccessorParam", expression = "sampleAccessorValue"),
            @MDCParam(name = "localMethodParam", expression = "'Transformed: ' + sampleMethodValue(sampleFieldValue)"),
            @MDCParam(name = "environmentProperty", expression = "#environment['sample.property']"),
            @MDCParam(name = "externalParameterBeanValue", expression = "@externalParameterBean.externalBeanValue"),
    })
    public void mdcLocalBeanEvaluatedParameters() {
        log.info("Local bean evaluated parameters");
    }


    @MDCScope(parameters = {
            @MDCParam(name = "concatAllArgumentsParam", expression = "#param1 + #param2 + #param3 + #clazz + #notIncluded")
    })
    public void mdcArgumentParams(@MDCParam String param1,
                                  @MDCParam("param2") int myArg,
                                  @MDCParam BigDecimal param3,
                                  @MDCParam(expression = "name") Class<?> clazz,
                                  String notIncluded)
    {
        log.info("Arguments as MDC parameters");
    }
}
