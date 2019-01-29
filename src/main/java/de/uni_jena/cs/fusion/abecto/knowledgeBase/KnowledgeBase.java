package de.uni_jena.cs.fusion.abecto.knowledgebase;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class KnowledgeBase {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private Long projectId;
	
	protected KnowledgeBase() {
	}

}
