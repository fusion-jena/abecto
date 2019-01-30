package de.uni_jena.cs.fusion.abecto.processing.parameter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ProcessingParameter {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
}
