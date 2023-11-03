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
package de.uni_jena.cs.fusion.abecto.util;

import java.util.Arrays;

import org.apache.jena.rdf.model.RDFNode;

public class Values {
	private final RDFNode[] valuesArray;

	public Values(RDFNode[] valuesArray) {
		this.valuesArray = valuesArray;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		for (RDFNode node : valuesArray) {
			if (node != null) {
				hash ^= node.hashCode();
			}
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof Values && Arrays.equals(((Values) obj).valuesArray, this.valuesArray);
	}

	public RDFNode[] getValuesArray() {
		return Arrays.copyOf(valuesArray, valuesArray.length);
	}

}
