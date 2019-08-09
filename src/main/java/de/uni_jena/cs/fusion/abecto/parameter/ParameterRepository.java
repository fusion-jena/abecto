package de.uni_jena.cs.fusion.abecto.parameter;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface ParameterRepository extends CrudRepository<Parameter, UUID> {
}
