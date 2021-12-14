/**
 * Enables custom instrumentation by providing SDK to initiate traces, send span entry, info, and exit events, and report errors.
 * <p>
 * There are 2 ways to insert custom instrumentation:
 * <ol>
 * <li>
 * Apply annotation to methods {@link com.appoptics.api.ext.LogMethod}
 * </li>
 * <li>
 * Create and report traces/events explicitly using {@link com.appoptics.api.ext.Trace} and {@link com.appoptics.api.ext.TraceEvent}
 * </li>
 * </ol>
 * <p>
 * The SDK is based around event generation and reporting. Events are created, populated with key/value pairs and reported.
 * <p>
 * The SDK jar file appoptics-sdk.jar can be located in the agent installation directory and copied into your build project.
 * It can also be downloaded from <a href="https://files.appoptics.com/java/latest/">https://files.appoptics.com/java/latest/</a>
 * or be included as a Maven Dependency with group ID <b>com.appoptics.agent.java</b> and artifact ID <b>appoptics-sdk</b>
 * <p>
 * The SDK jar file appoptics-sdk.jar must be used during the development and building phases.
 * Please import or reference classes/interfaces from package {@code com.appoptics.api.ext} only and ensure the SDK jar
 * file/classes is accessible in your runtime classpath/environment.
 * Please take note that the java agent jar file appoptics-agent.jar should only be used via the javaagent JVM argument and
 * not be included elsewhere in your runtime classpath/environment or you may encounter classloader errors.
 * <p>
 * All publicly accessible SDK classes are in this {@code com.appoptics.api.ext} package.
 * Your application code should not import classes outside of this package. Even if they are marked as public, classes and
 * interfaces outside of {@code com.appoptics.api.ext} are subject to change without notice.
 *
 *  @see <a href="http://docs.appoptics.com/kb/apm_tracing/java/sdk/#instrumentation-sdk"> Java Agent - Instrumentation SDK</a>
 */
package com.appoptics.api.ext;

