package de.uni_jena.cs.fusion.abecto.sparq;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the member which the {@link SparqlEntityManager} should assign to a
 * constructor parameter.
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Member {
	String value();
}
