/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core.impl;

import com.google.protobuf.ByteString;
import io.armadakv.core.KV;
import io.armadakv.core.RetryConfig;
import io.armadakv.core.api.KeyValue;
import io.armadakv.core.api.PutResponse;
import io.armadakv.core.api.Txn;
import io.armadakv.core.api.TxnResponse;
import io.armadakv.core.api.*;
import io.armadakv.core.api.op.TxnImpl;
import io.armadakv.core.encoding.SnappyCodec;
import io.armadakv.core.options.*;
import armada.v1.*;
import io.grpc.Channel;
import mvcc.v1.Mvcc.RequestOp;

import java.util.LinkedList;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class KVImpl extends Impl implements KV {

    private final KVGrpc.KVBlockingStub stub;

    KVImpl(Channel managedChannel, RetryConfig retryConfig) {
        super(retryConfig);
        stub = KVGrpc.newBlockingStub(managedChannel).withCompression(SnappyCodec.NAME);
    }

    @Override
    public PutResponse put(ByteSequence table, ByteSequence key, ByteSequence value) {
        return put(table, key, value, PutOption.DEFAULT);
    }

    @Override
    public PutResponse put(ByteSequence table, ByteSequence key, ByteSequence value, PutOption option) {
        var request = PutRequest.newBuilder()
                .setTable(ByteString.copyFrom(table.getBytes()))
                .setKey(ByteString.copyFrom(key.getBytes()))
                .setValue(ByteString.copyFrom(value.getBytes()))
                .setPrevKv(option.getPrevKV())
                .build();
        return execute(
                () -> stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).put(request),
                put -> new PutResponse(new Response.HeaderImpl(put.getHeader()), new KeyValue(ByteSequence.from(put.getPrevKv().getKey()), ByteSequence.from(put.getPrevKv().getValue()))),
                RETRY_NEVER
        );
    }

    @Override
    public GetResponse get(ByteSequence table, ByteSequence key) {
        return get(table, key, GetOption.DEFAULT);
    }

    @Override
    public GetResponse get(ByteSequence table, ByteSequence key, GetOption option) {
        var request = RangeRequest.newBuilder()
                .setTable(ByteString.copyFrom(table.getBytes()))
                .setKey(ByteString.copyFrom(key.getBytes()))
                .setRangeEnd(option.getEndKey()
                        .map(byteSequence -> ByteString.copyFrom(byteSequence.getBytes()))
                        .orElseGet(() -> option.isPrefix() ? ByteString.copyFrom(OptionsUtil.prefixEndOf(key).getBytes()) : ByteString.EMPTY)
                )
                .setLimit(option.getLimit())
                .setCountOnly(option.isCountOnly())
                .setKeysOnly(option.isKeysOnly())
                .setLinearizable(!option.isSerializable())
                .build();

        return execute(
                () -> stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).range(request),
                get -> {
                    var kvs = get.getKvsList().stream().map(keyValue -> new KeyValue(ByteSequence.from(keyValue.getKey()), ByteSequence.from(keyValue.getValue()))).toList();
                    return new GetResponse(new Response.HeaderImpl(get.getHeader()), kvs, get.getCount());
                },
                option.isSerializable() ? RETRY_ALWAYS : RETRY_TRANSIENT
        );
    }

    @Override
    public Stream<GetResponse> iterate(ByteSequence table, ByteSequence key) {
        return iterate(table, key, GetOption.DEFAULT);
    }

    @Override
    public Stream<GetResponse> iterate(ByteSequence table, ByteSequence key, GetOption option) {
        var request = RangeRequest.newBuilder()
                .setTable(ByteString.copyFrom(table.getBytes()))
                .setKey(ByteString.copyFrom(key.getBytes()))
                .setRangeEnd(option.getEndKey()
                        .map(byteSequence -> ByteString.copyFrom(byteSequence.getBytes()))
                        .orElseGet(() -> option.isPrefix() ? ByteString.copyFrom(OptionsUtil.prefixEndOf(key).getBytes()) : ByteString.EMPTY)
                )
                .setLimit(option.getLimit())
                .setCountOnly(option.isCountOnly())
                .setKeysOnly(option.isKeysOnly())
                .setLinearizable(!option.isSerializable())
                .build();
        return execute(
                () -> stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).iterateRange(request),
                ir -> StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(ir, Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE),
                                false)
                        .map(get -> new GetResponse(
                                new Response.HeaderImpl(get.getHeader()),
                                get.getKvsList().stream().map(keyValue -> new KeyValue(ByteSequence.from(keyValue.getKey()), ByteSequence.from(keyValue.getValue()))).toList(),
                                get.getCount()
                        )),
                option.isSerializable() ? RETRY_ALWAYS : RETRY_TRANSIENT
        );
    }

    @Override
    public DeleteResponse delete(ByteSequence table, ByteSequence key) {
        return delete(table, key, DeleteOption.DEFAULT);
    }

    @Override
    public DeleteResponse delete(ByteSequence table, ByteSequence key, DeleteOption option) {
        var request = DeleteRangeRequest.newBuilder()
                .setTable(ByteString.copyFrom(table.getBytes()))
                .setKey(ByteString.copyFrom(key.getBytes()))
                .setRangeEnd(option.getEndKey()
                        .map(byteSequence -> ByteString.copyFrom(byteSequence.getBytes()))
                        .orElseGet(() -> option.isPrefix() ? ByteString.copyFrom(OptionsUtil.prefixEndOf(key).getBytes()) : ByteString.EMPTY)
                )
                .setCount(option.isCount())
                .setPrevKv(option.isPrevKV())
                .build();
        return execute(
                () -> stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).deleteRange(request),
                delete -> {
                    var kvs = delete.getPrevKvsList().stream().map(keyValue -> new KeyValue(ByteSequence.from(keyValue.getKey()), ByteSequence.from(keyValue.getValue()))).toList();
                    return new DeleteResponse(new Response.HeaderImpl(delete.getHeader()), kvs, delete.getDeleted());
                },
                RETRY_NEVER
        );
    }

    @Override
    public Txn txn(ByteSequence table) {
        return txn(table, TxnOption.DEFAULT);
    }

    @Override
    public Txn txn(ByteSequence table, TxnOption option) {
        return TxnImpl.newTxn(request -> this.txn(request, option), table);
    }

    private TxnResponse txn(TxnRequest request, TxnOption option) {
        return execute(
                () -> stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).txn(request),
                txnResult -> new TxnResponse(new Response.HeaderImpl(txnResult.getHeader()), txnResult.getSucceeded(), txnResult.getResponsesList()),
                isReadonlyTxn(request) ? RETRY_TRANSIENT : RETRY_NEVER
        );
    }

    private static boolean isReadonlyTxn(TxnRequest request) {
        var ops = new LinkedList<RequestOp>();
        ops.addAll(request.getSuccessList());
        ops.addAll(request.getFailureList());
        return ops.stream().allMatch(RequestOp::hasRequestRange);
    }
}
