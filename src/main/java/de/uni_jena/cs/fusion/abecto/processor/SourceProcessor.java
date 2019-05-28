package de.uni_jena.cs.fusion.abecto.processor;

import java.util.UUID;

public interface SourceProcessor extends Processor {
	
	/**
	 * @param uuid {@link UUID} of the knowledge base
	 */
	public void setKnowledgBase(UUID uuid);

}
