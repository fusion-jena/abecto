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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

abstract class Measure<K, V extends Number> {

    final Map<K, V> values = new HashMap<>();
    final Resource quantity;
    final Resource unit;

    public Measure(Resource quantity, Resource unit) {
        this.quantity = quantity;
        this.unit = unit;
    }

    public V get(K key) {
        return values.get(key);
    }

    public Set<K> keySet() {
        return values.keySet();
    }

    public boolean contains(K key) {
        return values.containsKey(key);
    }

    public void clear() {
        values.clear();
    }

    public void reset(Iterable<K> keys, V initialValue) {
        clear();
        setAll(keys, initialValue);
    }

    public void setAll(Iterable<K> keys, V initialValue) {
        assert initialValue != null;
        for (K key : keys) {
            set(key, initialValue);
        }
    }

    public void set(K key, V value) {
        values.put(key, value);
    }

}
