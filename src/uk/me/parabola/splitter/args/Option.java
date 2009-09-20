package uk.me.parabola.splitter.args;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tag interface that marks command line arguments in a java interface.
 *
 * @author Chris Miller
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Option {

	String DEFAULT_DESCRIPTION = "[No description specified]";
	String OPTIONAL = "**OPTIONAL**";

	/**
	 * @return The name of the command line argument
	 */
	String name() default "";

	/**
	 * @return a default value to be used when one isn't specified. If this isn't set
	 * {@code null} will be returned (or zero/false in the case of primitives).
	 */
	String defaultValue() default OPTIONAL;

	/**
	 * @return A description of this parameter to be displayed to the end user when the
	 *         usage is presented
	 */
	String description() default DEFAULT_DESCRIPTION;
}

