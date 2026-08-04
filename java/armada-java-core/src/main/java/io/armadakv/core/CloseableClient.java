/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core;

public interface CloseableClient extends AutoCloseable {

    /**
     * close the client and release its resources.
     */
    @Override
    default void close() {
        // noop
    }

}
