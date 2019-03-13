package de.uni_jena.cs.fusion.abecto.project.knowledgebase;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface KnowledgeBaseRepository extends CrudRepository<KnowledgeBase, UUID> {
}