package de.uni_jena.cs.fusion.abecto.processor.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.util.iterator.ExtendedIterator;

import de.uni_jena.cs.fusion.abecto.processor.AbstractProcessor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public abstract class AbstractMappingProcessor extends AbstractProcessor implements MappingProcessor {

	/**
	 * The {@link Graph} groups to process.
	 */
	private List<Graph> sourceGraphs = new ArrayList<>();
	/**
	 * The {@link Graph} of known mappings and anti-mappings.
	 */
	private Graph knownMappingGraph;

	@Override
	protected RdfGraph computeResultGraph() {
		int groups = this.sourceGraphs.size();
		int progressTotal = groups * (groups - 1) / 2;
		int progress = 0;

		Graph mappingResults = Factory.createGraphMem();

		for (int i = 0; i < groups; i++) {
			for (int j = i + 1; j < groups; j++) {
				// compute mapping
				Graph mappings = computeMapping(sourceGraphs.get(i), sourceGraphs.get(j));

				// add mappings not contradicting to known mappings into mapping results
				ExtendedIterator<Triple> mappingIterator = mappings.find(Node.ANY, MAPPING_PROPERTY, Node.ANY);
				while (mappingIterator.hasNext()) {
					Triple mapping = mappingIterator.next();
					if (!knownMappingGraph.contains(mapping.getMatchSubject(), ANTI_MAPPING_PROPERTY,
							mapping.getMatchObject())
							&& !knownMappingGraph.contains(mapping.getMatchObject(), ANTI_MAPPING_PROPERTY,
									mapping.getMatchSubject())) {
						mappingResults.add(mapping);
					}
				}
				ExtendedIterator<Triple> antiMappingIterator = mappings.find(Node.ANY, ANTI_MAPPING_PROPERTY, Node.ANY);
				while (antiMappingIterator.hasNext()) {
					Triple antiMapping = antiMappingIterator.next();
					if (!knownMappingGraph.contains(antiMapping.getMatchSubject(), MAPPING_PROPERTY,
							antiMapping.getMatchObject())
							&& !knownMappingGraph.contains(antiMapping.getMatchObject(), MAPPING_PROPERTY,
									antiMapping.getMatchSubject())) {
						mappingResults.add(antiMapping);
					}
				}

				// notify progress
				this.listener.onProgress(++progress, progressTotal);
			}
		}

		return new RdfGraph(mappingResults);
	}

	@Override
	public void addSourcesGroup(Collection<RdfGraph> sources) {
		// create new graph union
		MultiUnion graphUnion = new MultiUnion();
		this.sourceGraphs.add(graphUnion);

		// add read only source graphs
		for (RdfGraph source : sources) {
			Graph sourceGraph = source.getGraph();
			graphUnion.addGraph(sourceGraph);
		}
	}

	@Override
	public void setMappings(Collection<RdfGraph> mappings) {
		// create new graph union
		MultiUnion mappingGraphUnion = new MultiUnion();
		this.knownMappingGraph = mappingGraphUnion;

		// add read only source graphs
		for (RdfGraph mapping : mappings) {
			Graph mappingGraph = mapping.getGraph();
			mappingGraphUnion.addGraph(mappingGraph);
		}
	}

	/**
	 * Compute the mapping of two graphs.
	 * 
	 * @param fisrtGraph  first graph to process
	 * @param secondGraph second graph to process
	 * @return computed mapping
	 */
	protected abstract Graph computeMapping(Graph fisrtGraph, Graph secondGraph);

}
