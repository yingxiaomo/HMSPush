package io.github.libxposed.annotation;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({METHOD, FIELD, TYPE})
public @interface SinceApi {
    int value();
}
