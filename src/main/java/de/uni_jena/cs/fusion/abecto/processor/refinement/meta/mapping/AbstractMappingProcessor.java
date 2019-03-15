package de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Triple;

import de.uni_jena.cs.fusion.abecto.Vocabulary;
import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public abstract class AbstractMappingProcessor extends AbstractMetaProcessor implements MappingProcessor {

	@Override
	protected RdfGraph computeResultGraph() {
		int graphCount = this.inputGraphs.size();
		int progressTotal = graphCount * (graphCount - 1) / 2;
		int progress = 0;

		// collect known mappings
		Collection<Mapping> knownMappings = new HashSet<>();
		Iterator<Triple> knownMappingsIterator = metaGraph.find(Node.ANY, Vocabulary.MAPPING_PROPERTY, Node.ANY);
		Iterator<Triple> knownAntiMappingsIterator = metaGraph.find(Node.ANY, Vocabulary.ANTI_MAPPING_PROPERTY,
				Node.ANY);
		while (knownMappingsIterator.hasNext()) {
			Triple triple = knownMappingsIterator.next();
			knownMappings.add(Mapping.of((Node_URI) triple.getSubject(), (Node_URI) triple.getObject()));
		}
		while (knownAntiMappingsIterator.hasNext()) {
			Triple triple = knownAntiMappingsIterator.next();
			knownMappings.add(Mapping.not((Node_URI) triple.getSubject(), (Node_URI) triple.getObject()));
		}

		// init result graph
		Graph resultsGraph = Factory.createGraphMem();

		// for each pair of source groups
		for (int i = 0; i < graphCount; i++) {
			for (int j = i + 1; j < graphCount; j++) {

				// compute mapping
				Collection<Mapping> mappings = computeMapping(inputGraphs.get(i), inputGraphs.get(j));

				for (Mapping mapping : mappings) {
					// check if mapping is already known or contradicts to previous known mappings
					if (!knownMappings.contains(mapping) && !knownMappings.contains(mapping.inverse())) {

						// add mapping to results
						resultsGraph.add(mapping.getTriple());
						resultsGraph.add(mapping.getReverseTriple());
					}
				}

				// notify progress
				this.listener.onProgress(++progress, progressTotal);
			}
		}

		return new RdfGraph(resultsGraph);
	}

	/**
	 * Compute the mapping of two graphs.
	 * 
	 * @param firstGraph  first graph to process
	 * @param secondGraph second graph to process
	 * @return computed mapping
	 */
	protected abstract Collection<Mapping> computeMapping(Graph firstGraph, Graph secondGraph);

	protected final static class Mapping {
		public final Node_URI first;
		public final Node_URI second;
		public final boolean isAntiMapping;

		private Mapping(Node_URI first, Node_URI second, boolean isAntiMapping) {
			this.first = first;
			this.second = second;
			this.isAntiMapping = isAntiMapping;
		}

		public static Mapping of(Node_URI first, Node_URI second) {
			return new Mapping(first, second, false);
		}

		public static Mapping not(Node_URI first, Node_URI second) {
			return new Mapping(first, second, true);
		}

		public Triple getTriple() {
			return new Triple(first,
					((this.isAntiMapping) ? Vocabulary.ANTI_MAPPING_PROPERTY : Vocabulary.MAPPING_PROPERTY), second);
		}

		public Triple getReverseTriple() {
			return new Triple(second,
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
