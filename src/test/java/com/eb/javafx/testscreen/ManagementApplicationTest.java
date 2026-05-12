package com.eb.javafx.testscreen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManagementApplicationTest {
    @Test
    void managementActionsHaveDescriptionsAndLaunchers() {
        assertTrue(ManagementApplication.managementActions().stream()
                .allMatch(action -> !action.description().isBlank() && action.launcher() != null));
    }
}
