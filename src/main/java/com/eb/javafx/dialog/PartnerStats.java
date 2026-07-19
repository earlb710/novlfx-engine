package com.eb.javafx.dialog;

/**
 * Engine-generic view of a conversation partner's named stats, used by
 * {@link DialogChoiceRequirement} to evaluate {@code minStats}/{@code maxStats} gates without
 * depending on any game-specific character type. The game's NPC/person type implements this
 * (e.g. {@code AltLifePerson}), mapping canonical stat names ({@code "love"}, {@code "trust"},
 * {@code "fear"}, …) to their current integer values.
 *
 * <p>Promotion note: this interface is part of the generic dialog-tree core and moves to novlfx
 * alongside {@link DialogChoiceRequirement}; the game supplies the implementation.</p>
 */
public interface PartnerStats {

    /**
     * The partner's current value for a canonical, case-insensitive stat name, or {@code null}
     * when the name is unknown to this partner (the gate then fails closed).
     */
    Integer namedStat(String statName);
}
