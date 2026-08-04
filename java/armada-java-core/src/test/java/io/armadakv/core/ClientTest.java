package io.armadakv.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientTest {
    @Test
    void targetCannotBeBlank() {
        assertThatThrownBy(() -> new ClientBuilder().target(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
