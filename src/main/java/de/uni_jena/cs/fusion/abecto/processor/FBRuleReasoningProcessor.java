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
