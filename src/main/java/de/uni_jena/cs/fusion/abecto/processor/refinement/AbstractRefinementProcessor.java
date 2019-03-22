package de.uni_jena.cs.fusion.abecto.processor.refinement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;

import de.uni_jena.cs.fusion.abecto.processor.AbstractProcessor;
import de.uni_jena.cs.fusion.abecto.processor.Processor;

public abstract class AbstractRefinementProcessor extends AbstractProcessor implements RefinementProcessor {

	/**
	 * {@link Processor}s this {@link Processor} depends on.
	 */
	protected final Collection<Processor> inputProcessors = new ArrayList<>();

	/**
	 * The previous meta result graph that is a {@link MultiUnion} of all
	 * {@link #metaSubGraphs}.
	 */
	protected final MultiUnion metaGraph = new MultiUnion();
	/**
	 * The previous meta result subgraphs.
	 */
	protected final Set<Graph> metaSubGraphs = new HashSet<>();
	/**
	 * The input graphs that as {@link MultiUnion}s of the according
	 * {@link #inputSubGraphs}.
	 */
	protected final Map<UUID, MultiUnion> inputGroupGraphs = new HashMap<>();
	/**
	 * The input subgraphs.
	 */
	protected final Map<UUID, Collection<Graph>> inputGroupSubGraphs = new HashMap<>();

	@Override
	public void addInputGraphGroup(UUID uuid, Collection<Graph> inputGraphGroup) {
		MultiUnion graphUnion = inputGroupGraphs.computeIfAbsent(uuid, (a) -> new MultiUnion());
		Collection<Graph> graphCollection = inputGroupSubGraphs.computeIfAbsent(uuid, (a) -> new HashSet<Graph>());
		inputGraphGroup.forEach(graphUnion::addGraph);
		graphCollection.addAll(inputGraphGroup);
	}

	@Override
	public void addInputProcessor(Processor processor) {
		this.inputProcessors.add(processor);
	}

	@Override
	public void addMetaGraphs(Collection<Graph> graphs) {
		graphs.forEach(this.metaGraph::addGraph);
		this.metaSubGraphs.addAll(graphs);
	}

	@Override
	protected void prepare() throws InterruptedException {
		for (Processor dependedProcessor : this.inputProcessors) {
			while (!dependedProcessor.isSucceeded()) {
				if (dependedProcessor.isFailed()) {
					this.fail();
				}
				dependedProcessor.await();
			}
			this.addMetaGraphs(dependedProcessor.getMetaGraph());
			this.addInputGraphGroups(dependedProcessor.getDataGraphs());
		}
	}
}
