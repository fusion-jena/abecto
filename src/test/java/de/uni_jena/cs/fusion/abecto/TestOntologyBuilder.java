package de.uni_jena.cs.fusion.abecto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

import de.uni_jena.cs.fusion.abecto.model.ModelUtils;
import de.uni_jena.cs.fusion.abecto.model.ModelSerializationLanguage;

public class TestOntologyBuilder {

	private ModelSerializationLanguage lang = ModelSerializationLanguage.NTRIPLES;

	private OntModel model;

	List<OntClass> classes;
	List<ObjectProperty> objectProperties;
	List<DatatypeProperty> dataProperties;
	List<Individual> individuals;

	private int ontologyNumber;

	private int classFactor = 1;

	private int objectPropertyFactor = 1;

	private int dataPropertyFactor = 1;

	private int individualFactor = 1;

	private int errorRate = 0;

	private int gapRate = 0;

	private int density = 1;

	public Model build(int ontologyNumber) {
		this.ontologyNumber = ontologyNumber;

		// initialize model and resource lists
		model = ModelUtils.getEmptyOntModel();
		classes = new ArrayList<>();
		objectProperties = new ArrayList<>();
		dataProperties = new ArrayList<>();
		individuals = new ArrayList<>();

		// generate classes
		for (int i = 0; i < classFactor; i++) {
			generateClass(i);
		}

		// generate object properties
		for (int i = 0; i < objectPropertyFactor; i++) {
			generateObjectProperty(i);
		}

		// generate data properties
		for (int i = 0; i < dataPropertyFactor; i++) {
			generateDataProperty(i);
		}

		// generate individuals
		for (int i = 0; i < individualFactor; i++) {
			generateIndividual(i);
		}

		for (int i = 0; i < individualFactor * density; i++) {
			// generate object property statements
			generateObjectPropertyStatement(i);

			// generate data property statements
			generateDataPropertyStatement(i);
		}
		return model;
	}

	public InputStream stream(int ontologyNumber) {
		// write model
		return getModelInputStream(build(ontologyNumber));
	}

	public void write(int ontologyNumber, OutputStream out) {
		build(ontologyNumber).write(out, lang.getApacheJenaKey());
	}

	private boolean isGapCase(int number) {
		return gapRate != 0 && number % gapRate == 0;
	}

	private boolean isErrorCase(int number) {
		final int OVERLAP_PROTECTION_OFFSET = 1;
		return errorRate != 0 && number + OVERLAP_PROTECTION_OFFSET % errorRate == 0;
	}

	private boolean isTextCase(int number) {
		return number % 2 == 0;
	}

	private String generateText(int number) {
		return BigInteger.valueOf(Integer.MAX_VALUE - number).toString(16);
	}

	private void generateClass(int number) {
		String name = "Class" + number;
		OntClass resource = model.createClass(this.getUri(name));
		resource.addLabel(name, "en");
		classes.add(resource);
	}

	private void generateDataPropertyStatement(int number) {
		if (!isGapCase(number)) {
			Statement statement;
			if (!isErrorCase(number)) {
				if (isTextCase(number)) {
					statement = model.createLiteralStatement(getIndividual(number), getDataProperty(number),
							generateText(number));
				} else {
					statement = model.createLiteralStatement(getIndividual(number), getDataProperty(number), number);
				}
			} else {
				if (isTextCase(number)) {
					statement = model.createLiteralStatement(getIndividual(number), getDataProperty(number),
							generateText(number - 1));
				} else {
					statement = model.createLiteralStatement(getIndividual(number), getDataProperty(number),
							number - 1);
				}
			}
			model.add(statement);
		}
	}

	private void generateObjectPropertyStatement(int number) {
		if (!isGapCase(number)) {
			Statement statement;
			if (!isErrorCase(number)) {
				statement = model.createStatement(getIndividual(number), getObjectProperty(number),
						getIndividual(number - 1));
			} else {
				statement = model.createStatement(getIndividual(number), getObjectProperty(number),
						getIndividual(number - 2));
			}
			model.add(statement);
		}
	}

	private void generateDataProperty(int number) {
		String name = "dataProperty" + number;
		DatatypeProperty resource = model.createDatatypeProperty(this.getUri(name));
		resource.addDomain(getClass(number));
		dataProperties.add(resource);
	}

	private void generateIndividual(int number) {
		String name = "individual" + number;
		Individual resource = model.createIndividual(this.getUri(name), getClass(number));
		resource.addLabel(name, "en");
		individuals.add(resource);
	}

	private void generateObjectProperty(int number) {
		String name = "objectProperty" + number;
		ObjectProperty resource = model.createObjectProperty(this.getUri(name));
//		resource.addDomain(getClass(number));
//		resource.addRange(getClass(number - 1));
		objectProperties.add(resource);
	}

	private OntClass getClass(int number) {
		return classes.get(save(number, classFactor));
	}

	private DatatypeProperty getDataProperty(int number) {
		return dataProperties.get(save(number, dataPropertyFactor));
	}

	private Individual getIndividual(int number) {
		return individuals.get(save(number, individualFactor));
	}

	private int save(int number, int max) {
		return (max + number) % max;
	}

	private InputStream getModelInputStream(Model model) {
		// work around PipedInputStream / PipedOutputStream problems
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.write(out, lang.getApacheJenaKey());
		return new ByteArrayInputStream(out.toByteArray());
	}

	private ObjectProperty getObjectProperty(int number) {
		return objectProperties.get(number % objectPropertyFactor);
	}

	private String getUri(String name) {
		return String.format("http://example.org/onto%s/%s", this.ontologyNumber, name);
	}

	public TestOntologyBuilder setClassFactor(int classFactor) {
		this.classFactor = classFactor;
		return this;
	}

	public TestOntologyBuilder setDataPropertyFactor(int dataPropertyFactor) {
		this.dataPropertyFactor = dataPropertyFactor;
		return this;
	}

	public TestOntologyBuilder setDensity(int density) {
		this.density = density;
		return this;
	}

	public TestOntologyBuilder setErrorRate(int errorRate) {
		this.errorRate = errorRate;
		return this;
	}

	public TestOntologyBuilder setGapRate(int gapRate) {
		this.gapRate = gapRate;
		return this;
	}

	public TestOntologyBuilder setIndividualFactor(int individualFactor) {
		this.individualFactor = individualFactor;
		return this;
	}

	public TestOntologyBuilder setLang(ModelSerializationLanguage lang) {
		this.lang = lang;
		return this;
	}

	public TestOntologyBuilder setObjectPropertyFactor(int objectPropertyFactor) {
		this.objectPropertyFactor = objectPropertyFactor;
		return this;
	}
}
