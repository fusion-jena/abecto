/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;

import de.uni_jena.cs.fusion.abecto.util.Models;

public class TestDataGenerator {

	private Lang lang = Lang.NTRIPLES;

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

	public Model generateOntology(int ontologyNumber) {
		this.ontologyNumber = ontologyNumber;

		// initialize model and resource lists
		model = Models.getEmptyOntModel();
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

	public Map<String, String> generatePatterns(int ontologyNumber) {
		this.ontologyNumber = ontologyNumber;
		Map<String, String> patterns = new HashMap<>();

		for (int classNumber = 0; classNumber < classFactor; classNumber++) {
			StringBuilder pattern = new StringBuilder("{");
			String className = generateClassName(classNumber);

			pattern.append("?" + className + " <" + RDF.type + "> <" + getUri(className) + "> .");

			// TODO correlate classes and properties of individuals

			for (int objectPropertyNumber = 0; objectPropertyNumber < objectPropertyFactor; objectPropertyNumber++) {
				String objectPropertyName = generateObjectPropertyName(objectPropertyNumber);
				pattern.append("OPTIONAL { ?" + className + " <" + getUri(objectPropertyName) + "> ?"
						+ objectPropertyName + " }");
			}

			for (int dataPropertyNumber = 0; dataPropertyNumber < dataPropertyFactor; dataPropertyNumber++) {
				String dataPropertyName = generateDataPropertyName(dataPropertyNumber);
				pattern.append(
						"OPTIONAL { ?" + className + " <" + getUri(dataPropertyName) + "> ?" + dataPropertyName + " }");
			}

			pattern.append("}");
			patterns.put(className, pattern.toString());
		}
		return patterns;
	}

	private void generateClass(int number) {
		String name = generateClassName(number);
		OntClass resource = model.createClass(this.getUri(name));
		resource.addLabel(name, "en");
		classes.add(resource);
	}

	public String generateClassName(int number) {
		return "Class" + number;
	}

	private void generateDataProperty(int number) {
		String name = generateDataPropertyName(number);
		DatatypeProperty resource = model.createDatatypeProperty(this.getUri(name));
		resource.addDomain(getClass(number));
		dataProperties.add(resource);
	}

	public String generateDataPropertyName(int number) {
		return "dataProperty" + number;
	}

	private void generateDataPropertyStatement(int number) {
		// TODO correlate classes and properties of individuals
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

	private void generateIndividual(int number) {
		String name = generateIndividualName(number);
		Individual resource = model.createIndividual(this.getUri(name), getClass(number));
		resource.addLabel(name, "en");
		individuals.add(resource);
	}

	public String generateIndividualName(int number) {
		return "individual" + number;
	}

	private void generateObjectProperty(int number) {
		String name = generateObjectPropertyName(number);
		ObjectProperty resource = model.createObjectProperty(this.getUri(name));
//		resource.addDomain(getClass(number));
//		resource.addRange(getClass(number - 1));
		objectProperties.add(resource);
	}

	public String generateObjectPropertyName(int number) {
		return "objectProperty" + number;
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

	private String generateText(int number) {
		return BigInteger.valueOf(Integer.MAX_VALUE - number).toString(16);
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

	private InputStream getModelInputStream(Model model) throws IOException {
		// work around PipedInputStream / PipedOutputStream problems
		return new ByteArrayInputStream(Models.writeBytes(model, lang));
	}

	private ObjectProperty getObjectProperty(int number) {
		return objectProperties.get(number % objectPropertyFactor);
	}

	private String getUri(String name) {
		return String.format("http://example.org/onto%s/%s", this.ontologyNumber, name);
	}

	private boolean isErrorCase(int number) {
		final int OVERLAP_PROTECTION_OFFSET = 1;
		return errorRate != 0 && number + OVERLAP_PROTECTION_OFFSET % errorRate == 0;
	}

	private boolean isGapCase(int number) {
		return gapRate != 0 && number % gapRate == 0;
	}

	private boolean isTextCase(int number) {
		return number % 2 == 0;
	}

	private int save(int number, int max) {
		return (max + number) % max;
	}

	public TestDataGenerator setClassFactor(int classFactor) {
		this.classFactor = classFactor;
		return this;
	}

	public TestDataGenerator setDataPropertyFactor(int dataPropertyFactor) {
		this.dataPropertyFactor = dataPropertyFactor;
		return this;
	}

	public TestDataGenerator setDensity(int density) {
		this.density = density;
		return this;
	}

	public TestDataGenerator setErrorRate(int errorRate) {
		this.errorRate = errorRate;
		return this;
	}

	public TestDataGenerator setGapRate(int gapRate) {
		this.gapRate = gapRate;
		return this;
	}

	public TestDataGenerator setIndividualFactor(int individualFactor) {
		this.individualFactor = individualFactor;
		return this;
	}

	public TestDataGenerator setLang(Lang lang) {
		this.lang = lang;
		return this;
	}

	public TestDataGenerator setObjectPropertyFactor(int objectPropertyFactor) {
		this.objectPropertyFactor = objectPropertyFactor;
		return this;
	}

	public InputStream stream(int ontologyNumber) throws IOException {
		// write model
		return getModelInputStream(generateOntology(ontologyNumber));
	}

	public void write(int ontologyNumber, OutputStream out) throws IOException {
		Models.write(out, generateOntology(ontologyNumber), this.lang);
	}
}
