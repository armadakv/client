/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.armadakv.core.Client;
import io.armadakv.core.api.ByteSequence;
import io.armadakv.core.api.TableInfo;
import io.armadakv.core.api.TxnResponse;
import io.armadakv.core.api.op.Cmp;
import io.armadakv.core.api.op.CmpTarget;
import io.armadakv.core.api.op.Op;
import io.armadakv.core.options.GetOption;
import io.armadakv.test.Armada;
import io.armadakv.test.ArmadaCluster;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoreBlackboxConformanceTest {

    private static final String DEFAULT_IMAGE = "ghcr.io/armadakv/armada:master";

    private BlackboxFixture fixture;
    private ArmadaCluster cluster;
    private Client client;

    @BeforeAll
    void setUp() throws IOException, SSLException {
        //Assumptions.assumeTrue("1".equals(System.getenv("ARMADA_BLACKBOX")), "black-box conformance disabled");
        //Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "docker unavailable");

        fixture = BlackboxFixture.load();
        String image = System.getenv("ARMADA_IMAGE");
        if (image == null || image.isBlank()) {
            image = fixture.image() != null && !fixture.image().isBlank() ? fixture.image() : DEFAULT_IMAGE;
        }

        cluster = Armada.builder()
                .withNodes(1)
                .withMountedDataDirectory(false)
                .withImage(image)
                .build();
        cluster.start();

        URI endpoint = cluster.clientEndpoints().get(0);
        client = Client.builder()
                .negotiationType(NegotiationType.PLAINTEXT)
                .target("http://" + endpoint.getHost() + ":" + endpoint.getPort())
                .build();

        waitUntilReady();
    }

    @AfterAll
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        if (cluster != null) {
            cluster.close();
        }
    }

    @Test
    void tableLifecycleScenario() {
        var scenario = fixture.scenarios().tableLifecycle();
        client.getTablesClient().createTable(scenario.table());

        List<String> names = client.getTablesClient().listTables().tables().stream().map(TableInfo::name).toList();
        assertThat(names).contains(scenario.table());

        client.getTablesClient().deleteTable(scenario.table());
        names = client.getTablesClient().listTables().tables().stream().map(TableInfo::name).toList();
        assertThat(names).doesNotContain(scenario.table());
    }

    @Test
    void putGetScenario() {
        var scenario = fixture.scenarios().putGet();
        createTable(scenario.table());

        client.getKVClient().put(bytes(scenario.table()), bytes(scenario.key()), bytes(scenario.value()));
        var response = client.getKVClient().get(bytes(scenario.table()), bytes(scenario.key()));

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.kvs()).hasSize(1);
        assertThat(response.kvs().get(0).value().toString()).isEqualTo(scenario.value());
    }

    @Test
    void iteratePrefixScenario() {
        var scenario = fixture.scenarios().iteratePrefix();
        createTable(scenario.table());
        seedTable(scenario.table(), scenario.seed());

        List<String> keys = client.getKVClient()
                .iterate(bytes(scenario.table()), bytes(scenario.prefix()), GetOption.builder().isPrefix(true).build())
                .flatMap(response -> response.kvs().stream())
                .map(kv -> kv.key().toString())
                .sorted()
                .toList();

        assertThat(keys).containsExactlyElementsOf(scenario.expectedKeys().stream().sorted().toList());
    }

    @Test
    void deletePrefixScenario() {
        var scenario = fixture.scenarios().deletePrefix();
        createTable(scenario.table());
        seedTable(scenario.table(), scenario.seed());

        var response = client.getKVClient().delete(
                bytes(scenario.table()),
                bytes(scenario.prefix()),
                io.armadakv.core.options.DeleteOption.builder().isPrefix(true).withCount(true).build()
        );

        assertThat(response.deleted()).isEqualTo(scenario.expectedDeleted());

        List<String> keys = client.getKVClient()
                .get(bytes(scenario.table()), ByteSequence.EMPTY, GetOption.builder().withRange(ByteSequence.from(new byte[]{0})).build())
                .kvs().stream()
                .map(kv -> kv.key().toString())
                .sorted()
                .toList();

        assertThat(keys).containsExactlyElementsOf(scenario.remainingKeys().stream().sorted().toList());
    }

    @Test
    void txnSuccessScenario() {
        runTxnScenario(fixture.scenarios().txnSuccess());
    }

    @Test
    void txnFailureScenario() {
        runTxnScenario(fixture.scenarios().txnFailure());
    }

    @Test
    void memberListScenario() {
        var scenario = fixture.scenarios().memberList();
        var response = client.getClusterClient().memberList();
        assertThat(response.members()).hasSizeGreaterThanOrEqualTo(scenario.minMembers());
    }

    private void runTxnScenario(BlackboxTxnScenario scenario) {
        createTable(scenario.table());
        seedTable(scenario.table(), scenario.seed());

        TxnResponse response = client.getKVClient().txn(bytes(scenario.table()))
                .If(new Cmp(bytes(scenario.compare().key()), Cmp.Op.EQUAL, CmpTarget.value(bytes(scenario.compare().value()))))
                .Then(scenario.successPuts().stream().map(put -> Op.put(bytes(put.key()), bytes(put.value()), io.armadakv.core.options.PutOption.DEFAULT)).toArray(Op[]::new))
                .Else(scenario.failurePuts().stream().map(put -> Op.put(bytes(put.key()), bytes(put.value()), io.armadakv.core.options.PutOption.DEFAULT)).toArray(Op[]::new))
                .commit();

        assertThat(response.succeeded()).isEqualTo(scenario.expectedSucceeded());
        assertThat(response.responses()).hasSize(scenario.expectedResponseCount());

        List<BlackboxSeedKV> expectedPuts = scenario.expectedSucceeded() ? scenario.successPuts() : scenario.failurePuts();
        for (BlackboxSeedKV expected : expectedPuts) {
            var get = client.getKVClient().get(bytes(scenario.table()), bytes(expected.key()));
            assertThat(get.kvs()).hasSize(1);
            assertThat(get.kvs().get(0).value().toString()).isEqualTo(expected.value());
        }

        if (!scenario.expectedSucceeded() && !scenario.successPuts().isEmpty()) {
            var preserved = client.getKVClient().get(bytes(scenario.table()), bytes(scenario.successPuts().get(0).key()));
            assertThat(preserved.kvs()).hasSize(1);
            assertThat(preserved.kvs().get(0).value().toString()).isEqualTo(scenario.seed().get(0).value());
        }
    }

    private void createTable(String table) {
        client.getTablesClient().createTable(table);
    }

    private void waitUntilReady() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (true) {
            try {
                client.getTablesClient().listTables();
                return;
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() != Status.Code.FAILED_PRECONDITION || System.nanoTime() > deadline) {
                    throw e;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(interruptedException);
                }
            }
        }
    }

    private void seedTable(String table, List<BlackboxSeedKV> seed) {
        for (BlackboxSeedKV kv : seed) {
            client.getKVClient().put(bytes(table), bytes(kv.key()), bytes(kv.value()));
        }
    }

    private static ByteSequence bytes(String value) {
        return ByteSequence.fromUtf8String(value);
    }

    record BlackboxFixture(String version, String scope, String image, BlackboxScenarios scenarios) {
        static BlackboxFixture load() throws IOException {
            try (InputStream input = CoreBlackboxConformanceTest.class.getClassLoader().getResourceAsStream("blackbox-core.json")) {
                if (input == null) {
                    throw new IOException("missing blackbox-core.json fixture");
                }
                return new ObjectMapper().readValue(input, BlackboxFixture.class);
            }
        }
    }

    record BlackboxScenarios(
            BlackboxTableLifecycle tableLifecycle,
            BlackboxPutGet putGet,
            BlackboxIteratePrefix iteratePrefix,
            BlackboxDeletePrefix deletePrefix,
            BlackboxTxnScenario txnSuccess,
            BlackboxTxnScenario txnFailure,
            BlackboxMemberList memberList
    ) {
    }

    record BlackboxTableLifecycle(String table) {
    }

    record BlackboxPutGet(String table, String key, String value) {
    }

    record BlackboxSeedKV(String key, String value) {
    }

    record BlackboxIteratePrefix(String table, List<BlackboxSeedKV> seed, String prefix, List<String> expectedKeys) {
    }

    record BlackboxDeletePrefix(String table, List<BlackboxSeedKV> seed, String prefix, long expectedDeleted,
                                List<String> remainingKeys) {
    }

    record BlackboxTxnCompare(String key, String value) {
    }

    record BlackboxTxnScenario(
            String table,
            List<BlackboxSeedKV> seed,
            BlackboxTxnCompare compare,
            List<BlackboxSeedKV> successPuts,
            List<BlackboxSeedKV> failurePuts,
            boolean expectedSucceeded,
            int expectedResponseCount
    ) {
    }

    record BlackboxMemberList(int minMembers) {
    }
}
