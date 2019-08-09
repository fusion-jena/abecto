package de.uni_jena.cs.fusion.abecto.processing.configuration;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.uni_jena.cs.fusion.abecto.project.Project;

public interface ConfigurationRepository extends CrudRepository<Configuration, UUID> {
	public Iterable<Configuration> findAllByProject(Project project);
}