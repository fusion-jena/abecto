package de.uni_jena.cs.fusion.abecto.processing.configuration;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface ProcessingConfigurationRepository extends CrudRepository<ProcessingConfiguration, UUID> {
}