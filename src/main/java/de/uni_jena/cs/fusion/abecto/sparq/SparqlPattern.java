package de.uni_jena.cs.fusion.abecto.sparq;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the properties used by {@link SparqlEntityManager} for a field.
 * 
 * Namespaces indicated at the class with {@link SparqlNamespace} will be
 * considered.
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
@Repeatable(SparqlPatterns.class)
public @interface SparqlPattern {
	String subject() default "";

	String predicate();

	String object() default "";
}
