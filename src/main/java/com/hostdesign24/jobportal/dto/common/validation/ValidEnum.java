package com.hostdesign24.jobportal.dto.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValidator.class)
public @interface ValidEnum {

  String message() default "Invalid value. This is not a valid enum value.";

  Class<? extends Enum<?>> enumClass();

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
