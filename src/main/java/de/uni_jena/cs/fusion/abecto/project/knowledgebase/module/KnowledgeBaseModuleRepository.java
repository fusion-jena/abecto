package de.uni_jena.cs.fusion.abecto.project.knowledgebase.module;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface KnowledgeBaseModuleRepository extends CrudRepository<KnowledgeBaseModule, UUID> {
}