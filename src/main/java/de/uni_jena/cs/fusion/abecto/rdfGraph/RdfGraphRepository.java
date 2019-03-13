package de.uni_jena.cs.fusion.abecto.rdfGraph;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface RdfGraphRepository extends CrudRepository<RdfGraph, UUID> {
}