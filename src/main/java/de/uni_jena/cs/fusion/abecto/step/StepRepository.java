package de.uni_jena.cs.fusion.abecto.step;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.uni_jena.cs.fusion.abecto.project.Project;

public interface StepRepository extends CrudRepository<Step, UUID> {
	public Iterable<Step> findAllByProject(Project project);
}