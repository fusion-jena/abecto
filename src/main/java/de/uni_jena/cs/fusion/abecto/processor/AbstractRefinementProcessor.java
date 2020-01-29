package de.uni_jena.cs.fusion.abecto.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

public abstract class AbstractRefinementProcessor<P extends ParameterModel> extends AbstractProcessor<P>
		implements RefinementProcessor<P> {

	/**
	 * The {@link Processor}s this {@link Processor} depends on.
	 */
	protected final Collection<Processor<?>> inputProcessors = new ArrayList<>();
	/**
	 * {@link MultiUnion} of the input meta models.
	 * 
	 * @see {@link #metaSubModels} provides the single input meta models.
	 */
	protected final OntModel metaModel = Models.getEmptyOntModel();
	/**
	 * The input meta models.
	 * 
	 * @see {@link #metaModel} provides a union of the input meta models.
	 */
	protected final Set<Model> metaSubModels = new HashSet<>();
	/**
	 * {@link MultiUnion}s of the input group models by knowledge base.
	 * 
	 * @see {@link #inputGroupSubModels} provides the single input group models by
	 *      knowledge base.
	 * @see {@link #inputModelUnion} provides a union over all knowledge bases. .
	 */
	protected final Map<UUID, Model> inputGroupModels = new HashMap<>();
	/**
	 * The input group models by knowledge base.
	 * 
	 * @see {@link #inputGroupModels} provides a union of he input group models by
	 *      knowledge base.
	 * @see {@link #inputModelUnion} provides a union over all knowledge bases. .
	 */
	protected final Map<UUID, Collection<Model>> inputGroupSubModels = new HashMap<>();
	/**
	 * {@link MultiUnion}s of all input group models.
	 * 
	 * @see {@link #inputGroupModels} provides a union of he input group models by
	 *      knowledge base.
	 * @see {@link #inputGroupSubModels} provides the single input group models by
	 *      knowledge base.
	 */
	protected final OntModel inputModelUnion = Models.getEmptyOntModel();

	@Override
	public void addInputModelGroup(UUID uuid, Collection<Model> inputModelGroup) {
		Collection<Model> modelCollection = this.inputGroupSubModels.computeIfAbsent(uuid, (a) -> new HashSet<Model>());
		modelCollection.addAll(inputModelGroup);

		OntModel modelUnion = (OntModel) this.inputGroupModels.computeIfAbsent(uuid,
				(a) -> ModelFactory.createOntologyModel());
		inputModelGroup.forEach(modelUnion::addSubModel);

		inputModelGroup.forEach(this.inputModelUnion::addSubModel);
	}

	@Override
	public void addInputProcessor(Processor<?> processor) {
		this.inputProcessors.add(processor);
	}

	@Override
	public void addMetaModels(Collection<Model> models) {
		models.forEach(this.metaModel::addSubModel);
		this.metaSubModels.addAll(models);
	}

	@Override
	public UUID getKnowledgeBase() {
		return this.inputProcessors.stream().map(Processor::getKnowledgeBase).reduce((a, b) -> {
			if (a.equals(b)) {
				return a;
			} else {
				throw new IllegalStateException(
						"Failed to get knowledge base UUID. Found multiple knowledge base UUID.");
			}
		}).orElseThrow();
	}

	@Override
	protected void prepare() throws InterruptedException, ExecutionException {
		for (Processor<?> inputProcessor : this.inputProcessors) {
			while (!inputProcessor.isSucceeded()) {
				if (inputProcessor.isFailed()) {
					this.fail(inputProcessor.getFailureCause());
				}
				inputProcessor.await();
			}
			this.addMetaModels(inputProcessor.getMetaModel());
			this.addInputModelGroups(inputProcessor.getDataModels());
		}
	}
}
