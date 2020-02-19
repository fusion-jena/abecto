package de.uni_jena.cs.fusion.abecto.knowledgebase;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.step.Step;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.util.EntityToIdConverter;

/**
 * Provides a representation of a knowledge base that can be persisted.
 *
 * Knowledge bases consist of sources represented by {@link Step}s.
 */
@Entity
public class KnowledgeBase extends AbstractEntityWithUUID {

	protected String label;

	@ManyToOne(optional = false)
	@JsonSerialize(converter = EntityToIdConverter.class)
	protected Project project;

	@OneToMany(mappedBy = "knowledgeBase")
	protected Collection<Step> sources;

	protected KnowledgeBase() {
	}

	public KnowledgeBase(Project project, String label) {
		this.project = project;
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public Project getProject() {
		return this.project;
	}

	@JsonIgnore
	public Collection<Step> getSources() {
		return this.sources;
	}
}
