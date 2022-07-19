package com.github.throwable.mdc4spring.anno;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@Documented
public @interface MDCParam {
    @AliasFor("value")
    String name() default "";

    @AliasFor("name")
    String value() default "";

    String expression() default "";

    boolean include() default true;
}
