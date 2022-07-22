package com.github.throwable.mdc4spring.spring.cmp;

import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.WithMDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.github.throwable.mdc4spring.MDC.current;

@Service
public class SampleMDCComponent {
    private final static Logger log = LoggerFactory.getLogger(SampleMDCComponent.class);

    @Autowired
    NestedMDCComponent nestedMDCComponent;

    private final String sampleFieldValue = "Sample local field value";

    private String getSampleAccessorValue() {
        return "Sample accessor value";
    }

    public String sampleMethodValue(String argument) {
        return argument.toUpperCase();
    }

    @WithMDC
    public void execWithSimpleMDC() {
        current().put("sampleKey", "Some Value");
        log.info("Simple MDC Scope trace");
    }

    @WithMDC(name = "component1")
    public void execWithNamedMDC() {
        current().put("sampleKey", "Some Value");
        log.info("Prefixed MDC keys");
    }

    @WithMDC(name = "component1")
    public void execWithNestedMDCs() {
        current().put("sampleKey", "Some Value");
        log.info("Before nested MDC");
        nestedMDCComponent.execWithNewMDC();
        log.info("After nested MDC");
    }

    @WithMDC(name = "component1")
    public void execWithNestedSameNameMDC() {
        current().put("sampleKey", "Some Value");
        log.info("Before nested MDC");
        nestedMDCComponent.execWithSameNameMDC();
        log.info("After nested MDC");
    }

    @WithMDC(parameters = {
            @MDCParam(name = "keyParam1", eval = "'Sample string'"),
            @MDCParam(name = "keyParam2", eval = "'Number ' + 5")
    })
    public void execWithMDCWithFixedParameters() {
        log.info("Fixed parameters");
    }

    @WithMDC(parameters = {
            @MDCParam(name = "localFieldParam", eval = "sampleFieldValue"),
            @MDCParam(name = "localAccessorParam", eval = "sampleAccessorValue"),
            @MDCParam(name = "localMethodParam", eval = "'Transformed: ' + sampleMethodValue(sampleFieldValue)"),
            @MDCParam(name = "environmentProperty", eval = "#environment['sample.property']"),
            @MDCParam(name = "systemProperty", eval = "#systemProperties['user.home']"),
            @MDCParam(name = "externalParameterBeanValue", eval = "@externalParameterBean.externalBeanValue"),
    })
    public void execWithMDCWithReferencedParameters() {
        log.info("Parameters referencing local bean and environment");
    }


    @WithMDC(parameters = {
            @MDCParam(name = "concatAllArgumentsParam", eval = "#param1 + #param2 + #param3 + #clazz + #notIncluded")
    })
    public void execWithMDCWithArgumentsAsParameters(@MDCParam String param1,
                                                     @MDCParam("param2") int myArg,
                                                     @MDCParam BigDecimal param3,
                                                     @MDCParam(eval = "name") Class<?> clazz,
                                                     String notIncluded)
    {
        log.info("Arguments as MDC parameters");
    }
}