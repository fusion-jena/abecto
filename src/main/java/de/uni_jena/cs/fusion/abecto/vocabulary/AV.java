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
package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Provides the ABECTO vocabulary.
 *
 */
public class AV {
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://w3id.org/abecto-vocabulary#";

	public static final Resource absoluteCoverage = ResourceFactory.createResource(namespace + "absoluteCoverage");
	public static final Property affectedAspect = ResourceFactory.createProperty(namespace, "affectedAspect");
	public static final Property affectedValue = ResourceFactory.createProperty(namespace, "affectedValue");
	public static final Property affectedVariableName = ResourceFactory.createProperty(namespace,
			"affectedVariableName");
	public static final Resource Aspect = ResourceFactory.createResource(namespace + "Aspect");
	public static final Resource AspectPattern = ResourceFactory.createResource(namespace + "AspectPattern");
	public static final Property associatedDataset = ResourceFactory.createProperty(namespace, "associatedDataset");
	public static final Property comparedToDataset = ResourceFactory.createProperty(namespace, "comparedToDataset");
	public static final Property comparedToResource = ResourceFactory.createProperty(namespace, "comparedToResource");
	public static final Property comparedToValue = ResourceFactory.createProperty(namespace, "comparedToValue");
	public static final Property correspondsNotToResource = ResourceFactory.createProperty(namespace,
			"correspondsNotToResource");
	public static final Property correspondsToResource = ResourceFactory.createProperty(namespace,
			"correspondsToResource");
	public static final Resource count = ResourceFactory.createResource(namespace + "count");
	public static final Property definingQuery = ResourceFactory.createProperty(namespace, "definingQuery");
	public static final Resource Deviation = ResourceFactory.createResource(namespace + "Deviation");
	public static final Property hasParameter = ResourceFactory.createProperty(namespace, "hasParameter");
	public static final Property inputMetaDataGraph = ResourceFactory.createProperty(namespace, "inputMetaDataGraph");
	public static final Resource Issue = ResourceFactory.createResource(namespace + "Issue");
	public static final Property issueType = ResourceFactory.createProperty(namespace, "issueType");;
	public static final Property key = ResourceFactory.createProperty(namespace, "key");
	public static final Property keyVariableName = ResourceFactory.createProperty(namespace, "keyVariableName");
	// TODO markAndRecapture might become renamed
	public static final Resource marCompletenessThomas08 = ResourceFactory
			.createResource(namespace + "marCompletenessThomas08");
	public static final Resource MetaDataGraph = ResourceFactory.createResource(namespace + "MetaDataGraph");
	public static final Property ofAspect = ResourceFactory.createProperty(namespace, "ofAspect");
	public static final Resource Parameter = ResourceFactory.createResource(namespace + "Parameter");
	public static final Resource PrimaryDataGraph = ResourceFactory.createResource(namespace + "PrimaryDataGraph");
	public static final Property processorClass = ResourceFactory.createProperty(namespace, "processorClass");
	public static final Resource QualityAnnotationBody = ResourceFactory
			.createResource(namespace + "QualityAnnotationBody");
	public static final Resource QualityMeasurement = ResourceFactory.createResource(namespace + "QualityMeasurement");
	public static final Resource relativeCoverage = ResourceFactory.createResource(namespace + "relativeCoverage");
	public static final Property relevantResource = ResourceFactory.createProperty(namespace, "relevantResource");
	public static final Resource ResourceOmission = ResourceFactory.createResource(namespace + "ResourceOmission");
	public static final Resource Sparql11SelectQuery = ResourceFactory
			.createResource(namespace + "Sparql11SelectQuery");
	public static final Resource Step = ResourceFactory.createResource(namespace + "Step");
	public static final Resource StepExecution = ResourceFactory.createResource(namespace + "StepExecution");
	public static final Resource ValueOmission = ResourceFactory.createResource(namespace + "ValueOmission");
	public static final Resource WrongValue = ResourceFactory.createResource(namespace + "WrongValue");

	public static String getURI() {
		return namespace;
	}
}