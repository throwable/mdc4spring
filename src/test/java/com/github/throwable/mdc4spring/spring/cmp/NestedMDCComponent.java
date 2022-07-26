package com.github.throwable.mdc4spring.spring.cmp;

import com.github.throwable.mdc4spring.MDC;
import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.WithMDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import static com.github.throwable.mdc4spring.MDC.current;

@Service
public class NestedMDCComponent {
    private final static Logger log = LoggerFactory.getLogger(NestedMDCComponent.class);

    @WithMDC(name = "component2")
    public void execWithNewMDC() {
        current().put("nestedKey", "NestedKeyValue");
        log.info("nested component");
    }

    @WithMDC
    public void execWithSameNameMDC() {
        current().put("nestedKey", "NestedKeyValue");
        log.info("nested component");
    }

    public void execWithCurrentMDC(@MDCParam String param1) {
        MDC.param("param2", "value2");
        log.info("Inside method call");
    }

    @WithMDC
    void samplePackagePrivateMethod(@MDCParam String scope) {
        log.info("Package-private method");
    }

    @WithMDC
    protected void sampleProtectedMethod(@MDCParam String scope) {
        log.info("Protected method");
    }
}
