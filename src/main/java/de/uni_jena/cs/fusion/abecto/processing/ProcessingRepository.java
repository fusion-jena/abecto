package de.uni_jena.cs.fusion.abecto.processing;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.uni_jena.cs.fusion.abecto.node.Node;

public interface ProcessingRepository extends CrudRepository<Processing, UUID> {
	Processing findTopByNodeOrderByStartDateTimeDesc(Node node);

	Iterable<Processing> findByNodeOrderByStartDateTime(Node node);

	Iterable<Processing> findAllByNode(Node node);
}