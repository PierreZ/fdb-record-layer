/*
 * RecordQueryPlannerConfiguration.java
 *
 * This source file is part of the FoundationDB open source project
 *
 * Copyright 2015-2020 Apple Inc. and the FoundationDB project authors
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

package com.apple.foundationdb.record.query.plan;

import com.apple.foundationdb.annotation.API;

import javax.annotation.Nonnull;

/**
 * A set of configuration options for the {@link RecordQueryPlanner}.
 */
@API(API.Status.MAINTAINED)
public class RecordQueryPlannerConfiguration {
    @Nonnull
    private final QueryPlanner.IndexScanPreference indexScanPreference;
    private boolean attemptFailedInJoinAsOr;

    private RecordQueryPlannerConfiguration(@Nonnull QueryPlanner.IndexScanPreference indexScanPreference,
                                            boolean attemptFailedInJoinAsOr) {
        this.indexScanPreference = indexScanPreference;
        this.attemptFailedInJoinAsOr = attemptFailedInJoinAsOr;
    }

    /**
     * Get whether {@link com.apple.foundationdb.record.query.plan.plans.RecordQueryIndexPlan} is preferred over
     * {@link com.apple.foundationdb.record.query.plan.plans.RecordQueryScanPlan} even when it does not satisfy any
     * additional conditions.
     * Scanning without an index is more efficient, but will have to skip over unrelated record types.
     * For that reason, it is safer to use an index, except when there is only one record type.
     * If the meta-data has more than one record type but the record store does not, this can be overridden.
     * @return the index scan preference
     */
    @Nonnull
    public QueryPlanner.IndexScanPreference getIndexScanPreference() {
        return indexScanPreference;
    }

    /**
     * Get whether the query planner should attempt to transform IN predicates that can't be implemented using a
     * {@link com.apple.foundationdb.record.query.plan.plans.RecordQueryInJoinPlan} into an equivalent OR of
     * equality predicates, which might be plannable as a union.
     * @return whether the planner will transform IN predicates into ORs when they can't be planned as in-joins
     */
    public boolean shouldAttemptFailedInJoinAsOr() {
        return attemptFailedInJoinAsOr;
    }

    @Nonnull
    public Builder asBuilder() {
        return new Builder(this);
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link RecordQueryPlannerConfiguration}.
     */
    public static class Builder {
        @Nonnull
        private QueryPlanner.IndexScanPreference indexScanPreference = QueryPlanner.IndexScanPreference.PREFER_SCAN;
        private boolean attemptFailedInJoinAsOr = false;

        public Builder(@Nonnull RecordQueryPlannerConfiguration configuration) {
            this.indexScanPreference = configuration.indexScanPreference;
            this.attemptFailedInJoinAsOr = configuration.attemptFailedInJoinAsOr;
        }

        public Builder() {
        }

        public Builder setIndexScanPreference(@Nonnull QueryPlanner.IndexScanPreference indexScanPreference) {
            this.indexScanPreference = indexScanPreference;
            return this;
        }

        public Builder setAttemptFailedInJoinAsOr(boolean attemptFailedInJoinAsOr) {
            this.attemptFailedInJoinAsOr = attemptFailedInJoinAsOr;
            return this;
        }

        public RecordQueryPlannerConfiguration build() {
            return new RecordQueryPlannerConfiguration(indexScanPreference, attemptFailedInJoinAsOr);
        }
    }
}
