/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.memory.operation.joiner;

import com.hazelcast.jet.io.SerializationOptimizer;
import com.hazelcast.jet.io.Pair;
import com.hazelcast.jet.memory.binarystorage.SortOrder;
import com.hazelcast.jet.memory.binarystorage.comparator.Comparator;
import com.hazelcast.jet.memory.memoryblock.MemoryChainingRule;
import com.hazelcast.jet.memory.memoryblock.MemoryContext;
import com.hazelcast.jet.memory.operation.aggregator.SortedJoinAggregator;
import com.hazelcast.jet.memory.operation.aggregator.SortedPartitionedAggregator;

/**
 * Sorted partitioned self-joiner.
 */
public class SortedPartitionedSelfJoiner extends SortedPartitionedAggregator implements SortedJoinAggregator {
    private final int numSelfJoins;

    @SuppressWarnings("checkstyle:parameternumber")
    public SortedPartitionedSelfJoiner(
            int numSelfJoins, int partitionCount, int spillingBufferSize, SerializationOptimizer optimizer, Comparator comparator,
            MemoryContext memoryContext, MemoryChainingRule memoryChainingRule, Pair pair,
            String spillingDirectory, SortOrder sortOrder, int spillingChunkSize, boolean spillToDisk,
            boolean useBigEndian
    ) {
        super(partitionCount, spillingBufferSize, optimizer, comparator, memoryContext, memoryChainingRule,
                pair, spillingDirectory, sortOrder, spillingChunkSize, spillToDisk, useBigEndian);

        this.numSelfJoins = numSelfJoins;
    }

    @Override
    public void setSource(short source) {
        assert source == 0;
    }
}