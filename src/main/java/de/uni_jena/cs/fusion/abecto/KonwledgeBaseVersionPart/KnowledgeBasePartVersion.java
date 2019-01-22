package de.uni_jena.cs.fusion.abecto.KonwledgeBaseVersionPart;

import java.io.InputStream;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.KonwledgeBasePart.KnowledgeBasePart;
import de.uni_jena.cs.fusion.abecto.RdfModel.ModelConverter;

@Entity
public class KnowledgeBasePartVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private Long knowledgeBasePartId;
	private Long knowledgeBaseVersionId;
	@Lob
	@Convert(converter = ModelConverter.class)
	private Model model;

	protected KnowledgeBasePartVersion() {
	}

	public KnowledgeBasePartVersion(KnowledgeBasePart knowledgeBasePart, InputStream stream, String base, String lang) {
		this.knowledgeBasePartId = knowledgeBasePart.getId();
		this.model = ModelFactory.createDefaultModel();
		this.model.read(stream, base, lang);
	}

	public Model getModel() {
		return model;
	}

	public Long getId() {
		return id;
	}

}
