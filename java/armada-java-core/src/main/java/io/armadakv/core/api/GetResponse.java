/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core.api;

import java.util.List;

public record GetResponse(Header header, List<KeyValue> kvs, long count) implements Response {
}
