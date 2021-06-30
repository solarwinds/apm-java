package com.appoptics.api.ext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
* Profiles execution of a method call using annotation. Will be deprecated soon. Please use {@link LogMethod} instead
*/
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProfileMethod {
    /**
     * Sets the name of the profile reported
     * @return
     */
    String profileName();
    /**
     * Flags whether method stack trace should be included in the event
     * @return
     */
    boolean backTrace() default false;
    /**
     * Flags whether method result will be converted to string and stored in the event
     * @return
     */
    boolean storeReturn() default false;
}
