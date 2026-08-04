/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core.api;

public record PutResponse(Header header, KeyValue prev) implements Response {
}
