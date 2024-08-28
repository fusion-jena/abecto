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

public class ResourceTupel {
    public final Resource first;
    public final Resource second;

    private ResourceTupel(Resource first, Resource second) {
        this.first = first;
        this.second = second;
    }

    public static Set<ResourceTupel> getTupelsOf(Set<Resource> resources) {
        Set<ResourceTupel> tupels = new HashSet<>();
        for (Resource first : resources) {
            for (Resource second : resources) {
                if (!first.equals(second)) {
                    tupels.add(new ResourceTupel(first, second));
                }
            }
        }
        return tupels;
    }

    public static ResourceTupel getTupel(Resource first, Resource second) {
        return new ResourceTupel(first, second);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ResourceTupel &&
                first.equals(((ResourceTupel) obj).first) &&
                second.equals(((ResourceTupel) obj).second);
    }

    @Override
    public int hashCode() {
        return first.hashCode() + (second.hashCode() >>> 1);
    }

    @Override
    public String toString() {
        return "(" + first + "," + second + ")";
    }
}
