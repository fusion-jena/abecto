package de.uni_jena.cs.fusion.abecto.rdfModel;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface RdfModelRepository extends CrudRepository<RdfModel, UUID> {
}