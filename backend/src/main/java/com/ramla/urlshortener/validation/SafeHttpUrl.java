package com.ramla.urlshortener.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The value must be an http(s) URL that points at a public host. See {@link SafeHttpUrlValidator}. */
@Documented
@Constraint(validatedBy = SafeHttpUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeHttpUrl {

    String message() default "must be a valid public http or https URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
