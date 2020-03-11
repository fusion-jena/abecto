package de.uni_jena.cs.fusion.abecto.node;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.uni_jena.cs.fusion.abecto.ontology.Ontology;
import de.uni_jena.cs.fusion.abecto.project.Project;

public interface NodeRepository extends CrudRepository<Node, UUID> {
	public Iterable<Node> findAllByProject(Project project);

	public Iterable<Node> findAllByOntology(Ontology ontology);
}