package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.Collection;

import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractReportProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Issue;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class IssueReportProcessor extends AbstractReportProcessor<EmptyParameters> {

	@Override
	protected void computeResultModel() throws Exception {
		// gather issues into one result model
		Collection<Issue> issues = SparqlEntityManager.select(new Issue(), this.metaModel);
		SparqlEntityManager.insert(issues, this.getResultModel());
	}

}
