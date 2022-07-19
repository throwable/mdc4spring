package com.github.throwable.mdc4spring.spring.cmp;

import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.MDCScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@MDCScope(parameters = {
        @MDCParam(name = "environmentProperty", expression = "#environment['sample.property']"),
        @MDCParam(name = "staticParam", expression = "'Static Value'")
})
public class BeanScopedComponent {
    private final static Logger log = LoggerFactory.getLogger(NestedScopedComponent.class);


    public void beanScopedMethod() {
        log.info("Bean scoped method");
    }

    public void beanScopedMethodWithParams(@MDCParam String param1, String sample) {
        log.info("Bean scoped method with params");
    }

    @MDCScope(parameters = {
            @MDCParam(name = "anotherProperty", expression = "'Fixed value'")
    })
    public void beanScopeWithMethodScopeMix(@MDCParam String param1, String sample) {
        log.info("Bean scope with method scope mix");
    }

    @MDCScope(name = "nestedScope", parameters = {
            @MDCParam(name = "anotherProperty", expression = "'Fixed value'")
    })
    public void beanScopeWithNestedMethodScope(@MDCParam String param1, String sample) {
        log.info("Bean scope with method scope on different namespace");
    }
}
