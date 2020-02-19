package de.uni_jena.cs.fusion.abecto.project;

import javax.persistence.Entity;

import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class Project extends AbstractEntityWithUUID {

	protected String label;

	protected Project() {
	}

	public Project(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
