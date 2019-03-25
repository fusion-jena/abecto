package de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;

import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.util.Vocabulary;

public abstract class AbstractMappingProcessor extends AbstractMetaProcessor implements MappingProcessor {

	@Override
	protected Model computeResultModel() {
		// collect known mappings
		Collection<Mapping> knownMappings = new HashSet<>();
		Iterator<Statement> knownMappingsIterator = this.metaModel.listStatements(null, Vocabulary.MAPPING_PROPERTY,
				(RDFNode) null);
		Iterator<Statement> knownAntiMappingsIterator = this.metaModel.listStatements(null,
				Vocabulary.ANTI_MAPPING_PROPERTY, (RDFNode) null);

		while (knownMappingsIterator.hasNext()) {
			Statement statement = knownMappingsIterator.next();
			knownMappings.add(Mapping.of(statement.getSubject(), statement.getObject().asResource()));
		}
		while (knownAntiMappingsIterator.hasNext()) {
			Statement statement = knownAntiMappingsIterator.next();
			knownMappings.add(Mapping.not(statement.getSubject(), statement.getObject().asResource()));
		}

		// init result model
		Model resultsModel = ModelFactory.createDefaultModel();

		for (Entry<UUID, Model> i : this.inputGroupModels.entrySet()) {
			for (Entry<UUID, Model> j : this.inputGroupModels.entrySet()) {
				if (i.getKey().compareTo(j.getKey()) > 0) {
					// compute mapping
					Collection<Mapping> mappings = computeMapping(i.getValue(), j.getValue());

					for (Mapping mapping : mappings) {
						// check if mapping is already known or contradicts to previous known mappings
						if (!knownMappings.contains(mapping) && !knownMappings.contains(mapping.inverse())) {

							// add mapping to results
							resultsModel.add(mapping.getStatement());
							resultsModel.add(mapping.getReverseStatement());
						}
					}
				}
			}
		}

		return resultsModel;
	}

	/**
	 * Compute the mapping of two models.
	 * 
	 * @param firstModel  first model to process
	 * @param secondModel second model to process
	 * @return computed mapping
	 */
	protected abstract Collection<Mapping> computeMapping(Model firstModel, Model secondModel);

	protected final static class Mapping {
		public final Resource first;
		public final Resource second;
		public final boolean isAntiMapping;

		private Mapping(Resource first, Resource second, boolean isAntiMapping) {
			this.first = first;
			this.second = second;
			this.isAntiMapping = isAntiMapping;
		}

		public static Mapping of(Resource first, Resource second) {
			return new Mapping(first, second, false);
		}

		public static Mapping not(Resource first, Resource second) {
			return new Mapping(first, second, true);
		}

		public Statement getStatement() {
			return ResourceFactory.createStatement(first,
					((this.isAntiMapping) ? Vocabulary.ANTI_MAPPING_PROPERTY : Vocabulary.MAPPING_PROPERTY), second);
		}

		public Statement getReverseStatement() {
			return ResourceFactory.createStatement(second,
					((this.isAntiMapping) ? Vocabulary.ANTI_MAPPING_PROPERTY : Vocabulary.MAPPING_PROPERTY), first);
		}

		public Mapping inverse() {
			return new Mapping(this.first, this.second, !this.isAntiMapping);
		}

		@Override
		public boolean equals(Object o) {
			Mapping other = (Mapping) o;
			return this.isAntiMapping == other.isAntiMapping
					&& (this.first.equals(other.first) && this.second.equals(other.second)
							|| this.first.equals(other.second) && this.second.equals(other.first));
		}

		@Override
		public int hashCode() {
			return first.getURI().hashCode() + second.getURI().hashCode() + ((this.isAntiMapping) ? 1 : 0);
		}

	}

}
