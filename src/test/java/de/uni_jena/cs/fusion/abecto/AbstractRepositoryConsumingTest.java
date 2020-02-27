package de.uni_jena.cs.fusion.abecto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.uni_jena.cs.fusion.abecto.execution.ExecutionRepository;
import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.parameter.ParameterRepository;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.step.StepRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
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
