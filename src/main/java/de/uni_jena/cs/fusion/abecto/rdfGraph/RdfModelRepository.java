package de.uni_jena.cs.fusion.abecto.rdfGraph;

import org.springframework.data.repository.CrudRepository;

public interface RdfModelRepository extends CrudRepository<RdfGraph, Long> {
}