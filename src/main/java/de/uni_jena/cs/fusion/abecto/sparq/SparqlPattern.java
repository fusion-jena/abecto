package de.uni_jena.cs.fusion.abecto.sparq;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the properties used by {@link SparqlEntityManager} for a field.
 * 
 * Namespaces indicated with at the class with {@link SparqlNamespace} will be
 * considered.
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface SparqlPattern {
	String value();
}
