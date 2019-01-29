package de.uni_jena.cs.fusion.abecto.knowledgebase.part.version;

import java.io.InputStream;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import de.uni_jena.cs.fusion.abecto.knowledgebase.part.KnowledgeBasePart;

@Entity
public class KnowledgeBasePartVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private Long knowledgeBasePartId;

	protected KnowledgeBasePartVersion() {
	}

	public KnowledgeBasePartVersion(KnowledgeBasePart knowledgeBasePart, InputStream stream, String base, String lang) {
		this.knowledgeBasePartId = knowledgeBasePart.getId();

	}

	public Long getId() {
		return id;
	}

}
