package com.github.throwable.mdc4spring.spring.cmp;

import com.github.throwable.mdc4spring.anno.MDCScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import static com.github.throwable.mdc4spring.MDC.current;

@Service
public class NestedScopedComponent {
    private final static Logger log = LoggerFactory.getLogger(NestedScopedComponent.class);

    @MDCScope(name = "component2")
    public void newScope() {
        current().put("nestedKey", "NestedKeyValue");
        log.info("nested component");
    }

    @MDCScope
    public void newNonPrefixedScope() {
        current().put("nestedKey", "NestedKeyValue");
        log.info("nested component");
    }
}
