// Copyright JAMF Software, LLC

package client

import (
	"context"
	"encoding/json"

	"github.com/armadakv/client/go/internal/proto"
	"google.golang.org/grpc"
)

type (
	CreateTableResponse armadapb.CreateTableResponse
	DeleteTableResponse armadapb.DeleteTableResponse
	ListTablesResponse  struct {
		Tables []TableInfo
	}
	TableInfo struct {
		ID     string
		Name   string
		Config map[string]any
	}
)

func (resp *CreateTableResponse) String() string {
	enc, _ := json.Marshal(resp)
	return string(enc)
}

func (resp *ListTablesResponse) String() string {
	enc, _ := json.Marshal(resp)
	return string(enc)
}

type Tables interface {
	// CreateTable creates table with specified name.
	CreateTable(ctx context.Context, name string) (*CreateTableResponse, error)
	// DeleteTable gets the status of the endpoint.
	DeleteTable(ctx context.Context, name string) (*DeleteTableResponse, error)
	// ListTables lists all currently registered tables.
	ListTables(ctx context.Context) (*ListTablesResponse, error)
}

type tables struct {
	remote   armadapb.TablesClient
	callOpts []grpc.CallOption
}

func newTables(c *Client) Tables {
	api := &tables{
		remote:   &retryTablesClient{client: armadapb.NewTablesClient(c.conn)},
		callOpts: c.callOpts,
	}
	return api
}

func NewTablesFromTablesClient(remote armadapb.TablesClient, c *Client) Tables {
	api := &tables{remote: &retryTablesClient{remote}}
	if c != nil {
		api.callOpts = c.callOpts
	}
	return api
}

func (t *tables) CreateTable(ctx context.Context, name string) (*CreateTableResponse, error) {
	resp, err := t.remote.Create(ctx, &armadapb.CreateTableRequest{Name: name}, t.callOpts...)
	if err == nil {
		return (*CreateTableResponse)(resp), nil
	}
	return nil, toErr(ctx, err)
}

func (t *tables) DeleteTable(ctx context.Context, name string) (*DeleteTableResponse, error) {
	resp, err := t.remote.Delete(ctx, &armadapb.DeleteTableRequest{Name: name}, t.callOpts...)
	if err == nil {
		return (*DeleteTableResponse)(resp), nil
	}
	return nil, toErr(ctx, err)
}

func (t *tables) ListTables(ctx context.Context) (*ListTablesResponse, error) {
	resp, err := t.remote.List(ctx, &armadapb.ListTablesRequest{}, append(t.callOpts, withRepeatable())...)
	if err == nil {
		return mapProtoListTablesResponse(resp), nil
	}
	return nil, toErr(ctx, err)
}

func mapProtoListTablesResponse(resp *armadapb.ListTablesResponse) *ListTablesResponse {
	ts := make([]TableInfo, len(resp.Tables))
	for k, v := range resp.Tables {
		ts[k] = TableInfo{
			ID:     v.Id,
			Name:   v.Name,
			Config: v.Config.AsMap(),
		}
	}
	return &ListTablesResponse{Tables: ts}
}

type retryTablesClient struct {
	client armadapb.TablesClient
}

func (r *retryTablesClient) Create(ctx context.Context, in *armadapb.CreateTableRequest, opts ...grpc.CallOption) (*armadapb.CreateTableResponse, error) {
	return r.client.Create(ctx, in, opts...)
}

func (r *retryTablesClient) Delete(ctx context.Context, in *armadapb.DeleteTableRequest, opts ...grpc.CallOption) (*armadapb.DeleteTableResponse, error) {
	return r.client.Delete(ctx, in, opts...)
}

func (r *retryTablesClient) List(ctx context.Context, in *armadapb.ListTablesRequest, opts ...grpc.CallOption) (*armadapb.ListTablesResponse, error) {
	return r.client.List(ctx, in, append(opts, withRepeatable())...)
}
