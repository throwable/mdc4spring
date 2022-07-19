package com.github.throwable.mdc4spring.spring.cmp;

import org.springframework.stereotype.Service;

@Service("externalParameterBean")
public class ExternalParameterBean {
    public String getExternalBeanValue() {
        return "Sample external bean value";
    }
}
