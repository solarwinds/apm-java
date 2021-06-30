package com.appoptics.api.ext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Logs execution of a method call using annotation. To add custom spans with annotation:
 * <p>
 * <ol>
 * <li>
 * Import the {@code com.appoptics.api.ext.LogMethod} annotation
 * </li>
 * <li>
 * Annotate your method by adding {@code @LogMethod(layer="yourLayerName")} above it.
 * </li>
 * </ol>
 * <p>
 * @see <a href="http://docs.appoptics.com/kb/apm_tracing/java/sdk/#custom-spans-using-annotations"> Java Agent - Instrumentation SDK </a>
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMethod {
    /**
     * Sets the layer name for the custom span, this is optional
     * @return
     */
    String layer() default "";
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
    /**
     * Flags whether exceptions thrown by this method would be reported
     * @return
     */
    boolean reportExceptions() default true;
}

