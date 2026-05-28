package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.util.Validation;

/**
 * Immutable record for a single row in a {@link StatusLogPanel}.
 *
 * <p>The status log is generic — it doesn't model specific game systems. An entry has
 * a free-form {@code subject} string (typically the character id, MC name, or system
 * label that "owns" the change), the human-readable {@code message} describing what
 * happened, an optional {@code category} for stylistic grouping (e.g.
 * {@code "stat"} / {@code "relationship"} / {@code "inventory"}), and a {@link GameDateTime}
 * timestamp. Host games decide what constitutes a status change and call
 * {@link StatusLogPanel#append(StatusLogEntry)} at the appropriate moments.</p>
 *
 * <p>Convenience factories ({@link #of(String, String)} and friends) are provided so the
 * common emit path stays terse at the call site:</p>
 * <pre>
 *   statusLog.append(StatusLogEntry.of("MC", "Stamina -1 → 4"));
 *   statusLog.append(StatusLogEntry.of("Anna", "Love +2 (3 → 5)", "relationship"));
 * </pre>
 *
 * <p>{@code subject} and {@code category} are normalised to empty strings when null so
 * call sites can omit either without dragging null checks through the codebase. The
 * {@code message} is required (blank is rejected) because an entry without a message
 * is meaningless. {@code occurredAt} defaults to {@code (day 1, "default")} when null
 * so the record is always usable for sorting/serialisation even if the host hasn't
 * wired a real clock.</p>
 */
public record StatusLogEntry(
        String subject,
        String message,
        String category,
        GameDateTime occurredAt
) {
    public StatusLogEntry {
        subject = subject == null ? "" : subject.trim();
        message = Validation.requireNonBlank(message, "Status log message is required.").trim();
        category = category == null ? "" : category.trim();
        occurredAt = occurredAt == null ? new GameDateTime(1, "default") : occurredAt;
    }

    /** Subject + message, no category, default timestamp. */
    public static StatusLogEntry of(String subject, String message) {
        return new StatusLogEntry(subject, message, "", null);
    }

    /** Subject + message + category, default timestamp. */
    public static StatusLogEntry of(String subject, String message, String category) {
        return new StatusLogEntry(subject, message, category, null);
    }

    /** Fully specified entry. */
    public static StatusLogEntry of(String subject, String message, String category, GameDateTime occurredAt) {
        return new StatusLogEntry(subject, message, category, occurredAt);
    }
}
