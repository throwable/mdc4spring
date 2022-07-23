package com.github.throwable.mdc4spring.anno;

import java.lang.annotation.*;

/**
 * Repeatable wrapper for {@literal @}MDCParam
 * @see MDCParam
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MDCParams {
    MDCParam[] value() default {};
}
