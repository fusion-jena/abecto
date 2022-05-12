package de.uni_jena.cs.fusion.abecto.processor;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;

/**
 * Provides an skeleton for reasoning {@link Processor Processors} that takes
 * care of the execution and inference output storing of a reasoner provided by
 * the method {@link #getReasoner()}.
 */
public abstract class AbstractReasoningProcessor<P extends Processor<P>> extends Processor<P> {

	@Override
	public final void run() {
		Resource dataset = this.getAssociatedDataset().orElseThrow();
		Reasoner reasoner = getReasoner();
		InfModel infModel = ModelFactory.createInfModel(reasoner, this.getInputPrimaryModelUnion(dataset));
		this.getOutputPrimaryModel().orElseThrow().add(infModel.getDeductionsModel());
	}

	/**
	 * Returns the reasoner to use.
	 * 
	 * @return the reasoner to use
	 */
	public abstract Reasoner getReasoner();

}
