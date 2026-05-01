package com.eb.javafx.random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GameRandomServiceTest {

    @Test
    void valuesCannotBeReadBeforeInitialization() {
        GameRandomService service = new GameRandomService();

        assertThrows(IllegalStateException.class, service::gameplaySeed);
        assertThrows(IllegalStateException.class, () -> service.nextGameplayInt(10));
        assertThrows(IllegalStateException.class, () -> service.nextUiInt(10));
    }

    @Test
    void gameplayRandomnessIsDeterministicAfterInitialization() {
        GameRandomService first = new GameRandomService();
        GameRandomService second = new GameRandomService();

        first.initialize();
        second.initialize();

        assertEquals(1L, first.gameplaySeed());
        assertEquals(first.gameplaySeed(), second.gameplaySeed());
        assertEquals(first.nextGameplayInt(1000), second.nextGameplayInt(1000));
        assertEquals(first.nextGameplayInt(1000), second.nextGameplayInt(1000));
    }
}
