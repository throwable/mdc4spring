package com.github.throwable.mdc4spring.anno;

import java.lang.annotation.*;

/**
 * Repeatable wrapper for {@literal @}MDCParamOut
 * @see MDCOutParam
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MDCOutParams {
    MDCOutParam[] value();
}
