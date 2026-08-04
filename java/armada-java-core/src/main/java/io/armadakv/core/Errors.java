/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core;

import io.grpc.Status;

public final class Errors {
    private Errors() {
    }

    public static boolean isRetryable(Status status) {
        return Status.UNAVAILABLE.getCode().equals(status.getCode());
    }
}
