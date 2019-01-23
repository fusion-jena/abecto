package de.uni_jena.cs.fusion.abecto.knowledgeBaseVersionPart;

import java.io.InputStream;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import de.uni_jena.cs.fusion.abecto.knowledgeBasePart.KnowledgeBasePart;

@Entity
public class KnowledgeBasePartVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private Long knowledgeBasePartId;
	private Long knowledgeBaseVersionId;

	protected KnowledgeBasePartVersion() {
	}

	public KnowledgeBasePartVersion(KnowledgeBasePart knowledgeBasePart, InputStream stream, String base, String lang) {
		this.knowledgeBasePartId = knowledgeBasePart.getId();

	}

	public Long getId() {
		return id;
	}

}
