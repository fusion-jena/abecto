package de.uni_jena.cs.fusion.abecto.processing.provenance;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ProcessingProvenance {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
}
