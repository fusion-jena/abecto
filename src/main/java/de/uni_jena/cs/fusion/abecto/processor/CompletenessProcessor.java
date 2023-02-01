package de.uni_jena.cs.fusion.abecto.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class CompletenessProcessor extends PopulationComparisonProcessor {
	final static Logger log = LoggerFactory.getLogger(CompletenessProcessor.class);

	@Override
	public void run() {
		log.warn("Deprecated: Use PopulationComparisonProcessor.");
		log.info("PopulationComparisonProcessor started.");
		super.run();
	}

}
