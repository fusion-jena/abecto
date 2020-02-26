package de.uni_jena.cs.fusion.abecto.execution;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface ExecutionRepository extends CrudRepository<Execution, UUID> {

}
