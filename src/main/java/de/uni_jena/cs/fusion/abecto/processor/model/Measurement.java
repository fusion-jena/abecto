package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Optional;
import java.util.UUID;

import org.apache.jena.ext.com.google.common.base.Objects;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto#")
public class Measurement {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Measurement")
	public final Resource id;
	/**
	 * The measured value.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:value")
	public final Long value;
	/**
	 * The knowledge base on which the value was measured.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:knowledgeBase")
	public final UUID knowledgeBase;
	/**
	 * The measurement type.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:measure")
	public final String measure;
	/**
	 * The type of the first dimension.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:dimension1Key")
	public final Optional<String> dimension1Key;
	/**
	 * The value of the first dimension.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:dimension1Value")
	public final Optional<String> dimension1Value;
	/**
	 * The type of the second dimension.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:dimension2Key")
	public final Optional<String> dimension2Key;
	/**
	 * The value of the second dimension.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:dimension2Value")
	public final Optional<String> dimension2Value;

	public Measurement() {
		this.id = null;
		this.value = null;
		this.knowledgeBase = null;
		this.measure = null;
		this.dimension1Key = null;
		this.dimension1Value = null;
		this.dimension2Key = null;
		this.dimension2Value = null;
	}

	public Measurement(@Member("id") Resource id, @Member("knowledgeBase") UUID knowledgeBase,
			@Member("measure") String measure, @Member("value") Long value,
			@Member("dimension1Key") Optional<String> dimension1Key,
			@Member("dimension1Value") Optional<String> dimension1Value,
			@Member("dimension2Key") Optional<String> dimension2Key,
			@Member("dimension2Value") Optional<String> dimension2Value) {
		this.id = id;
		this.value = value;
		this.knowledgeBase = knowledgeBase;
		this.measure = measure;
		this.dimension1Key = dimension1Key;
		this.dimension1Value = dimension1Value;
		this.dimension2Key = dimension2Key;
		this.dimension2Value = dimension2Value;
	}

	@Override
	public int hashCode() {
		return value.hashCode() + knowledgeBase.hashCode() + measure.hashCode() + dimension1Key.hashCode()
				+ dimension1Value.hashCode() + dimension2Key.hashCode() + dimension2Value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Measurement) {
			Measurement other = (Measurement) obj;
			return Objects.equal(this.value, other.value) && Objects.equal(this.knowledgeBase, other.knowledgeBase)
					&& Objects.equal(this.measure, other.measure)
					&& Objects.equal(this.dimension1Key, other.dimension1Key)
					&& Objects.equal(this.dimension1Value, other.dimension1Value)
					&& Objects.equal(this.dimension2Key, other.dimension2Key)
					&& Objects.equal(this.dimension2Value, other.dimension2Value);
		}
		return false;
	}
}
