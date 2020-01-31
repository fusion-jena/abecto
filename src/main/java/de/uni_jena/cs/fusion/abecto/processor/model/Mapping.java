package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

public interface Mapping {

	public static PositiveMapping of(Resource first, Resource second) {
		return new PositiveMapping(first, second, null, null, null, null);
	}

	public static NegativeMapping not(Resource first, Resource second) {
		return new NegativeMapping(first, second, null, null, null, null);
	}

	public Mapping inverse();

	public void setKnowledgeBases(UUID firstKnowledgeBase, UUID secondKnowledgeBase);

	public Mapping setCategories(String categorie);
}
