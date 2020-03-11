package de.uni_jena.cs.fusion.abecto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.uni_jena.cs.fusion.abecto.execution.ExecutionRepository;
import de.uni_jena.cs.fusion.abecto.node.NodeRepository;
import de.uni_jena.cs.fusion.abecto.ontology.OntologyRepository;
import de.uni_jena.cs.fusion.abecto.parameter.ParameterRepository;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractRepositoryConsumingTest {
	@Autowired
	ExecutionRepository executionRepository;
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	OntologyRepository ontologyRepository;
	@Autowired
	NodeRepository nodeRepository;
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	ParameterRepository parameterRepository;

	@AfterEach
	public void cleanup() throws Exception {
		executionRepository.deleteAll();
		processingRepository.deleteAll();
		nodeRepository.deleteAll();
		parameterRepository.deleteAll();
		ontologyRepository.deleteAll();
		projectRepository.deleteAll();
	}
}
