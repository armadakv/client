// Copyright JAMF Software, LLC

package client

import (
	"context"
	"fmt"

	"github.com/armadakv/client/go/internal/proto"
	"google.golang.org/grpc"
)

type (
	Member             armadapb.Member
	MemberListResponse armadapb.MemberListResponse
	TableStatus        armadapb.TableStatus
	// StatusResponse represents response from Status API.
	StatusResponse struct {
		// Id is the member ID of this member.
		Id string
		// Version is the semver version used by the responding member.
		Version string
		// Info is the additional server info.
		Info string
		// Tables is a status of tables of the responding member.
		Tables map[string]*TableStatus
		// Errors contains alarm/health information and status.
		Errors []string
		// Config contains the member configuration.
		Config map[string]any
	}
)

type Cluster interface {
	// MemberList lists the current cluster membership.
	MemberList(ctx context.Context) (*MemberListResponse, error)
	// Status gets the status of the endpoint.
	Status(ctx context.Context, endpoint string) (*StatusResponse, error)
}

type cluster struct {
	remote   armadapb.ClusterClient
	dial     func(endpoint string) (armadapb.ClusterClient, func(), error)
	callOpts []grpc.CallOption
}

func newCluster(c *Client) Cluster {
	api := &cluster{
		remote:   &retryClusterClient{cc: armadapb.NewClusterClient(c.conn)},
		callOpts: c.callOpts,
		dial: func(endpoint string) (armadapb.ClusterClient, func(), error) {
			conn, err := c.Dial(endpoint)
			if err != nil {
				return nil, nil, fmt.Errorf("failed to dial endpoint %s with maintenance client: %v", endpoint, err)
			}

			cancel := func() { conn.Close() }
			return &retryClusterClient{cc: armadapb.NewClusterClient(c.conn)}, cancel, nil
		},
	}
	return api
}

func NewClusterFromClusterClient(remote armadapb.ClusterClient, c *Client) Cluster {
	api := &cluster{remote: remote}
	if c != nil {
		api.callOpts = c.callOpts
	}
	return api
}

func (c *cluster) MemberList(ctx context.Context) (*MemberListResponse, error) {
	resp, err := c.remote.MemberList(ctx, &armadapb.MemberListRequest{}, append(c.callOpts, withRepeatable())...)
	if err == nil {
		return (*MemberListResponse)(resp), nil
	}
	return nil, toErr(ctx, err)
}

func (c *cluster) Status(ctx context.Context, endpoint string) (*StatusResponse, error) {
	remote, cancel, err := c.dial(endpoint)
	if err != nil {
		return nil, toErr(ctx, err)
	}
	defer cancel()
	resp, err := remote.Status(ctx, &armadapb.StatusRequest{Config: true}, append(c.callOpts, withRepeatable())...)
	if err != nil {
		return nil, toErr(ctx, err)
	}
	return mapProtoStatusResponse(resp), nil
}

func mapProtoStatusResponse(resp *armadapb.StatusResponse) *StatusResponse {
	ts := make(map[string]*TableStatus, len(resp.Tables))

	for k, v := range resp.Tables {
		ts[k] = (*TableStatus)(v)
	}

	return &StatusResponse{
		Id:      resp.Id,
		Version: resp.Version,
		Info:    resp.Info,
		Tables:  ts,
		Errors:  resp.Errors,
		Config:  resp.Config.AsMap(),
	}
}

type retryClusterClient struct {
	cc armadapb.ClusterClient
}

func (r *retryClusterClient) MemberList(ctx context.Context, in *armadapb.MemberListRequest, opts ...grpc.CallOption) (resp *armadapb.MemberListResponse, err error) {
	return r.cc.MemberList(ctx, in, append(opts, withRepeatable())...)
}

func (r *retryClusterClient) Status(ctx context.Context, in *armadapb.StatusRequest, opts ...grpc.CallOption) (*armadapb.StatusResponse, error) {
	return r.cc.Status(ctx, in, append(opts, withRepeatable())...)
}
