/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core.api;

import mvcc.v1.Mvcc.ResponseOp;

import java.util.List;

public record TxnResponse(Header header, boolean succeeded, List<ResponseOp> responses) implements Response {
}
