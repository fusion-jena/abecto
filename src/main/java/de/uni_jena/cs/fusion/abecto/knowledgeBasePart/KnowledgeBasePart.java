package de.uni_jena.cs.fusion.abecto.konwledgeBasePart;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class KnowledgeBasePart {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private Long knowledgeBaseId;
	
	protected KnowledgeBasePart() {
	}

	public long getId() {
		return this.knowledgeBaseId;
	}
}
