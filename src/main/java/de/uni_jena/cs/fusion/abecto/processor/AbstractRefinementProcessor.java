package de.uni_jena.cs.fusion.abecto.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.model.ModelUtils;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

public abstract class AbstractRefinementProcessor<P extends ParameterModel> extends AbstractProcessor<P> implements RefinementProcessor<P> {

	/**
	 * {@link Processor}s this {@link Processor} depends on.
	 */
	protected final Collection<Processor<?>> inputProcessors = new ArrayList<>();

	/**
	 * TODO The previous meta result graph that is a {@link MultiUnion} of all
	 * {@link #metaSubModels}.
	 */
	protected final OntModel metaModel = ModelUtils.getEmptyOntModel();
	/**
	 * TODO The previous meta result submodels.
	 */
	protected final Set<Model> metaSubModels = new HashSet<>();
	/**
	 * TODO The input {@link Model}s that as {@link MultiUnion}s of the according
	 * {@link #inputSubModels}.
	 */
	protected final Map<UUID, Model> inputGroupModels = new HashMap<>();
	/**
	 * TODO The input submodels.
	 */
	protected final Map<UUID, Collection<Model>> inputGroupSubModels = new HashMap<>();
	/**
	 * Union of all input {@link Model}s.
	 */
	protected final OntModel inputModelUnion = ModelUtils.getEmptyOntModel();

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
	protected void prepare() throws InterruptedException {
		for (Processor<?> inputProcessor : this.inputProcessors) {
			while (!inputProcessor.isSucceeded()) {
				if (inputProcessor.isFailed()) {
					this.fail();
				}
				inputProcessor.await();
			}
			this.addMetaModels(inputProcessor.getMetaModel());
			this.addInputModelGroups(inputProcessor.getDataModels());
		}
	}
}
