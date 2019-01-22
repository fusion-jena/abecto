package de.uni_jena.cs.fusion.abecto.Project;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface ProjectRepository extends CrudRepository<Project, Long> {
	List<Project> findByName(String name);
}