package de.uni_jena.cs.fusion.abecto.pattern;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.lang.sparql_11.SPARQLParser11;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.PatternVars;

public class Pattern {

	public static void validate(String category, String pattern) throws ParseException {
		if (!getVariables(parse(pattern)).stream().anyMatch((v) -> v.getVarName().equals(category))) {
			throw new IllegalArgumentException("Template does not contain variable named \"" + category + "\".");
		}
	}

	public static ElementGroup parse(String pattern) throws ParseException {
		SPARQLParser11 parser = new SPARQLParser11(new ByteArrayInputStream(pattern.getBytes()));
		return (ElementGroup) parser.GroupGraphPatternSub();
	}

	public static Collection<Var> getVariables(ElementGroup elementGroup) throws ParseException {
		return PatternVars.vars(elementGroup);
	}
}
