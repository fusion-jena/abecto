package de.uni_jena.cs.fusion.abecto.processor.source;

import java.util.UUID;

import de.uni_jena.cs.fusion.abecto.processor.Processor;

public interface SourceProcessor extends Processor {
	
	/**
	 * @param uuid {@link UUID} of the knowledge base
	 */
	public void setKnowledgBase(UUID uuid);

}
