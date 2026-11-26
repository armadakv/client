package client

import (
	"context"
	"encoding/json"
	"errors"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"slices"
	"testing"
	"time"

	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"
)

const defaultArmadaBlackboxImage = "ghcr.io/armadakv/armada:v0.3.2"
const defaultArmadaBlackboxPlatform = "linux/amd64"

type blackboxFixture struct {
	Image     string            `json:"image"`
	Scenarios blackboxScenarios `json:"scenarios"`
}

type blackboxScenarios struct {
	TableLifecycle blackboxTableLifecycle `json:"tableLifecycle"`
	PutGet         blackboxPutGet         `json:"putGet"`
	IteratePrefix  blackboxIteratePrefix  `json:"iteratePrefix"`
	RevFilters     blackboxRevFilters     `json:"revisionFilters"`
	DeletePrefix   blackboxDeletePrefix   `json:"deletePrefix"`
	TxnSuccess     blackboxTxnScenario    `json:"txnSuccess"`
	TxnFailure     blackboxTxnScenario    `json:"txnFailure"`
	MemberList     blackboxMemberList     `json:"memberList"`
}

type blackboxTableLifecycle struct {
	Table string `json:"table"`
}

type blackboxPutGet struct {
	Table string `json:"table"`
	Key   string `json:"key"`
	Value string `json:"value"`
}

type blackboxSeedKV struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

type blackboxIteratePrefix struct {
	Table        string           `json:"table"`
	Seed         []blackboxSeedKV `json:"seed"`
	Prefix       string           `json:"prefix"`
	ExpectedKeys []string         `json:"expectedKeys"`
}

// blackboxRevFilters seeds keys, takes the highest mod revision among them as
// the watermark, applies changes, and expects each revision filter, relative to
// the watermark, to return the listed keys under prefix.
type blackboxRevFilters struct {
	Table    string           `json:"table"`
	Seed     []blackboxSeedKV `json:"seed"`
	Changes  []blackboxSeedKV `json:"changes"`
	Prefix   string           `json:"prefix"`
	Expected struct {
		MinModRevisionAfterSeed    []string `json:"minModRevisionAfterSeed"`
		MaxModRevisionAtSeed       []string `json:"maxModRevisionAtSeed"`
		MinCreateRevisionAfterSeed []string `json:"minCreateRevisionAfterSeed"`
		MaxCreateRevisionAtSeed    []string `json:"maxCreateRevisionAtSeed"`
	} `json:"expected"`
}

type blackboxDeletePrefix struct {
	Table           string           `json:"table"`
	Seed            []blackboxSeedKV `json:"seed"`
	Prefix          string           `json:"prefix"`
	ExpectedDeleted int64            `json:"expectedDeleted"`
	RemainingKeys   []string         `json:"remainingKeys"`
}

type blackboxTxnCompare struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

type blackboxTxnScenario struct {
	Table                 string             `json:"table"`
	Seed                  []blackboxSeedKV   `json:"seed"`
	Compare               blackboxTxnCompare `json:"compare"`
	SuccessPuts           []blackboxSeedKV   `json:"successPuts"`
	FailurePuts           []blackboxSeedKV   `json:"failurePuts"`
	ExpectedSucceeded     bool               `json:"expectedSucceeded"`
	ExpectedResponseCount int                `json:"expectedResponseCount"`
}

type blackboxMemberList struct {
	MinMembers int `json:"minMembers"`
}

func TestCoreBlackboxConformance(t *testing.T) {
	if os.Getenv("ARMADA_BLACKBOX") != "1" {
    	t.Skip("set ARMADA_BLACKBOX=1 to enable black-box conformance tests")
	}
	if _, err := exec.LookPath("docker"); err != nil {
		t.Skip("docker binary unavailable")
	}

	ctx := context.Background()
	fixture := loadBlackboxFixture(t)
	image := os.Getenv("ARMADA_IMAGE")
	if image == "" {
		image = fixture.Image
	}
	if image == "" {
		image = defaultArmadaBlackboxImage
	}
	platform := os.Getenv("ARMADA_IMAGE_PLATFORM")
	if platform == "" {
		platform = defaultArmadaBlackboxPlatform
	}

	_, endpoint, cleanup := startArmadaBlackboxContainer(ctx, t, image, platform)
	defer cleanup()

	client, err := New(
		WithEndpoints(endpoint),
		WithInitialConnectionTimeout(30*time.Second),
	)
	if err != nil {
		t.Fatalf("failed to create blackbox client: %v", err)
	}
	defer client.Close()
	//waitForBlackboxReady(t, ctx, client)

	t.Run("table lifecycle", func(t *testing.T) {
		scenario := fixture.Scenarios.TableLifecycle
		if _, err := client.CreateTable(ctx, scenario.Table); err != nil {
			t.Fatalf("create table failed: %v", err)
		}

		tables, err := client.ListTables(ctx)
		if err != nil {
			t.Fatalf("list tables failed: %v", err)
		}
		if !containsTable(tables, scenario.Table) {
			t.Fatalf("expected table %q in list", scenario.Table)
		}

		if _, err := client.DeleteTable(ctx, scenario.Table); err != nil {
			t.Fatalf("delete table failed: %v", err)
		}
		tables, err = client.ListTables(ctx)
		if err != nil {
			t.Fatalf("list tables after delete failed: %v", err)
		}
		if containsTable(tables, scenario.Table) {
			t.Fatalf("table %q still present after delete", scenario.Table)
		}
	})

	t.Run("put get", func(t *testing.T) {
		scenario := fixture.Scenarios.PutGet
		mustCreateTable(t, ctx, client, scenario.Table)

		table := client.Table(scenario.Table)
		if _, err := table.Put(ctx, scenario.Key, scenario.Value); err != nil {
			t.Fatalf("put failed: %v", err)
		}
		resp, err := table.Get(ctx, scenario.Key)
		if err != nil {
			t.Fatalf("get failed: %v", err)
		}
		if resp.Count != 1 || len(resp.Kvs) != 1 {
			t.Fatalf("unexpected get result count: %+v", resp)
		}
		if string(resp.Kvs[0].Value) != scenario.Value {
			t.Fatalf("unexpected get value: got %q want %q", string(resp.Kvs[0].Value), scenario.Value)
		}
	})

	t.Run("iterate prefix", func(t *testing.T) {
		scenario := fixture.Scenarios.IteratePrefix
		mustCreateTable(t, ctx, client, scenario.Table)
		seedGoTable(t, ctx, client.Table(scenario.Table), scenario.Seed)

		iter, err := client.Table(scenario.Table).Iterate(ctx, scenario.Prefix, WithPrefix())
		if err != nil {
			t.Fatalf("iterate failed: %v", err)
		}

		var keys []string
		iter(func(response *GetResponse, err error) bool {
			if err != nil {
				t.Fatalf("iterate callback failed: %v", err)
			}
			for _, kv := range response.Kvs {
				keys = append(keys, string(kv.Key))
			}
			return true
		})

		slices.Sort(keys)
		expected := slices.Clone(scenario.ExpectedKeys)
		slices.Sort(expected)
		if !slices.Equal(keys, expected) {
			t.Fatalf("unexpected iterate keys: got %v want %v", keys, expected)
		}
	})

	t.Run("revision filters", func(t *testing.T) {
		scenario := fixture.Scenarios.RevFilters
		mustCreateTable(t, ctx, client, scenario.Table)
		table := client.Table(scenario.Table)
		seedGoTable(t, ctx, table, scenario.Seed)

		seeded, err := table.Get(ctx, "", WithFromKey())
		if err != nil {
			t.Fatalf("get seeded keys failed: %v", err)
		}
		var watermark int64
		for _, kv := range seeded.Kvs {
			watermark = max(watermark, kv.ModRevision)
		}
		if watermark == 0 {
			t.Fatalf("seeded keys carry no mod revision: %+v", seeded)
		}
		seedGoTable(t, ctx, table, scenario.Changes)

		keysWith := func(name string, opt OpOption) []string {
			t.Helper()
			resp, err := table.Get(ctx, scenario.Prefix, WithPrefix(), opt)
			if err != nil {
				t.Fatalf("%s: get failed: %v", name, err)
			}
			var keys []string
			for _, kv := range resp.Kvs {
				keys = append(keys, string(kv.Key))
			}
			return keys
		}
		for _, c := range []struct {
			name string
			opt  OpOption
			want []string
		}{
			{"min mod revision after seed", WithMinModRev(watermark + 1), scenario.Expected.MinModRevisionAfterSeed},
			{"max mod revision at seed", WithMaxModRev(watermark), scenario.Expected.MaxModRevisionAtSeed},
			{"min create revision after seed", WithMinCreateRev(watermark + 1), scenario.Expected.MinCreateRevisionAfterSeed},
			{"max create revision at seed", WithMaxCreateRev(watermark), scenario.Expected.MaxCreateRevisionAtSeed},
		} {
			if got := keysWith(c.name, c.opt); !slices.Equal(got, c.want) {
				t.Fatalf("%s: got keys %v, want %v", c.name, got, c.want)
			}
		}

		iter, err := table.Iterate(ctx, scenario.Prefix, WithPrefix(), WithMinModRev(watermark+1))
		if err != nil {
			t.Fatalf("iterate with min mod revision failed: %v", err)
		}
		var iterated []string
		iter(func(response *GetResponse, err error) bool {
			if err != nil {
				t.Fatalf("iterate callback failed: %v", err)
			}
			for _, kv := range response.Kvs {
				iterated = append(iterated, string(kv.Key))
			}
			return true
		})
		if !slices.Equal(iterated, scenario.Expected.MinModRevisionAfterSeed) {
			t.Fatalf("iterate min mod revision: got keys %v, want %v", iterated, scenario.Expected.MinModRevisionAfterSeed)
		}
	})

	t.Run("delete prefix", func(t *testing.T) {
		scenario := fixture.Scenarios.DeletePrefix
		mustCreateTable(t, ctx, client, scenario.Table)
		table := client.Table(scenario.Table)
		seedGoTable(t, ctx, table, scenario.Seed)

		resp, err := table.Delete(ctx, scenario.Prefix, WithPrefix(), WithCount())
		if err != nil {
			t.Fatalf("delete prefix failed: %v", err)
		}
		if resp.Deleted != scenario.ExpectedDeleted {
			t.Fatalf("unexpected deleted count: got %d want %d", resp.Deleted, scenario.ExpectedDeleted)
		}

		getResp, err := table.Get(ctx, "", WithFromKey())
		if err != nil {
			t.Fatalf("get remaining keys failed: %v", err)
		}
		var keys []string
		for _, kv := range getResp.Kvs {
			keys = append(keys, string(kv.Key))
		}
		slices.Sort(keys)
		expected := slices.Clone(scenario.RemainingKeys)
		slices.Sort(expected)
		if !slices.Equal(keys, expected) {
			t.Fatalf("unexpected remaining keys: got %v want %v", keys, expected)
		}
	})

	t.Run("txn success", func(t *testing.T) {
		runGoBlackboxTxnScenario(t, ctx, client, fixture.Scenarios.TxnSuccess)
	})

	t.Run("txn failure", func(t *testing.T) {
		runGoBlackboxTxnScenario(t, ctx, client, fixture.Scenarios.TxnFailure)
	})

	t.Run("member list", func(t *testing.T) {
		scenario := fixture.Scenarios.MemberList
		resp, err := client.MemberList(ctx)
		if err != nil {
			t.Fatalf("member list failed: %v", err)
		}
		if len(resp.Members) < scenario.MinMembers {
			t.Fatalf("unexpected member count: got %d want >= %d", len(resp.Members), scenario.MinMembers)
		}
	})
}

func runGoBlackboxTxnScenario(t *testing.T, ctx context.Context, client *Client, scenario blackboxTxnScenario) {
	t.Helper()
	mustCreateTable(t, ctx, client, scenario.Table)
	table := client.Table(scenario.Table)
	seedGoTable(t, ctx, table, scenario.Seed)

	txn := table.Txn(ctx).If(Compare(Value(scenario.Compare.Key), "=", scenario.Compare.Value))
	successOps := make([]Op, 0, len(scenario.SuccessPuts))
	for _, put := range scenario.SuccessPuts {
		successOps = append(successOps, OpPut(put.Key, put.Value))
	}
	failureOps := make([]Op, 0, len(scenario.FailurePuts))
	for _, put := range scenario.FailurePuts {
		failureOps = append(failureOps, OpPut(put.Key, put.Value))
	}

	resp, err := txn.Then(successOps...).Else(failureOps...).Commit()
	if err != nil {
		t.Fatalf("txn failed: %v", err)
	}
	if resp.Succeeded != scenario.ExpectedSucceeded {
		t.Fatalf("unexpected txn succeeded: got %v want %v", resp.Succeeded, scenario.ExpectedSucceeded)
	}
	if len(resp.Responses) != scenario.ExpectedResponseCount {
		t.Fatalf("unexpected txn response count: got %d want %d", len(resp.Responses), scenario.ExpectedResponseCount)
	}

	checks := scenario.FailurePuts
	if scenario.ExpectedSucceeded {
		checks = scenario.SuccessPuts
	}
	for _, expected := range checks {
		getResp, err := table.Get(ctx, expected.Key)
		if err != nil {
			t.Fatalf("get after txn failed: %v", err)
		}
		if len(getResp.Kvs) != 1 || string(getResp.Kvs[0].Value) != expected.Value {
			t.Fatalf("unexpected value for key %q after txn", expected.Key)
		}
	}
	if !scenario.ExpectedSucceeded && len(scenario.SuccessPuts) > 0 {
		getResp, err := table.Get(ctx, scenario.SuccessPuts[0].Key)
		if err != nil {
			t.Fatalf("get preserved key failed: %v", err)
		}
		if len(getResp.Kvs) != 1 || string(getResp.Kvs[0].Value) != scenario.Seed[0].Value {
			t.Fatalf("success branch should not have applied")
		}
	}
}

func startArmadaBlackboxContainer(ctx context.Context, t *testing.T, image, platform string) (testcontainers.Container, string, func()) {
	t.Helper()

	req := testcontainers.ContainerRequest{
		Image:         image,
		ImagePlatform: platform,
		ExposedPorts: []string{
			"8443/tcp",
			"8079/tcp",
			"5012/tcp",
		},
		Cmd: []string{
			"leader",
			"--dev-mode",
			"--memberlist.cluster-name", "blackbox-conformance",
			"--memberlist.node-name", "armada-1",
			"--api.address", "http://0.0.0.0:8443",
			"--api.advertise-address", "http://0.0.0.0:8443",
			"--rest.address", "http://0.0.0.0:8079",
			"--raft.state-machine-dir", "/tmp/stable",
			"--raft.node-host-dir", "/tmp/raft",
			"--raft.address", "127.0.0.1:5012",
			"--raft.initial-members", "127.0.0.1:5012",
			"--tables.names=default",
		},
		WaitingFor: wait.ForLog("raft: storage.leaderUpdated {ShardID:1000 ReplicaID:1 Term:[0-9]+ LeaderID:1}").AsRegexp().WithStartupTimeout(90 * time.Second),
		// WaitingFor: wait.ForHTTP("/healthz").WithPort("8079/tcp").WithStatusCodeMatcher(func(status int) bool {
		// return status == 200
		// }).WithStartupTimeout(90 * time.Second),
	}

	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: req,
		Started:          true,
	})
	if err != nil {
		t.Fatalf("failed to start Armada container: %v", err)
	}

	host, err := container.Host(ctx)
	if err != nil {
		t.Fatalf("failed to resolve Armada host: %v", err)
	}
	port, err := container.MappedPort(ctx, "8443/tcp")
	if err != nil {
		t.Fatalf("failed to resolve Armada port: %v", err)
	}

	endpoint := net.JoinHostPort(host, port.Port())
	cleanup := func() {
		if err := container.Terminate(context.Background()); err != nil && !errors.Is(err, context.Canceled) {
			t.Fatalf("failed to terminate Armada container: %v", err)
		}
	}

	return container, endpoint, cleanup
}

type logConsumerFunc func(log testcontainers.Log)

func (f logConsumerFunc) Accept(log testcontainers.Log) {
	f(log)
}

func loadBlackboxFixture(t *testing.T) blackboxFixture {
	t.Helper()

	_, currentFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("failed to resolve test file path")
	}

	fixturePath := filepath.Join(filepath.Dir(currentFile), "..", "conformance", "fixtures", "blackbox-core.json")
	data, err := os.ReadFile(fixturePath)
	if err != nil {
		t.Fatalf("failed to read blackbox fixture: %v", err)
	}

	var fixture blackboxFixture
	if err := json.Unmarshal(data, &fixture); err != nil {
		t.Fatalf("failed to decode blackbox fixture: %v", err)
	}
	return fixture
}

func mustCreateTable(t *testing.T, ctx context.Context, client *Client, table string) {
	t.Helper()
	if _, err := client.CreateTable(ctx, table); err != nil {
		t.Fatalf("create table %q failed: %v", table, err)
	}
}

func waitForBlackboxReady(t *testing.T, ctx context.Context, client *Client) {
	t.Helper()

	deadline := time.Now().Add(60 * time.Second)
	for {
		r, err := client.Get(ctx, "default", "", WithFromKey(), WithLimit(1))
		if err == nil && len(r.Kvs) > 0 {
			return
		}
		if time.Now().After(deadline) {
			t.Fatalf("armada did not become ready: %v", err)
		}
		time.Sleep(500 * time.Millisecond)
	}
}

func seedGoTable(t *testing.T, ctx context.Context, table Table, seed []blackboxSeedKV) {
	t.Helper()
	for _, kv := range seed {
		if _, err := table.Put(ctx, kv.Key, kv.Value); err != nil {
			t.Fatalf("seed put for key %q failed: %v", kv.Key, err)
		}
	}
}

func containsTable(resp *ListTablesResponse, table string) bool {
	for _, info := range resp.Tables {
		if info.Name == table {
			return true
		}
	}
	return false
}
