/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core.impl;

import armada.v1.ClusterGrpc;
import armada.v1.DeleteRangeRequest;
import armada.v1.KVGrpc;
import armada.v1.Member;
import armada.v1.MemberListRequest;
import armada.v1.MemberListResponse;
import armada.v1.PutRequest;
import armada.v1.RangeRequest;
import armada.v1.RangeResponse;
import armada.v1.ResponseHeader;
import armada.v1.TxnRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.armadakv.core.RetryConfig;
import io.armadakv.core.api.ByteSequence;
import io.armadakv.core.api.DeleteResponse;
import io.armadakv.core.api.GetResponse;
import io.armadakv.core.api.PutResponse;
import io.armadakv.core.api.TxnResponse;
import io.armadakv.core.api.op.Cmp;
import io.armadakv.core.api.op.CmpTarget;
import io.armadakv.core.api.op.Op;
import io.armadakv.core.encoding.SnappyCodec;
import io.armadakv.core.options.DeleteOption;
import io.armadakv.core.options.GetOption;
import io.armadakv.core.options.PutOption;
import io.grpc.Codec;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import mvcc.v1.Mvcc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreConformanceTest {

    private KVImpl kvClient;
    private ClusterImpl clusterClient;
    private CoreConformanceFixture fixture;
    private ManagedChannel channel;
    private Server server;
    private MutableHandlerRegistry serviceRegistry;

    @BeforeEach
    void setUp() throws IOException {
        String serverName = UUID.randomUUID().toString();
        serviceRegistry = new MutableHandlerRegistry();

        var compressorRegistry = CompressorRegistry.newEmptyInstance();
        compressorRegistry.register(Codec.Identity.NONE);
        compressorRegistry.register(SnappyCodec.INSTANCE);

        var decompressorRegistry = DecompressorRegistry.emptyInstance()
                .with(Codec.Identity.NONE, false)
                .with(SnappyCodec.INSTANCE, true);

        server = InProcessServerBuilder.forName(serverName)
                .fallbackHandlerRegistry(serviceRegistry)
                .compressorRegistry(compressorRegistry)
                .decompressorRegistry(decompressorRegistry)
                .directExecutor()
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName)
                .compressorRegistry(compressorRegistry)
                .decompressorRegistry(decompressorRegistry)
                .directExecutor()
                .build();

        kvClient = new KVImpl(channel, RetryConfig.NO_RETRY);
        clusterClient = new ClusterImpl(channel, RetryConfig.NO_RETRY);
        fixture = CoreConformanceFixture.load();
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.shutdownNow();
        server.shutdownNow();
        channel.awaitTermination(1, TimeUnit.MINUTES);
        server.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    void getDefaultScenario() {
        var scenario = fixture.scenarios().getDefault();
        var stub = new KVConformanceStub();
        stub.rangeResponse = RangeResponse.newBuilder()
                .setHeader(header(scenario.response().header().revision()))
                .addAllKvs(kvList(scenario.response().kvs()))
                .setCount(scenario.response().count())
                .build();
        serviceRegistry.addService(stub);

        GetResponse response = kvClient.get(bytes(scenario.table()), bytes(scenario.key()));

        assertThat(stub.lastRangeRequest.getTable().toStringUtf8()).isEqualTo(scenario.table());
        assertThat(stub.lastRangeRequest.getKey().toStringUtf8()).isEqualTo(scenario.key());
        assertThat(stub.lastRangeRequest.getRangeEnd().toStringUtf8()).isEqualTo(scenario.expectedRequest().rangeEnd());
        assertThat(stub.lastRangeRequest.getLimit()).isEqualTo(scenario.expectedRequest().limit());
        assertThat(stub.lastRangeRequest.getLinearizable()).isEqualTo(scenario.expectedRequest().linearizable());
        assertThat(stub.lastRangeRequest.getKeysOnly()).isEqualTo(scenario.expectedRequest().keysOnly());
        assertThat(stub.lastRangeRequest.getCountOnly()).isEqualTo(scenario.expectedRequest().countOnly());

        assertThat(response.header().getRevision()).isEqualTo(scenario.response().header().revision());
        assertThat(response.kvs()).hasSize(1);
        assertThat(response.kvs().get(0).key().toString()).isEqualTo(scenario.response().kvs().get(0).key());
        assertThat(response.kvs().get(0).value().toString()).isEqualTo(scenario.response().kvs().get(0).value());
        assertThat(response.count()).isEqualTo(scenario.response().count());
    }

    @Test
    void getPrefixKeysOnlyScenario() {
        var scenario = fixture.scenarios().getPrefixKeysOnly();
        var stub = new KVConformanceStub();
        stub.rangeResponse = RangeResponse.newBuilder()
                .setHeader(header(scenario.response().header().revision()))
                .setCount(scenario.response().count())
                .build();
        serviceRegistry.addService(stub);

        GetResponse response = kvClient.get(
                bytes(scenario.table()),
                bytes(scenario.key()),
                GetOption.builder()
                        .isPrefix(scenario.options().prefix())
                        .withSerializable(scenario.options().serializable())
                        .withKeysOnly(scenario.options().keysOnly())
                        .withLimit(scenario.options().limit())
                        .build()
        );

        assertThat(stub.lastRangeRequest.getRangeEnd().toStringUtf8()).isEqualTo(scenario.expectedRequest().rangeEnd());
        assertThat(stub.lastRangeRequest.getLimit()).isEqualTo(scenario.expectedRequest().limit());
        assertThat(stub.lastRangeRequest.getLinearizable()).isEqualTo(scenario.expectedRequest().linearizable());
        assertThat(stub.lastRangeRequest.getKeysOnly()).isEqualTo(scenario.expectedRequest().keysOnly());
        assertThat(response.count()).isEqualTo(scenario.response().count());
    }

    @Test
    void getCountOnlyRangeScenario() {
        var scenario = fixture.scenarios().getCountOnlyRange();
        var stub = new KVConformanceStub();
        stub.rangeResponse = RangeResponse.newBuilder()
                .setHeader(header(scenario.response().header().revision()))
                .setCount(scenario.response().count())
                .build();
        serviceRegistry.addService(stub);

        GetResponse response = kvClient.get(
                bytes(scenario.table()),
                bytes(scenario.key()),
                GetOption.builder()
                        .withRange(bytes(scenario.options().rangeEnd()))
                        .withCountOnly(scenario.options().countOnly())
                        .build()
        );

        assertThat(stub.lastRangeRequest.getRangeEnd().toStringUtf8()).isEqualTo(scenario.expectedRequest().rangeEnd());
        assertThat(stub.lastRangeRequest.getCountOnly()).isEqualTo(scenario.expectedRequest().countOnly());
        assertThat(stub.lastRangeRequest.getLinearizable()).isEqualTo(scenario.expectedRequest().linearizable());
        assertThat(response.count()).isEqualTo(scenario.response().count());
        assertThat(response.kvs()).isEmpty();
    }

    @Test
    void iteratePrefixScenario() {
        var scenario = fixture.scenarios().iteratePrefix();
        var stub = new KVConformanceStub();
        stub.iterateResponses = scenario.responseChunks().stream()
                .map(chunk -> RangeResponse.newBuilder()
                        .setHeader(header(chunk.header().revision()))
                        .addAllKvs(kvList(chunk.kvs()))
                        .setCount(chunk.count())
                        .build())
                .toList();
        serviceRegistry.addService(stub);

        var responses = kvClient.iterate(
                bytes(scenario.table()),
                bytes(scenario.key()),
                GetOption.builder()
                        .isPrefix(scenario.options().prefix())
                        .withSerializable(scenario.options().serializable())
                        .withLimit(scenario.options().limit())
                        .build()
        ).toList();

        assertThat(stub.lastIterateRangeRequest.getRangeEnd().toStringUtf8()).isEqualTo(scenario.expectedRequest().rangeEnd());
        assertThat(stub.lastIterateRangeRequest.getLimit()).isEqualTo(scenario.expectedRequest().limit());
        assertThat(stub.lastIterateRangeRequest.getLinearizable()).isEqualTo(scenario.expectedRequest().linearizable());
        assertThat(responses).hasSize(scenario.responseChunks().size());
        assertThat(responses.get(0).kvs().get(0).key().toString()).isEqualTo(scenario.responseChunks().get(0).kvs().get(0).key());
        assertThat(responses.get(1).kvs().get(0).key().toString()).isEqualTo(scenario.responseChunks().get(1).kvs().get(0).key());
    }

    @Test
    void putPrevKvScenario() {
        var scenario = fixture.scenarios().putPrevKV();
        var stub = new KVConformanceStub();
        stub.putResponse = armada.v1.PutResponse.newBuilder()
                .setHeader(header(scenario.response().header().revision()))
                .setPrevKv(keyValue(scenario.response().prevKv()))
                .build();
        serviceRegistry.addService(stub);

        PutResponse response = kvClient.put(
                bytes(scenario.table()),
                bytes(scenario.key()),
                bytes(scenario.value()),
                PutOption.builder().withPrevKV().build()
        );

        assertThat(stub.lastPutRequest.getPrevKv()).isEqualTo(scenario.expectedRequest().prevKV());
        assertThat(response.prev().key().toString()).isEqualTo(scenario.response().prevKv().key());
        assertThat(response.prev().value().toString()).isEqualTo(scenario.response().prevKv().value());
    }

    @Test
    void deletePrefixPrevKvScenario() {
        var scenario = fixture.scenarios().deletePrefixPrevKV();
        var stub = new KVConformanceStub();
        stub.deleteResponse = armada.v1.DeleteRangeResponse.newBuilder()
                .setHeader(header(scenario.response().header().revision()))
                .setDeleted(scenario.response().deleted())
                .addAllPrevKvs(kvList(scenario.response().prevKvs()))
                .build();
        serviceRegistry.addService(stub);

        DeleteResponse response = kvClient.delete(
                bytes(scenario.table()),
                bytes(scenario.key()),
                DeleteOption.builder().isPrefix(scenario.options().prefix()).withPrevKV(scenario.options().prevKV()).build()
        );

        assertThat(stub.lastDeleteRangeRequest.getRangeEnd().toStringUtf8()).isEqualTo(scenario.expectedRequest().rangeEnd());
        assertThat(stub.lastDeleteRangeRequest.getPrevKv()).isEqualTo(scenario.expectedRequest().prevKV());
        assertThat(response.deleted()).isEqualTo(scenario.response().deleted());
        assertThat(response.prevKv().get(0).key().toString()).isEqualTo(scenario.response().prevKvs().get(0).key());
    }

    @Test
    void deleteRangeScenario() {
        var scenario = fixture.scenarios().deleteRange();
        var stub = new KVConformanceStub();
        stub.deleteResponse = armada.v1.DeleteRangeResponse.newBuilder()
                .setHeader(header(scenario.response().header().revision()))
                .setDeleted(scenario.response().deleted())
                .build();
        serviceRegistry.addService(stub);

        DeleteResponse response = kvClient.delete(
                bytes(scenario.table()),
                bytes(scenario.key()),
                DeleteOption.builder().withRange(bytes(scenario.options().rangeEnd())).build()
        );

        assertThat(stub.lastDeleteRangeRequest.getRangeEnd().toStringUtf8()).isEqualTo(scenario.expectedRequest().rangeEnd());
        assertThat(stub.lastDeleteRangeRequest.getPrevKv()).isEqualTo(scenario.expectedRequest().prevKV());
        assertThat(response.deleted()).isEqualTo(scenario.response().deleted());
    }

    @Test
    void txnCompareAndOpsScenario() {
        var scenario = fixture.scenarios().txnCompareAndOps();
        var stub = new KVConformanceStub();
        stub.txnResponse = armada.v1.TxnResponse.newBuilder()
                .setHeader(header(scenario.response().header().revision()))
                .setSucceeded(scenario.response().succeeded())
                .addResponses(mvcc.v1.Mvcc.ResponseOp.newBuilder()
                        .setResponsePut(mvcc.v1.Mvcc.ResponseOp.Put.newBuilder().setPrevKv(keyValue(scenario.response().responses().get(0).prevKv())).build())
                        .build())
                .addResponses(mvcc.v1.Mvcc.ResponseOp.newBuilder()
                        .setResponseDeleteRange(mvcc.v1.Mvcc.ResponseOp.DeleteRange.newBuilder()
                                .setDeleted(scenario.response().responses().get(1).deleted())
                                .addAllPrevKvs(kvList(scenario.response().responses().get(1).prevKvs()))
                                .build())
                        .build())
                .build();
        serviceRegistry.addService(stub);

        TxnResponse response = kvClient.txn(bytes(scenario.table()))
                .If(new Cmp(bytes(scenario.compare().key()), Cmp.Op.valueOf(scenario.compare().result()), CmpTarget.value(bytes(scenario.compare().value()))))
                .Then(
                        Op.put(bytes(scenario.success().get(0).key()), bytes(scenario.success().get(0).value()), PutOption.builder().withPrevKV().build()),
                        Op.delete(bytes(scenario.success().get(1).key()), DeleteOption.builder().withPrevKV(true).build()))
                .Else(Op.get(bytes(scenario.failure().get(0).key()), GetOption.DEFAULT))
                .commit();

        assertThat(stub.lastTxnRequest.getCompareList()).hasSize(1);
        assertThat(stub.lastTxnRequest.getCompare(0).getKey().toStringUtf8()).isEqualTo(scenario.compare().key());
        assertThat(stub.lastTxnRequest.getCompare(0).getValue().toStringUtf8()).isEqualTo(scenario.compare().value());
        assertThat(stub.lastTxnRequest.getSuccessList()).hasSize(scenario.success().size());
        assertThat(stub.lastTxnRequest.getFailureList()).hasSize(scenario.failure().size());
        assertThat(stub.lastTxnRequest.getSuccess(0).getRequestPut().getPrevKv()).isEqualTo(scenario.success().get(0).prevKV());
        assertThat(stub.lastTxnRequest.getSuccess(1).getRequestDeleteRange().getPrevKv()).isEqualTo(scenario.success().get(1).prevKV());

        assertThat(response.succeeded()).isEqualTo(scenario.response().succeeded());
        assertThat(response.responses()).hasSize(scenario.response().responses().size());
        assertThat(response.responses().get(0).hasResponsePut()).isTrue();
        assertThat(response.responses().get(1).hasResponseDeleteRange()).isTrue();
    }

    @Test
    void txnFailureOpsScenario() {
        var scenario = fixture.scenarios().txnFailureOps();
        var stub = new KVConformanceStub();
        stub.txnResponse = armada.v1.TxnResponse.newBuilder()
                .setHeader(header(scenario.response().header().revision()))
                .setSucceeded(scenario.response().succeeded())
                .addResponses(mvcc.v1.Mvcc.ResponseOp.newBuilder()
                        .setResponseRange(mvcc.v1.Mvcc.ResponseOp.Range.newBuilder().build())
                        .build())
                .addResponses(mvcc.v1.Mvcc.ResponseOp.newBuilder()
                        .setResponseDeleteRange(mvcc.v1.Mvcc.ResponseOp.DeleteRange.newBuilder()
                                .setDeleted(scenario.response().responses().get(1).deleted())
                                .addAllPrevKvs(kvList(scenario.response().responses().get(1).prevKvs()))
                                .build())
                        .build())
                .build();
        serviceRegistry.addService(stub);

        TxnResponse response = kvClient.txn(bytes(scenario.table()))
                .If(new Cmp(bytes(scenario.compare().key()), Cmp.Op.valueOf(scenario.compare().result()), CmpTarget.value(bytes(scenario.compare().value()))))
                .Then(Op.put(bytes(scenario.success().get(0).key()), bytes(scenario.success().get(0).value()), PutOption.builder().withPrevKV().build()))
                .Else(
                        Op.get(bytes(scenario.failure().get(0).key()), GetOption.DEFAULT),
                        Op.delete(bytes(scenario.failure().get(1).key()), DeleteOption.builder().withPrevKV(scenario.failure().get(1).prevKV()).build()))
                .commit();

        assertThat(stub.lastTxnRequest.getSuccessList()).hasSize(scenario.success().size());
        assertThat(stub.lastTxnRequest.getFailureList()).hasSize(scenario.failure().size());
        assertThat(stub.lastTxnRequest.getFailure(0).hasRequestRange()).isTrue();
        assertThat(stub.lastTxnRequest.getFailure(1).getRequestDeleteRange().getPrevKv()).isEqualTo(scenario.failure().get(1).prevKV());
        assertThat(response.succeeded()).isFalse();
        assertThat(response.responses()).hasSize(scenario.response().responses().size());
        assertThat(response.responses().get(0).hasResponseRange()).isTrue();
        assertThat(response.responses().get(1).hasResponseDeleteRange()).isTrue();
    }

    @Test
    void getUnavailableErrorScenario() {
        var scenario = fixture.scenarios().getUnavailableError();
        var stub = new KVConformanceStub();
        stub.rangeError = Status.UNAVAILABLE.asRuntimeException();
        serviceRegistry.addService(stub);

        assertThatThrownBy(() -> kvClient.get(bytes(scenario.table()), bytes(scenario.key())))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(error -> Status.fromThrowable((Throwable) error).getCode())
                .isEqualTo(Status.Code.UNAVAILABLE);
    }

    @Test
    void memberListScenario() {
        var scenario = fixture.scenarios().memberList();
        var stub = new ClusterConformanceStub();
        stub.response = MemberListResponse.newBuilder()
                .setCluster(scenario.cluster())
                .addAllMembers(scenario.members().stream()
                        .map(member -> Member.newBuilder()
                                .setId(member.id())
                                .setName(member.name())
                                .addAllPeerURLs(member.peerURLs())
                                .addAllClientURLs(member.clientURLs())
                                .build())
                        .toList())
                .build();
        serviceRegistry.addService(stub);

        var response = clusterClient.memberList();

        assertThat(response.cluster()).isEqualTo(scenario.cluster());
        assertThat(response.members()).hasSize(scenario.members().size());
        assertThat(response.members().get(0).name()).isEqualTo(scenario.members().get(0).name());
        assertThat(response.members().get(0).clientURLs()).containsExactlyElementsOf(scenario.members().get(0).clientURLs());
    }

    private static ByteSequence bytes(String value) {
        return ByteSequence.fromUtf8String(value);
    }

    private static ResponseHeader header(long revision) {
        return ResponseHeader.newBuilder().setRevision(revision).build();
    }

    private static List<mvcc.v1.Mvcc.KeyValue> kvList(List<KvPair> kvs) {
        return kvs.stream().map(CoreConformanceTest::keyValue).toList();
    }

    private static mvcc.v1.Mvcc.KeyValue keyValue(KvPair kv) {
        return mvcc.v1.Mvcc.KeyValue.newBuilder()
                .setKey(ByteString.copyFromUtf8(kv.key()))
                .setValue(ByteString.copyFromUtf8(kv.value()))
                .build();
    }

    private static final class KVConformanceStub extends KVGrpc.KVImplBase {
        private RangeRequest lastRangeRequest;
        private RangeRequest lastIterateRangeRequest;
        private PutRequest lastPutRequest;
        private DeleteRangeRequest lastDeleteRangeRequest;
        private TxnRequest lastTxnRequest;
        private RangeResponse rangeResponse = RangeResponse.getDefaultInstance();
        private List<RangeResponse> iterateResponses = List.of();
        private RuntimeException rangeError;
        private armada.v1.PutResponse putResponse = armada.v1.PutResponse.getDefaultInstance();
        private armada.v1.DeleteRangeResponse deleteResponse = armada.v1.DeleteRangeResponse.getDefaultInstance();
        private armada.v1.TxnResponse txnResponse = armada.v1.TxnResponse.getDefaultInstance();

        @Override
        public void range(RangeRequest request, StreamObserver<RangeResponse> responseObserver) {
            this.lastRangeRequest = request;
            if (rangeError != null) {
                responseObserver.onError(rangeError);
                return;
            }
            responseObserver.onNext(rangeResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void iterateRange(RangeRequest request, StreamObserver<RangeResponse> responseObserver) {
            this.lastIterateRangeRequest = request;
            for (RangeResponse response : iterateResponses) {
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void put(PutRequest request, StreamObserver<armada.v1.PutResponse> responseObserver) {
            this.lastPutRequest = request;
            responseObserver.onNext(putResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void deleteRange(DeleteRangeRequest request, StreamObserver<armada.v1.DeleteRangeResponse> responseObserver) {
            this.lastDeleteRangeRequest = request;
            responseObserver.onNext(deleteResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void txn(TxnRequest request, StreamObserver<armada.v1.TxnResponse> responseObserver) {
            this.lastTxnRequest = request;
            responseObserver.onNext(txnResponse);
            responseObserver.onCompleted();
        }
    }

    private static final class ClusterConformanceStub extends ClusterGrpc.ClusterImplBase {
        private MemberListResponse response = MemberListResponse.getDefaultInstance();

        @Override
        public void memberList(MemberListRequest request, StreamObserver<MemberListResponse> responseObserver) {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    record CoreConformanceFixture(Scenarios scenarios) {
        static CoreConformanceFixture load() throws IOException {
            try (InputStream input = CoreConformanceTest.class.getClassLoader().getResourceAsStream("core-sdk.json")) {
                if (input == null) {
                    throw new IOException("missing core-sdk.json conformance fixture");
                }
                return new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(input, CoreConformanceFixture.class);
            }
        }
    }

    record Scenarios(
            RangeScenario getDefault,
            RangeScenario getPrefixKeysOnly,
            RangeScenario getCountOnlyRange,
            IterateScenario iteratePrefix,
            PutScenario putPrevKV,
            DeleteScenario deletePrefixPrevKV,
            DeleteScenario deleteRange,
            TxnScenario txnCompareAndOps,
            TxnScenario txnFailureOps,
            ErrorScenario getUnavailableError,
            MemberListScenario memberList
    ) {
    }

    record RangeScenario(String table, String key, RangeOptions options, RangeExpectedRequest expectedRequest, RangeResponseFixture response) {
    }

    record IterateScenario(String table, String key, RangeOptions options, RangeExpectedRequest expectedRequest, List<RangeResponseFixture> responseChunks) {
    }

    record PutScenario(String table, String key, String value, PutOptions options, PutExpectedRequest expectedRequest, PutResponseFixture response) {
    }

    record DeleteScenario(String table, String key, DeleteOptions options, DeleteExpectedRequest expectedRequest, DeleteResponseFixture response) {
    }

    record TxnScenario(String table, TxnCompare compare, List<TxnOp> success, List<TxnOp> failure, TxnResponseFixture response) {
    }

    record MemberListScenario(String cluster, List<MemberFixture> members) {
    }

    record RangeOptions(String rangeEnd, boolean prefix, boolean serializable, boolean keysOnly, boolean countOnly, long limit) {
    }

    record PutOptions(boolean prevKV) {
    }

    record DeleteOptions(String rangeEnd, boolean prefix, boolean prevKV) {
    }

    record RangeExpectedRequest(String rangeEnd, long limit, boolean linearizable, boolean keysOnly, boolean countOnly) {
    }

    record PutExpectedRequest(boolean prevKV) {
    }

    record DeleteExpectedRequest(String rangeEnd, boolean prevKV) {
    }

    record HeaderFixture(long revision) {
    }

    record KvPair(String key, String value) {
    }

    record RangeResponseFixture(HeaderFixture header, List<KvPair> kvs, long count) {
    }

    record PutResponseFixture(HeaderFixture header, KvPair prevKv) {
    }

    record DeleteResponseFixture(HeaderFixture header, long deleted, List<KvPair> prevKvs) {
    }

    record TxnCompare(String key, String value, String result) {
    }

    record TxnOp(String type, String key, String value, boolean prevKV) {
    }

    record TxnResponseFixture(HeaderFixture header, boolean succeeded, List<TxnResponseOp> responses) {
    }

    record TxnResponseOp(String type, KvPair prevKv, long deleted, List<KvPair> prevKvs) {
    }

    record MemberFixture(String id, String name, List<String> peerURLs, List<String> clientURLs) {
    }

    record ErrorScenario(String table, String key, String code) {
    }
}
