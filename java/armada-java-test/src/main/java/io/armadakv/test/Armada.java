/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.testcontainers.containers.Network;

import com.google.common.base.Strings;

public final class Armada {
    public static final String CONTAINER_IMAGE = "ghcr.io/armadakv/armada:master";
    public static final int ARMADA_CLIENT_PORT = 8443;
    public static final int ARMADA_PEER_PORT = 5012;
    public static final int ARMADA_METRICS_PORT = 8079;
    public static final String ARMADA_DATA_DIR = "/data";

    private Armada() {
    }

    private static String resolveContainerImage() {
        String image = System.getenv("ARMADA_IMAGE");
        if (!Strings.isNullOrEmpty(image)) {
            return image;
        }
        return CONTAINER_IMAGE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String image = Armada.resolveContainerImage();
        private String clusterName = UUID.randomUUID().toString();
        private int nodes = 1;
        private boolean ssl = false;
        private boolean debug = false;
        private List<String> additionalArgs;
        private Network network;
        private boolean shouldMountDataDirectory = true;
        private String user;

        public Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public Builder withNodes(int nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder withSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public Builder withDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder withAdditionalArgs(Collection<String> additionalArgs) {
            this.additionalArgs = Collections.unmodifiableList(new ArrayList<>(additionalArgs));
            return this;
        }

        public Builder withAdditionalArgs(String... additionalArgs) {
            this.additionalArgs = Collections.unmodifiableList(Arrays.asList(additionalArgs));
            return this;
        }

        public Builder withImage(String image) {
            this.image = image;
            return this;
        }

        public Builder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public ArmadaCluster build() {
            return new ArmadaClusterImpl(
                image,
                clusterName,
                nodes,
                ssl,
                debug,
                additionalArgs,
                network != null ? network : Network.SHARED,
                shouldMountDataDirectory,
                user);
        }

        public Builder withMountedDataDirectory(boolean shouldMountDataDirectory) {
            this.shouldMountDataDirectory = shouldMountDataDirectory;
            return this;
        }

        public Builder withUser(String user) {
            this.user = user;
            return this;
        }
    }
}
