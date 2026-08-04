// Copyright JAMF Software, LLC

package client

import (
	"context"
	"fmt"
	"net"

	armadapb "github.com/armadakv/client/go/internal/proto"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/test/bufconn"
)

// RecordedRequest holds request fields recorded by the fake client.
type RecordedRequest struct {
	Table   string
	Request Op
}

// FakeResponse holds response fields returned by the fake client.
type FakeResponse struct {
	Response OpResponse
	Err      error
}

type fakeKVServer struct {
	armadapb.UnimplementedKVServer

	recordedRequests []RecordedRequest

	fakeResponses    []FakeResponse
	fakeResponsesIdx int
}

func (f *fakeKVServer) Range(_ context.Context, req *armadapb.RangeRequest) (*armadapb.RangeResponse, error) {
	f.recordedRequests = append(f.recordedRequests, convRangeToRequest(req))
	if len(f.fakeResponses) == 0 {
		return &armadapb.RangeResponse{}, nil
	}
	i := f.fakeResponsesIdx % len(f.fakeResponses)
	f.fakeResponsesIdx++
	resp := f.fakeResponses[i]

	if resp.Err != nil {
		return nil, resp.Err
	}

	r := resp.Response.Get()
	return &armadapb.RangeResponse{
		Header: convToHeaderProto(r.Header),
		Kvs:    convKeyValuesToProto(r.Kvs),
		More:   r.More,
		Count:  r.Count,
	}, nil
}

func (f *fakeKVServer) IterateRange(req *armadapb.RangeRequest, server armadapb.KV_IterateRangeServer) error {
	f.recordedRequests = append(f.recordedRequests, convRangeToRequest(req))
	if len(f.fakeResponses) == 0 {
		return nil
	}
	i := f.fakeResponsesIdx % len(f.fakeResponses)
	f.fakeResponsesIdx++
	resp := f.fakeResponses[i]

	if resp.Err != nil {
		return resp.Err
	}

	r := resp.Response.Iterate()
	r(func(r *GetResponse, err error) bool {
		_ = server.Send(&armadapb.RangeResponse{
			Header: convToHeaderProto(r.Header),
			Kvs:    convKeyValuesToProto(r.Kvs),
			More:   r.More,
			Count:  r.Count,
		})
		return true
	})
	return nil
}

func (f *fakeKVServer) Put(_ context.Context, req *armadapb.PutRequest) (*armadapb.PutResponse, error) {
	f.recordedRequests = append(f.recordedRequests, convPutToRequest(req))
	if len(f.fakeResponses) == 0 {
		return &armadapb.PutResponse{}, nil
	}
	i := f.fakeResponsesIdx % len(f.fakeResponses)
	f.fakeResponsesIdx++
	resp := f.fakeResponses[i]

	if resp.Err != nil {
		return nil, resp.Err
	}

	r := resp.Response.Put()
	var kv *armadapb.KeyValue
	if r.PrevKv != nil {
		kv = &armadapb.KeyValue{
			Key:            r.PrevKv.Key,
			CreateRevision: r.PrevKv.CreateRevision,
			ModRevision:    r.PrevKv.ModRevision,
			Value:          r.PrevKv.Value,
		}
	}
	return &armadapb.PutResponse{
		Header: convToHeaderProto(r.Header),
		PrevKv: kv,
	}, nil
}

func (f *fakeKVServer) DeleteRange(_ context.Context, req *armadapb.DeleteRangeRequest) (*armadapb.DeleteRangeResponse, error) {
	f.recordedRequests = append(f.recordedRequests, convDeleteRangeToRequest(req))
	if len(f.fakeResponses) == 0 {
		return &armadapb.DeleteRangeResponse{}, nil
	}
	i := f.fakeResponsesIdx % len(f.fakeResponses)
	f.fakeResponsesIdx++
	resp := f.fakeResponses[i]

	if resp.Err != nil {
		return nil, resp.Err
	}

	r := resp.Response.Del()
	return &armadapb.DeleteRangeResponse{
		Header:  convToHeaderProto(r.Header),
		Deleted: r.Deleted,
		PrevKvs: convKeyValuesToProto(r.PrevKvs),
	}, nil
}

func (f *fakeKVServer) Txn(_ context.Context, req *armadapb.TxnRequest) (*armadapb.TxnResponse, error) {
	f.recordedRequests = append(f.recordedRequests, convTxnToRequest(req))
	if len(f.fakeResponses) == 0 {
		return &armadapb.TxnResponse{}, nil
	}
	i := f.fakeResponsesIdx % len(f.fakeResponses)
	f.fakeResponsesIdx++
	resp := f.fakeResponses[i]

	if resp.Err != nil {
		return nil, resp.Err
	}

	r := resp.Response.Txn()
	return &armadapb.TxnResponse{
		Header:    convToHeaderProto(r.Header),
		Succeeded: r.Succeeded,
		Responses: convResponsesToProto(r.Responses),
	}, nil
}

// FakeClient provides a fake Armada client. This client is intended to be used only in tests.
type FakeClient struct {
	listener *bufconn.Listener
	kvServer *fakeKVServer
}

// NewFake creates an instance of FakeClient, including a function for client termination.
func NewFake(fakeResponses ...FakeResponse) (*FakeClient, context.CancelFunc) {
	srv := grpc.NewServer()
	fs := &fakeKVServer{fakeResponses: fakeResponses}
	armadapb.RegisterKVServer(srv, fs)

	l := bufconn.Listen(1024)
	go srv.Serve(l)

	return &FakeClient{listener: l, kvServer: fs}, srv.Stop
}

// Client returns a new instance of Armada client targeting the fake Armada server.
func (c *FakeClient) Client() *Client {
	cl, err := NewFromURL("fake", WithDialOptions(grpc.WithTransportCredentials(insecure.NewCredentials()), grpc.WithContextDialer(func(ctx context.Context, s string) (net.Conn, error) {
		return c.listener.Dial()
	})))
	if err != nil {
		panic(err)
	}
	return cl
}

func (c *FakeClient) RecordedRequests() []RecordedRequest {
	return c.kvServer.recordedRequests
}

func convRangeToRequest(rr *armadapb.RangeRequest) RecordedRequest {
	if rr == nil {
		return RecordedRequest{}
	}

	return RecordedRequest{
		Table: string(rr.Table),
		Request: Op{
			t:            tRange,
			key:          rr.Key,
			end:          rr.RangeEnd,
			limit:        rr.Limit,
			serializable: !rr.Linearizable,
			keysOnly:     rr.KeysOnly,
			countOnly:    rr.CountOnly,
			minModRev:    rr.MinModRevision,
			maxModRev:    rr.MaxModRevision,
			minCreateRev: rr.MinCreateRevision,
			maxCreateRev: rr.MaxCreateRevision,
		},
	}
}

func convPutToRequest(pr *armadapb.PutRequest) RecordedRequest {
	if pr == nil {
		return RecordedRequest{}
	}

	return RecordedRequest{
		Table: string(pr.Table),
		Request: Op{
			t:      tPut,
			key:    pr.Key,
			val:    pr.Value,
			prevKV: pr.PrevKv,
		},
	}
}

func convDeleteRangeToRequest(drr *armadapb.DeleteRangeRequest) RecordedRequest {
	if drr == nil {
		return RecordedRequest{}
	}

	return RecordedRequest{
		Table: string(drr.Table),
		Request: Op{
			t:      tDeleteRange,
			key:    drr.Key,
			end:    drr.RangeEnd,
			prevKV: drr.PrevKv,
			count:  drr.Count,
		},
	}
}

func convTxnToRequest(tr *armadapb.TxnRequest) RecordedRequest {
	if tr == nil {
		return RecordedRequest{}
	}

	thenOps := make([]Op, len(tr.Success))
	for i, s := range tr.Success {
		thenOps[i] = convToOp(s)
	}
	elseOps := make([]Op, len(tr.Failure))
	for i, f := range tr.Failure {
		elseOps[i] = convToOp(f)
	}
	cmps := make([]Cmp, len(tr.Compare))
	for i := range tr.Compare {
		cmps[i] = Cmp{tr.Compare[i]}
	}

	return RecordedRequest{
		Table: string(tr.Table),
		Request: Op{
			t:       tTxn,
			cmps:    cmps,
			thenOps: thenOps,
			elseOps: elseOps,
		},
	}
}

func convToOp(reqOp *armadapb.RequestOp) Op {
	switch op := reqOp.Request.(type) {
	case *armadapb.RequestOp_RequestRange:
		return Op{
			t:         tRange,
			key:       op.RequestRange.Key,
			end:       op.RequestRange.RangeEnd,
			limit:     op.RequestRange.Limit,
			keysOnly:  op.RequestRange.KeysOnly,
			countOnly: op.RequestRange.CountOnly,
		}
	case *armadapb.RequestOp_RequestPut:
		return Op{
			t:      tPut,
			key:    op.RequestPut.Key,
			val:    op.RequestPut.Value,
			prevKV: op.RequestPut.PrevKv,
		}
	case *armadapb.RequestOp_RequestDeleteRange:
		return Op{
			t:      tDeleteRange,
			key:    op.RequestDeleteRange.Key,
			end:    op.RequestDeleteRange.RangeEnd,
			prevKV: op.RequestDeleteRange.PrevKv,
			count:  op.RequestDeleteRange.Count,
		}
	default:
		panic(fmt.Sprintf("unsupported transaction op type %v", op))
	}
}

func convKeyValuesToProto(in []*KeyValue) (out []*armadapb.KeyValue) {
	for _, t := range in {
		out = append(out, (*armadapb.KeyValue)(t))
	}
	return
}

func convResponsesToProto(in []*ResponseOp) (out []*armadapb.ResponseOp) {
	for _, t := range in {
		switch {
		case t.get != nil:
			out = append(out, &armadapb.ResponseOp{Response: &armadapb.ResponseOp_ResponseRange{ResponseRange: &armadapb.ResponseOp_Range{
				Kvs:   convKeyValuesToProto(t.get.Kvs),
				More:  t.get.More,
				Count: t.get.Count,
			}}})
		case t.put != nil:
			out = append(out, &armadapb.ResponseOp{Response: &armadapb.ResponseOp_ResponsePut{ResponsePut: &armadapb.ResponseOp_Put{
				PrevKv: (*armadapb.KeyValue)(t.put.PrevKv),
			}}})
		case t.delete != nil:
			out = append(out, &armadapb.ResponseOp{Response: &armadapb.ResponseOp_ResponseDeleteRange{ResponseDeleteRange: &armadapb.ResponseOp_DeleteRange{
				Deleted: t.delete.Deleted,
				PrevKvs: convKeyValuesToProto(t.delete.PrevKvs),
			}}})
		}
	}
	return
}

func convToHeaderProto(rh *ResponseHeader) *armadapb.ResponseHeader {
	if rh == nil {
		return nil
	}

	return &armadapb.ResponseHeader{
		ShardId:      rh.ShardId,
		ReplicaId:    rh.ReplicaId,
		Revision:     rh.Revision,
		RaftTerm:     rh.RaftTerm,
		RaftLeaderId: rh.RaftLeaderId,
	}
}
