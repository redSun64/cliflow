package io.github.redsun64.acli.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CliRisk {
    Effect effect() default Effect.READ;
    Confirmation confirmation() default Confirmation.NONE;
}
