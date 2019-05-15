package de.uni_jena.cs.fusion.abecto.project.knowledgebase;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.uni_jena.cs.fusion.abecto.project.Project;

public interface KnowledgeBaseRepository extends CrudRepository<KnowledgeBase, UUID> {
	Iterable<KnowledgeBase> findAllByProject(Project project);
}