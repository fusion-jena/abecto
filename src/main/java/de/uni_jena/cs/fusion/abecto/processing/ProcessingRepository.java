package de.uni_jena.cs.fusion.abecto.processing;

import org.springframework.data.repository.CrudRepository;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;

public interface ProcessingRepository extends CrudRepository<Processing, Long> {
	Processing findTopByConfigurationOrderByStartDateTimeDesc(ProcessingConfiguration configuration);
}