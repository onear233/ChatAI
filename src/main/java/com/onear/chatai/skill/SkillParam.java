package com.onear.chatai.skill;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SkillParam {
    String name();
    String description();
    boolean required() default true;
    String type() default "string";
}
