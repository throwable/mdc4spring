package com.github.throwable.mdc4spring.spring.cmp;

import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.WithMDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@WithMDC(parameters = {
        @MDCParam(name = "environmentProperty", expression = "#environment['sample.property']"),
        @MDCParam(name = "staticParam", expression = "'Static Value'")
})
public class BeanMDCComponent {
    private final static Logger log = LoggerFactory.getLogger(NestedMDCComponent.class);


    public void execWithBeanMDC() {
        log.info("Bean MDC method");
    }

    public void execWithBeanMDCAndArgumentsAsParams(@MDCParam String param1, String sample) {
        log.info("Bean MDC method with params");
    }

    @WithMDC(parameters = {
            @MDCParam(name = "anotherProperty", expression = "'Fixed value'")
    })
    public void execWithBeanMDCAndMethodMDCMix(@MDCParam String param1, String sample) {
        log.info("Bean MDC with method MDC mix");
    }

    @WithMDC(name = "nestedScope", parameters = {
            @MDCParam(name = "anotherProperty", expression = "'Fixed value'")
    })
    public void execWithBeanMDCAndNestedMethodMDCMix(@MDCParam String param1, String sample) {
        log.info("Bean MDC with method MDC on different namespace");
    }
}
