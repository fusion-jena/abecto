package de.uni_jena.cs.fusion.abecto.processor;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;

@JsonSerialize
public class SparqlConstructProcessorParameter implements ParameterModel {
	public String query;
}
