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

public abstract class Count<K> extends Measure<K, Long> {

    public Count(Resource quantity, Resource unit) {
        super(quantity, unit);
    }

    public void incrementByOrSetOne(K key) {
        incrementByOrSet(key, 1);
    }

    public void incrementByOrSet(K key, long increment) {
        values.merge(key, increment, Long::sum);
    }

    public void setDifferenceOf(Count<K> minuend, Count<K> subtrahend) {
        for (K key : minuend.keySet()) {
            if (subtrahend.contains(key)) {
                set(key, minuend.get(key) - subtrahend.get(key));
            }
        }
    }

}
