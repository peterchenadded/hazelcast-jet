/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.jet.impl.operation;

import com.hazelcast.internal.cluster.MemberInfo;
import com.hazelcast.jet.impl.JetService;
import com.hazelcast.jet.impl.execution.init.ExecutionPlan;
import com.hazelcast.jet.impl.execution.init.JetInitDataSerializerHook;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.ExceptionAction;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.hazelcast.jet.impl.execution.init.CustomClassLoadedObject.deserializeWithCustomClassLoader;
import static com.hazelcast.jet.impl.util.ExceptionUtil.isRestartableException;
import static com.hazelcast.jet.impl.util.Util.jobIdAndExecutionId;
import static com.hazelcast.spi.ExceptionAction.THROW_EXCEPTION;

public class InitExecutionOperation extends AbstractJobOperation {

    private long executionId;
    private int coordinatorMemberListVersion;
    private Set<MemberInfo> participants;
    private Data serializedPlan;

    public InitExecutionOperation() {
    }

    public InitExecutionOperation(long jobId, long executionId, int coordinatorMemberListVersion,
                                  Set<MemberInfo> participants, Data serializedPlan) {
        super(jobId);
        this.executionId = executionId;
        this.coordinatorMemberListVersion = coordinatorMemberListVersion;
        this.participants = participants;
        this.serializedPlan = serializedPlan;
    }

    @Override
    public void run() {
        ILogger logger = getLogger();
        JetService service = getService();

        Address caller = getCallerAddress();
        logger.fine("Initializing execution plan for " + jobIdAndExecutionId(jobId(), executionId) + " from " + caller);

        ExecutionPlan plan = deserializePlan(serializedPlan);
        service.getJobExecutionService().initExecution(
                jobId(), executionId, caller, coordinatorMemberListVersion, participants, plan
        );
    }

    @Override
    public ExceptionAction onInvocationException(Throwable throwable) {
        return isRestartableException(throwable) ? THROW_EXCEPTION : super.onInvocationException(throwable);
    }

    @Override
    public int getId() {
        return JetInitDataSerializerHook.INIT_EXECUTION_OP;
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);

        out.writeLong(executionId);
        out.writeInt(coordinatorMemberListVersion);
        out.writeInt(participants.size());
        for (MemberInfo participant : participants) {
            participant.writeData(out);
        }
        out.writeData(serializedPlan);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);

        executionId = in.readLong();
        coordinatorMemberListVersion = in.readInt();
        int count = in.readInt();
        participants = new HashSet<>();
        for (int i = 0; i < count; i++) {
            MemberInfo participant = new MemberInfo();
            participant.readData(in);
            participants.add(participant);
        }
        serializedPlan = in.readData();
    }

    private ExecutionPlan deserializePlan(Data planBlob) {
        JetService service = getService();
        ClassLoader cl = service.getClassLoader(jobId());
        return deserializeWithCustomClassLoader(getNodeEngine().getSerializationService(), cl, planBlob);
    }
}
