/*
 * RecordQueryScanPlan.java
 *
 * This source file is part of the FoundationDB open source project
 *
 * Copyright 2015-2018 Apple Inc. and the FoundationDB project authors
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

package com.apple.foundationdb.record.query.plan.plans;

import com.apple.foundationdb.annotation.API;
import com.apple.foundationdb.record.EvaluationContext;
import com.apple.foundationdb.record.ExecuteProperties;
import com.apple.foundationdb.record.RecordCursor;
import com.apple.foundationdb.record.TupleRange;
import com.apple.foundationdb.record.provider.common.StoreTimer;
import com.apple.foundationdb.record.provider.foundationdb.FDBQueriedRecord;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordStoreBase;
import com.apple.foundationdb.record.provider.foundationdb.FDBStoreTimer;
import com.apple.foundationdb.record.query.expressions.Comparisons;
import com.apple.foundationdb.record.query.plan.ScanComparisons;
import com.apple.foundationdb.record.query.plan.temp.RelationalExpression;
import com.apple.foundationdb.record.query.plan.temp.explain.PlannerGraph;
import com.apple.foundationdb.record.query.plan.temp.explain.PlannerGraphRewritable;
import com.google.common.base.Verify;
import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A query plan that scans records directly from the main tree within a range of primary keys.
 */
@API(API.Status.MAINTAINED)
public class RecordQueryScanPlan implements RecordQueryPlanWithNoChildren, RecordQueryPlanWithComparisons, PlannerGraphRewritable {
    @Nonnull
    private final ScanComparisons comparisons;
    private boolean reverse;

    public RecordQueryScanPlan(@Nonnull ScanComparisons comparisons, boolean reverse) {
        this.comparisons = comparisons;
        this.reverse = reverse;
    }

    @Nonnull
    @Override
    public <M extends Message> RecordCursor<FDBQueriedRecord<M>> execute(@Nonnull FDBRecordStoreBase<M> store,
                                                                         @Nonnull EvaluationContext context,
                                                                         @Nullable byte[] continuation,
                                                                         @Nonnull ExecuteProperties executeProperties) {
        final TupleRange range = comparisons.toTupleRange(store, context);
        return store.scanRecords(
                range.getLow(), range.getHigh(), range.getLowEndpoint(), range.getHighEndpoint(), continuation,
                executeProperties.asScanProperties(reverse))
                .map(store::queriedRecord);
    }

    @Nonnull
    @Override
    public ScanComparisons getComparisons() {
        return comparisons;
    }

    @Override
    public boolean isReverse() {
        return reverse;
    }

    @Override
    public boolean hasRecordScan() {
        return true;
    }

    @Override
    public boolean hasFullRecordScan() {
        // full record scan happens iff the bounds of the scan fields are unlimited
        return comparisons.isEmpty();
    }

    @Override
    public boolean hasIndexScan(@Nonnull String indexName) {
        return false;
    }

    @Nonnull
    @Override
    public Set<String> getUsedIndexes() {
        return new HashSet<>();
    }

    @Override
    public boolean hasLoadBykeys() {
        return false;
    }

    @Nonnull
    @Override
    public String toString() {
        String range;
        range = getRange();
        return "Scan(" + range + ")";
    }

    @Nonnull
    private String getRange() {
        String range;
        try {
            range = comparisons.toTupleRange().toString();
        } catch (Comparisons.EvaluationContextRequiredException ex) {
            range = comparisons.toString();
        }
        return range;
    }

    @Override
    @API(API.Status.EXPERIMENTAL)
    public boolean equalsWithoutChildren(@Nonnull RelationalExpression otherExpression) {
        return equals(otherExpression);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecordQueryScanPlan that = (RecordQueryScanPlan) o;
        return reverse == that.reverse &&
                Objects.equals(comparisons, that.comparisons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comparisons, reverse);
    }

    @Override
    public int planHash() {
        return comparisons.planHash() + (reverse ? 1 : 0);
    }

    @Override
    public void logPlanStructure(StoreTimer timer) {
        timer.increment(FDBStoreTimer.Counts.PLAN_SCAN);
    }

    @Override
    public int getComplexity() {
        return 1;
    }

    /**
     * Rewrite the planner graph for better visualization of a query scan plan.
     * @param childGraphs planner graphs of children expression that already have been computed
     * @return the rewritten planner graph that models scanned storage as a separate node that is connected to the
     *         actual scan plan node.
     */
    @Nonnull
    @Override
    public PlannerGraph rewritePlannerGraph(@Nonnull List<? extends PlannerGraph> childGraphs) {
        Verify.verify(childGraphs.isEmpty());
        final PlannerGraph.Node root = new PlannerGraph.Node(this,
                getClass().getSimpleName());
        final PlannerGraph.SourceNode source = new PlannerGraph.SourceNode(getRange());
        return PlannerGraph.builder(root)
                .addNode(source)
                .addEdge(source, root, new PlannerGraph.Edge())
                .build();
    }
}
