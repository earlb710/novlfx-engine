package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ScreenViewModelTest {
    @Test
    void copiesLinesAndActionsForReusableScreenContracts() {
        List<String> lines = new ArrayList<>(List.of("Ready"));
        List<ScreenActionViewModel> actions = new ArrayList<>(List.of(
                new ScreenActionViewModel("Back", "main-menu", true)));

        ScreenViewModel viewModel = new ScreenViewModel("Title", lines, actions);
        lines.add("Mutated");
        actions.clear();

        assertEquals(List.of("Ready"), viewModel.lines());
        assertEquals(1, viewModel.actions().size());
    }

    @Test
    void validatesRequiredTextForReusableScreenContracts() {
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenViewModel("", List.of("Ready"), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenViewModel("Title", List.of(" "), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenActionViewModel("Back", "", true));
    }
}
