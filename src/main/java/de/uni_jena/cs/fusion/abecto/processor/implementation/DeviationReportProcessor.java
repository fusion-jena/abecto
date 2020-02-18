package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.Collection;

import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractReportProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Deviation;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class DeviationReportProcessor extends AbstractReportProcessor<EmptyParameters> {

	@Override
	protected void computeResultModel() throws Exception {
		// gather issues into one result model
		Collection<Deviation> deviations = SparqlEntityManager
				.select(new Deviation(null, null, null, null, null, null, null, null, null), this.metaModel);
		SparqlEntityManager.insert(deviations, this.getResultModel());
	}

}
