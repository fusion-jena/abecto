package de.uni_jena.cs.fusion.abecto.project;

import org.springframework.data.repository.CrudRepository;

public interface ProjectRepository extends CrudRepository<Project, Long> {
	// List<Project> findByName(String name);
}