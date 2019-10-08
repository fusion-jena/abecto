package de.uni_jena.cs.fusion.abecto.sparq;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(Namespaces.class)
public @interface Namespace {
	String prefix();
	String namespace();
}
