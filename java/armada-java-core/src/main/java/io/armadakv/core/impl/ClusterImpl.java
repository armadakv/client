/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core.impl;

import io.armadakv.core.Cluster;
import io.armadakv.core.RetryConfig;
import io.armadakv.core.api.Member;
import io.armadakv.core.api.MemberListResponse;
import armada.v1.ClusterGrpc;
import armada.v1.MemberListRequest;
import io.grpc.Channel;

public class ClusterImpl extends Impl implements Cluster {

    private final ClusterGrpc.ClusterBlockingStub stub;

    ClusterImpl(Channel managedChannel, RetryConfig retryConfig) {
        super(retryConfig);
        stub = ClusterGrpc.newBlockingStub(managedChannel);
    }

    @Override
    public MemberListResponse memberList() {
        return execute(
                () -> stub.memberList(MemberListRequest.getDefaultInstance()),
                ml -> new MemberListResponse(
                        ml.getCluster(),
                        ml.getMembersList().stream()
                                .map(member -> new Member(
                                        member.getId(),
                                        member.getName(),
                                        member.getPeerURLsList().stream().toList(),
                                        member.getClientURLsList().stream().toList()
                                ))
                                .toList()),
                RETRY_TRANSIENT
        );
    }

}
