package de.uni_jena.cs.fusion.abecto.ontology;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.uni_jena.cs.fusion.abecto.project.Project;

public interface OntologyRepository extends CrudRepository<Ontology, UUID> {
	Iterable<Ontology> findAllByProject(Project project);
}