package com.eb.javafx.testscreen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManagementApplicationTest {
    @Test
    void managementLauncherListsAuthoringScreens() {
        assertEquals(List.of("Default App Values", "Screen Designer", "Reloadable JSON Screen",
                        "Conversation Editor", "Manage Code Tables"),
                ManagementApplication.managementActionLabels());
    }

    @Test
    void managementActionsHaveDescriptionsAndLaunchers() {
        assertTrue(ManagementApplication.managementActions().stream()
                .allMatch(action -> !action.description().isBlank() && action.launcher() != null));
    }
}
