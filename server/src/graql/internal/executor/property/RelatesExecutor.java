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

package grakn.core.graql.internal.executor.property;

import com.google.common.collect.ImmutableSet;
import grakn.core.graql.concept.ConceptId;
import grakn.core.graql.concept.RelationType;
import grakn.core.graql.concept.Role;
import grakn.core.graql.internal.executor.WriteExecutor;
import grakn.core.graql.internal.gremlin.EquivalentFragmentSet;
import grakn.core.graql.internal.reasoner.atom.Atomic;
import grakn.core.graql.internal.reasoner.atom.binary.RelatesAtom;
import grakn.core.graql.internal.reasoner.atom.predicate.IdPredicate;
import grakn.core.graql.internal.reasoner.query.ReasonerQuery;
import graql.lang.property.RelatesProperty;
import graql.lang.property.VarProperty;
import graql.lang.statement.Statement;
import graql.lang.statement.Variable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static grakn.core.graql.internal.gremlin.sets.EquivalentFragmentSets.relates;
import static grakn.core.graql.internal.gremlin.sets.EquivalentFragmentSets.sub;
import static grakn.core.graql.internal.reasoner.utils.ReasonerUtils.getIdPredicate;

public class RelatesExecutor implements PropertyExecutor.Definable {

    private final Variable var;
    private final RelatesProperty property;

    RelatesExecutor(Variable var, RelatesProperty property) {
        this.var = var;
        this.property = property;
    }

    @Override
    public Set<EquivalentFragmentSet> matchFragments() {
        Statement superRole = property.superRole();
        EquivalentFragmentSet relates = relates(property, var, property.role().var());
        if (superRole == null) {
            return ImmutableSet.of(relates);
        } else {
            return ImmutableSet.of(relates, sub(property, property.role().var(), superRole.var()));
        }
    }

    @Override
    public Atomic atomic(ReasonerQuery parent, Statement statement, Set<Statement> otherStatements) {
        IdPredicate predicate = getIdPredicate(property.role().var(), property.role(), otherStatements, parent);
        ConceptId predicateId = predicate != null ? predicate.getPredicate() : null;
        return RelatesAtom.create(var.asUserDefined(), property.role().var(), predicateId, parent);
    }

    @Override
    public Set<PropertyExecutor.Writer> defineExecutors() {
        Set<PropertyExecutor.Writer> defineExecutors = new HashSet<>();
        defineExecutors.add(new DefineRelates());
        defineExecutors.add(new DefineRole());

        if (property.superRole() != null) {
            defineExecutors.add(new DefineSuperRole());
        }

        return Collections.unmodifiableSet(defineExecutors);
    }

    @Override
    public Set<PropertyExecutor.Writer> undefineExecutors() {
        return ImmutableSet.of(new UndefineRelates());
    }

    private abstract class RelatesWriter {

        public Variable var() {
            return var;
        }

        public VarProperty property() {
            return property;
        }
    }

    private class DefineRelates extends RelatesWriter implements PropertyExecutor.Writer {

        @Override
        public Set<Variable> requiredVars() {
            Set<Variable> required = new HashSet<>();
            required.add(var);
            required.add(property.role().var());

            return Collections.unmodifiableSet(required);
        }

        @Override
        public Set<Variable> producedVars() {
            return ImmutableSet.of();
        }

        @Override
        public void execute(WriteExecutor executor) {
            Role role = executor.getConcept(property.role().var()).asRole();
            executor.getConcept(var).asRelationshipType().relates(role);
        }
    }

    private class DefineRole extends RelatesWriter implements PropertyExecutor.Writer {

        @Override
        public Set<Variable> requiredVars() {
            return ImmutableSet.of();
        }

        @Override
        public Set<Variable> producedVars() {
            return ImmutableSet.of(property.role().var());
        }

        @Override
        public void execute(WriteExecutor executor) {
            executor.getBuilder(property.role().var()).isRole();
        }
    }

    private class DefineSuperRole extends RelatesWriter implements PropertyExecutor.Writer {

        @Override
        public Set<Variable> requiredVars() {
            return ImmutableSet.of(property.superRole().var());
        }

        @Override
        public Set<Variable> producedVars() {
            return ImmutableSet.of(property.role().var());
        }

        @Override
        public void execute(WriteExecutor executor) {
            Role superRole = executor.getConcept(property.superRole().var()).asRole();
            executor.getBuilder(property.role().var()).sub(superRole);
        }
    }

    private class UndefineRelates extends RelatesWriter implements PropertyExecutor.Writer {

        @Override
        public Set<Variable> requiredVars() {
            Set<Variable> required = new HashSet<>();
            required.add(var);
            required.add(property.role().var());

            return Collections.unmodifiableSet(required);
        }

        @Override
        public Set<Variable> producedVars() {
            return ImmutableSet.of();
        }

        @Override
        public void execute(WriteExecutor executor) {
            RelationType relationshipType = executor.getConcept(var).asRelationshipType();
            Role role = executor.getConcept(property.role().var()).asRole();

            if (!relationshipType.isDeleted() && !role.isDeleted()) {
                relationshipType.unrelate(role);
            }
        }
    }
}
