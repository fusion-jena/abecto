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

package de.uni_jena.cs.fusion.abecto;

import org.apache.jena.rdf.model.Resource;

import java.util.HashSet;
import java.util.Set;

public class ResourcePair {
    public final Resource first;
    public final Resource second;

    private ResourcePair(Resource first, Resource second) {
        this.first = first;
        this.second = second;
    }

    public static Set<ResourcePair> getPairsWithoutRepetitionOf(Set<Resource> resources) {
        Set<ResourcePair> pairs = new HashSet<>();
        for (Resource first : resources) {
            for (Resource second : resources) {
                if (validOrder(first, second)) {
                    pairs.add(new ResourcePair(first, second));
                }
            }
        }
        return pairs;
    }

    public static Set<ResourcePair> getPairsWithRepetitionOf(Set<Resource> resources) {
        Set<ResourcePair> pairs = new HashSet<>();
        for (Resource first : resources) {
            for (Resource second : resources) {
                if (first.equals(second) || validOrder(first, second)) {
                    pairs.add(new ResourcePair(first, second));
                }
            }
        }
        return pairs;
    }

    public static ResourcePair getPair(Resource first, Resource second) {
        if (validOrder(first, second)) {
            return new ResourcePair(first, second);
        } else {
            return new ResourcePair(second, first);
        }
    }

    private static boolean validOrder(Resource first, Resource second) {
        // do not use Resource#getURI() as it might be null for blank nodes
        return first.hashCode() < second.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ResourcePair &&
                first.equals(((ResourcePair) obj).first) &&
                second.equals(((ResourcePair) obj).second);
    }

    @Override
    public int hashCode() {
        return first.hashCode() + second.hashCode();
    }

    @Override
    public String toString() {
        return "[" + first + "," + second + "]";
    }
}
