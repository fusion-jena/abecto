package de.uni_jena.cs.fusion.abecto.processor;

import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.FBRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

import de.uni_jena.cs.fusion.abecto.Parameter;

/**
 * Provides inferred primary data based on custom <a href=
 * "https://jena.apache.org/documentation/inference/#RULEhybrid">rules</a>. An
 * arbitrary number of rules can be set with the parameter {@link #rules}.
 */
public class FBRuleReasoningProcessor extends AbstractReasoningProcessor<FBRuleReasoningProcessor> {

	@Parameter
	public String rules;

	@Override
	public Reasoner getReasoner() {
		return new FBRuleReasoner(Rule.parseRules(rules));
	}

}
