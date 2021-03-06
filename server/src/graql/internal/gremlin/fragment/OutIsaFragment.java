/*
 * GRAKN.AI - THE KNOWLEDGE GRAPH
 * Copyright (C) 2018 Grakn Labs Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.core.graql.internal.gremlin.fragment;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import grakn.core.graql.internal.gremlin.spanningtree.graph.DirectedEdge;
import grakn.core.graql.internal.gremlin.spanningtree.graph.Node;
import grakn.core.graql.internal.gremlin.spanningtree.graph.NodeId;
import grakn.core.graql.internal.gremlin.spanningtree.util.Weighted;
import grakn.core.server.session.TransactionOLTP;
import graql.lang.statement.Variable;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static grakn.core.graql.internal.Schema.EdgeLabel.ISA;
import static grakn.core.graql.internal.Schema.EdgeLabel.SHARD;
import static grakn.core.graql.internal.Schema.EdgeProperty.RELATIONSHIP_TYPE_LABEL_ID;

/**
 * A fragment representing traversing an isa edge from instance to type.
 *
 */

@AutoValue
public abstract class OutIsaFragment extends Fragment {

    @Override
    public abstract Variable end();

    @Override
    public GraphTraversal<Vertex, ? extends Element> applyTraversalInner(
            GraphTraversal<Vertex, ? extends Element> traversal, TransactionOLTP graph, Collection<Variable> vars) {

        // from the traversal, branch to take either of these paths
        return Fragments.union(traversal, ImmutableSet.of(
                Fragments.isVertex(__.identity()).out(ISA.getLabel()).out(SHARD.getLabel()),
                edgeTraversal() // what is this doing?
        ));
    }

    private GraphTraversal<Element, Vertex> edgeTraversal() {
        return Fragments.traverseSchemaConceptFromEdge(Fragments.isEdge(__.identity()), RELATIONSHIP_TYPE_LABEL_ID);
    }

    @Override
    public String name() {
        return "-[isa]->";
    }

    @Override
    public double internalFragmentCost() {
        return COST_SAME_AS_PREVIOUS;
    }

    @Override
    public Set<Weighted<DirectedEdge<Node>>> directedEdges(Map<NodeId, Node> nodes,
                                                           Map<Node, Map<Node, Fragment>> edges) {
        return directedEdges(NodeId.NodeType.ISA, nodes, edges);
    }

    @Override
    public boolean canOperateOnEdges() {
        return true;
    }
}
