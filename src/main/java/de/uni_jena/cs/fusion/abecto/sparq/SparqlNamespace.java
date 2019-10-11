package de.uni_jena.cs.fusion.abecto.sparq;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the namespaces used by {@link SparqlEntityManager}.
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(SparqlNamespaces.class)
public @interface SparqlNamespace {
	String prefix();

	String namespace();
}
