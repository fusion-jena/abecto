package de.uni_jena.cs.fusion.abecto;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import de.uni_jena.cs.fusion.abecto.execution.ExecutionRepository;
import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.parameter.ParameterRepository;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.step.StepRepository;

public abstract class AbstractRepositoryConsumingTest {
	@Autowired
	ExecutionRepository executionRepository;
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	StepRepository stepRepository;
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	ParameterRepository parameterRepository;

	@AfterEach
	public void cleanup() throws Exception {
		executionRepository.deleteAll();
		processingRepository.deleteAll();
		stepRepository.deleteAll();
		parameterRepository.deleteAll();
		knowledgeBaseRepository.deleteAll();
		projectRepository.deleteAll();
	}
}
