/*-
 * Copyright © 2019-2022 Heinz Nixdorf Chair for Distributed Information Systems,
 *                       Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 * Copyright © 2023-2024 Jan Martin Keil (jan-martin.keil@uni-jena.de)
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
-*/

package de.uni_jena.cs.fusion.abecto.measure;

import org.apache.jena.rdf.model.Resource;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class Ratio<K> extends Measure<K, BigDecimal> {

    public final static int SCALE = 16;
    public final static RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public Ratio(Resource quantity, Resource unit) {
        super(quantity, unit);
    }

    public void setRatioOf(Count<K> numerators, Count<K> denominators) {
        for (K key : numerators.keySet()) {
            if (denominators.contains(key)) {
                BigDecimal numerator = BigDecimal.valueOf(numerators.get(key));
                BigDecimal denominator = BigDecimal.valueOf(denominators.get(key));
                set(key, numerator.divide(denominator, SCALE, ROUNDING_MODE));
            }
        }
    }

    public void setRatioOf(Count<K> numerators, BigDecimal denominator) {
        for (K key : numerators.keySet()) {
                BigDecimal numerator = BigDecimal.valueOf(numerators.get(key));
                set(key, numerator.divide(denominator, SCALE, ROUNDING_MODE));
        }
    }
}
