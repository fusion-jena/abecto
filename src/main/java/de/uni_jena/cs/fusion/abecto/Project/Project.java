package de.uni_jena.cs.fusion.abecto.Project;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;

	protected Project() {
	}

	public Project(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return String.format("Project[id=%d, name='%s']", id, name);
	}
}
