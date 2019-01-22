package de.uni_jena.cs.fusion.abecto.KnowledgeBaseVersion;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class KnowledgeBaseVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private Long knowledgeBaseId;
	
	protected KnowledgeBaseVersion() {
	}
}
