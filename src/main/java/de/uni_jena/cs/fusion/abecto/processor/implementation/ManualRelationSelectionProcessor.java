package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.Template;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.util.SparqlUtil;
import de.uni_jena.cs.fusion.abecto.util.Vocabulary;

public class ManualRelationSelectionProcessor
		extends AbstractMetaProcessor<ManualRelationSelectionProcessor.Parameter> {

	@Override
	protected Model computeResultModel() throws Exception {
		Var uuidVar = Var.alloc(NodeFactory.createVariable("uuid"));
		Var nameVar = Var.alloc(NodeFactory.createVariable("name"));
		Var pathVar = Var.alloc(NodeFactory.createVariable("path"));
		Var enabledVar = Var.alloc(NodeFactory.createVariable("enabled"));

		Query query = QueryFactory.make();
		query.setQueryConstructType();
		BasicPattern construct = new BasicPattern();
		Node blankNode = NodeFactory.createBlankNode();
		construct.add(new Triple(blankNode, Vocabulary.KNOWLEDGE_BASE.asNode(), uuidVar));
		construct.add(new Triple(blankNode, Vocabulary.RELATION_TYPE_NAME.asNode(), nameVar));
		construct.add(new Triple(blankNode, Vocabulary.RELATION_ASSIGNMENT_PATH.asNode(), pathVar));
		construct.add(new Triple(blankNode, Vocabulary.RELATION_TYPE_ASSIGNMENT_ENABLED.asNode(), enabledVar));
		query.setConstructTemplate(new Template(construct));

		ElementGroup pattern = new ElementGroup();
		query.setQueryPattern(pattern);

		BasicPattern condition = new BasicPattern();
		condition.add(new Triple(blankNode, Vocabulary.KNOWLEDGE_BASE.asNode(), uuidVar));
		condition.add(new Triple(blankNode, Vocabulary.RELATION_TYPE_NAME.asNode(), nameVar));
		condition.add(new Triple(blankNode, Vocabulary.RELATION_ASSIGNMENT_PATH.asNode(), pathVar));
		pattern.addElementFilter(new ElementFilter(new E_NotExists(new ElementTriplesBlock(condition))));

		ElementData values = new ElementData();
		values.add(uuidVar);
		values.add(nameVar);
		values.add(pathVar);
		values.add(enabledVar);
		pattern.addElement(values);

		Optional<Map<String, String>> categoriesParameterOptional = this.getParameters().relations;
		if (categoriesParameterOptional.isPresent()) {
			for (Entry<String, String> categoryEntry : categoriesParameterOptional.orElseThrow().entrySet()) {
				String categoryName = categoryEntry.getKey();
				String pathPattern = SparqlUtil.sanitizePath(categoryEntry.getValue());
				for (UUID uuid : this.inputGroupModels.keySet()) {
					BindingHashMap binding = new BindingHashMap();
					binding.add(uuidVar, NodeFactory.createLiteral(uuid.toString()));
					binding.add(nameVar, NodeFactory.createLiteral(categoryName));
					binding.add(pathVar, NodeFactory.createLiteral(pathPattern));
					binding.add(enabledVar, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
					values.add(binding);
				}
			}
		}

		Optional<Map<String, String>> suppressedCategoriesParameterOptional = this.getParameters().suppressed_relations;
		if (suppressedCategoriesParameterOptional.isPresent()) {
			for (Entry<String, String> categoryEntry : suppressedCategoriesParameterOptional.orElseThrow().entrySet()) {
				String categoryName = categoryEntry.getKey();
				String pathPattern = SparqlUtil.sanitizePath(categoryEntry.getValue());
				for (UUID uuid : this.inputGroupModels.keySet()) {
					BindingHashMap binding = new BindingHashMap();
					binding.add(uuidVar, NodeFactory.createLiteral(uuid.toString()));
					binding.add(nameVar, NodeFactory.createLiteral(categoryName));
					binding.add(pathVar, NodeFactory.createLiteral(pathPattern));
					binding.add(enabledVar, NodeFactory.createLiteralByValue(false, XSDDatatype.XSDboolean));
					values.add(binding);
				}
			}
		}

		LoggerFactory.getLogger(ManualRelationSelectionProcessor.class).info(query.serialize());

		// prepare execution
		QueryExecution queryExecution = QueryExecutionFactory.create(query, this.inputModelUnion);

		// execute and return result
		return queryExecution.execConstruct();
	}

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public Optional<Map<String, String>> relations = Optional.empty();
		public Optional<Map<String, String>> suppressed_relations = Optional.empty();
	}

}