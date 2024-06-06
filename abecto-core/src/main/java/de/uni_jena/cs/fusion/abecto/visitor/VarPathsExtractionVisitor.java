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

package de.uni_jena.cs.fusion.abecto.visitor;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.syntax.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class VarPathsExtractionVisitor implements ElementVisitor {

    private Map<Node, Map<Node, Path>> paths = new HashMap<>();

    @Override
    public final void visit(ElementTriplesBlock el) {
        el.getPattern().forEach(triple -> this.consumeTriplePath(new TriplePath(triple)));
    }

    @Override
    public final void visit(ElementDataset el) {
        el.getElement().visit(this);
    }

    @Override
    public final void visit(ElementFilter el) {
    }

    @Override
    public void visit(ElementAssign el) {
    }

    @Override
    public void visit(ElementBind el) {
    }

    @Override
    public void visit(ElementData el) {
    }

    @Override
    public final void visit(ElementUnion el) {
        for (Element subElement : el.getElements()) {
            subElement.visit(this);
        }
    }

    @Override
    public final void visit(ElementGroup el) {
        for (Element subElement : el.getElements()) {
            subElement.visit(this);
        }
    }

    @Override
    public final void visit(ElementOptional el) {
        el.getOptionalElement().visit(this);
    }

    @Override
    public void visit(ElementLateral el) {
        el.getLateralElement().visit(this);
    }

    @Override
    public final void visit(ElementNamedGraph el) {
        el.getElement().visit(this);
    }

    @Override
    public final void visit(ElementService el) {
        el.getElement().visit(this);
    }

    @Override
    public final void visit(ElementExists el) {
        el.getElement().visit(this);
    }

    @Override
    public final void visit(ElementNotExists el) {
        el.getElement().visit(this);
    }

    @Override
    public final void visit(ElementMinus el) {
        el.getMinusElement().visit(this);
    }

    @Override
    public void visit(ElementSubQuery el) {
    }

    @Override
    public void visit(ElementPathBlock el) {
        el.getPattern().forEach(this::consumeTriplePath);
    }

    private void consumeTriplePath(TriplePath triplePath) {
        Node subject = triplePath.getSubject();
        Path path = normalize(triplePath.getPath());
        Node object = triplePath.getObject();
        if ((subject.isVariable() || subject.isBlank()) && (object.isVariable() || object.isBlank())
                && path != null) {
//				paths.computeIfAbsent(subject, k -> new HashMap<>()).compute(object,
//						(k, v) -> (v == null) ? path : new P_Alt(path, v));
            paths.computeIfAbsent(subject, k -> new HashMap<>()).merge(object, path, P_Alt::new);
//				paths.computeIfAbsent(object, k -> new HashMap<>()).compute(subject,
//						(k, v) -> (v == null) ? inverse(path) : new P_Alt(inverse(path), v));
            paths.computeIfAbsent(object, k -> new HashMap<>()).merge(subject, inverse(path), P_Alt::new);
        }
    }

    private void expandPaths() {
        for (Node from : paths.keySet()) {
            for (Node by : new ArrayList<>(paths.get(from).keySet())) { // avoid ConcurrentModificationException
                for (Node to : paths.get(by).keySet()) {
                    boolean progress;
                    do {
                        progress = false;
                        if (!from.equals(to)) {
                            LinkedList<Path> direct = pathSeq2List(paths.get(from).get(by));
                            direct.addAll(pathSeq2List(paths.get(by).get(to)));
                            if (!paths.get(from).containsKey(to)
                                    || pathSeq2List(paths.get(from).get(to)).size() > direct.size()) {
                                paths.get(from).put(to, list2PathSeq(direct));
                                paths.computeIfAbsent(to, k -> new HashMap<>()).put(from,
                                        inverse(list2PathSeq(direct)));
                                progress = true;
                            }
                        }
                    } while (progress);
                }
            }
        }
    }

    private LinkedList<Path> pathSeq2List(Path path) {
        if (path instanceof P_Seq) {
            LinkedList<Path> list = pathSeq2List(((P_Seq) path).getLeft());
            list.addAll(pathSeq2List(((P_Seq) path).getRight()));
            return list;
        } else {
            LinkedList<Path> list = new LinkedList<>();
            list.add(path);
            return list;
        }
    }

    private Path list2PathSeq(LinkedList<Path> list) {
        Path path = list.getLast();
        for (int i = list.size() - 2; i >= 0; i--) {
            path = new P_Seq(list.get(i), path);
        }
        return path;
    }

    private Path normalize(Path path) {
        return (path == null) ? null : list2PathSeq(pathSeq2List(path));
    }

    public Map<String, Path> getPaths(Var from) {
        expandPaths();
        Map<String, Path> pathsToTarget = new HashMap<>();
        try {
            paths.get(from).forEach((node, path) -> {
                if (node.isVariable() && !Var.isBlankNodeVar(node)) {
                    pathsToTarget.put(node.getName(), path);
                }
            });
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(String.format("Variable \"%s\" not found.", from.getVarName()));
        }
        return pathsToTarget;
    }

    private Path inverse(Path path) {
        if (path instanceof P_Inverse) {
            return ((P_Inverse) path).getSubPath();
        } else if (path instanceof P_Seq) {
            return normalize(new P_Seq(inverse(((P_Seq) path).getRight()), inverse(((P_Seq) path).getLeft())));
        } else if (path instanceof P_Alt) {
            return new P_Alt(inverse(((P_Alt) path).getLeft()), inverse(((P_Alt) path).getRight()));
        } else {
            return new P_Inverse(path);
        }
    }

}
