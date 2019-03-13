package de.uni_jena.cs.fusion.abecto.project;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface ProjectRepository extends CrudRepository<Project, UUID> {
	// List<Project> findByName(String name);
}