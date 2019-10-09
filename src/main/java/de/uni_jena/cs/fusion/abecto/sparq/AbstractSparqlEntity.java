package de.uni_jena.cs.fusion.abecto.sparq;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public abstract class AbstractSparqlEntity {
	protected Model model;
	protected Resource id;

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AbstractSparqlEntity
				&& (this.id == null && ((AbstractSparqlEntity) obj).id == null
						|| this.id.equals(((AbstractSparqlEntity) obj).id))
				&& (this.model == null && ((AbstractSparqlEntity) obj).id == model
						|| this.model.equals(((AbstractSparqlEntity) obj).model));
	}

	@Override
	public int hashCode() {
		return (id != null) ? id.hashCode() : super.hashCode();
	}

	public Resource id() {
		return id;
	}

	public void id(Resource id) {
		this.id = id;
	}
}
