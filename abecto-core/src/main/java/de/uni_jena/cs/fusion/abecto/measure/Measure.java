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

import de.uni_jena.cs.fusion.abecto.Aspect;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Measure<K, V extends Number> {

    protected final Map<K, V> values = new HashMap<>();
    protected final Resource quantity;
    protected final Resource unit;
    String variable;

    public Measure(Resource quantity, Resource unit) {
        this.quantity = quantity;
        this.unit = unit;
    }

    public static <T extends Measure<?,?>> Map<String, T> createMapByVariable(Iterable<String> variables, Class<T> type) {
        Map<String, T> mapOfCounts = new HashMap<>();
        for (String variable : variables) {
            try {
                T countOfVariable = type.getConstructor().newInstance();
                countOfVariable.setVariable(variable);
                mapOfCounts.put(variable, countOfVariable);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return mapOfCounts;
    }

    public static <K, V extends Number, M extends Measure<K, V>> void storeMeasuresByVariableInModel(Map<String, M> measuresByVariable, Aspect aspect, Map<Resource, Model> outputModelsMap) {
        for (M measure : measuresByVariable.values()) {
            measure.storeInModel(aspect, outputModelsMap);
        }
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

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public abstract void storeInModel(Aspect aspect, Map<Resource, Model> outputModelsMap);

}
