/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class CompletenessProcessor extends PopulationComparisonProcessor {
	final static Logger log = LoggerFactory.getLogger(CompletenessProcessor.class);

	@Override
	public void run() {
		log.warn("Deprecated: Use PopulationComparisonProcessor.");
		log.info("PopulationComparisonProcessor started.");
		super.run();
	}

}
