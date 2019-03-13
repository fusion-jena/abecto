package de.uni_jena.cs.fusion.abecto.processing.parameter;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface ProcessingParameterRepository extends CrudRepository<ProcessingParameter, UUID> {
}
