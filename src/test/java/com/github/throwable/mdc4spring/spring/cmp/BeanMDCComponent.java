package com.github.throwable.mdc4spring.spring.cmp;

import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.WithMDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@MDCParam(name = "environmentProperty", eval = "#environment['sample.property']")
@MDCParam(name = "staticParam", eval = "'Static Value'")
public class BeanMDCComponent {
    private final static Logger log = LoggerFactory.getLogger(NestedMDCComponent.class);


    public void execWithBeanMDCParams() {
        log.info("Bean MDC method");
    }

    public void execWithBeanMDCAndArgumentsAsParams(@MDCParam String param1, String sample) {
        log.info("Bean MDC method with params");
    }

    @MDCParam(name = "anotherProperty", eval = "'Fixed value'")
    public void execWithBeanParamsAndMethodParamsCombined(@MDCParam String param1, String sample) {
        log.info("Bean MDC with method MDC mix");
    }

    @WithMDC(name = "nestedScope")
    @MDCParam(name = "anotherProperty", eval = "'Fixed value'")
    public void execWithBeanMDCAndNestedMethodMDCCombined(@MDCParam String param1, String sample) {
        log.info("Bean MDC with method MDC on different namespace");
    }

    @WithMDC
    public void extractParameterValue(@MDCParam(name = "id", eval = "['id']") @Nullable Map<String, String> map) {
        log.info("Parameter extracted");
    }
}
