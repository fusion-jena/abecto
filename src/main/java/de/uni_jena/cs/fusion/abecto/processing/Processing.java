package de.uni_jena.cs.fusion.abecto.processing;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Processing {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
}
