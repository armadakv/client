/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core;

import io.armadakv.core.api.MemberListResponse;

public interface Cluster extends CloseableClient {
    MemberListResponse memberList();
}
