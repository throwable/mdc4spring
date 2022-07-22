package com.github.throwable.mdc4spring.anno;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Define MDC parameter for method's invocation scope.
 * The annotation may be applied to method's argument or may be used inside {@literal @}WithMDC annotation parameter list.
 * <p>
 * When annotating method's argument a new parameter is created in method's MDC using argument's name and value.
 * You also can specify alternative parameter's name or define an expression that will transform argument's original value.
 * @see WithMDC
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface MDCParam {
    /**
     * Parameter's name. Is optional for method's argument annotation. By default, an argument's name is used.
     */
    @AliasFor("value")
    String name() default "";

    /**
     * Parameter's name. Alias for <code>name</code> attribute.
     */
    @AliasFor("name")
    String value() default "";

    /**
     * Expression to evaluate.
     * When annotating method's argument an argument's value will be used as expression's <code>#root</code> object.
     */
    String eval() default "";
}
