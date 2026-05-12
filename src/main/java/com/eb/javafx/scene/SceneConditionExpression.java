package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed condition expression for a CONDITIONAL scene step.
 *
 * <p>Supported forms:
 * <ul>
 *   <li>{@code flag:id} — checks ProgressTracker.hasFlag(id)</li>
 *   <li>{@code !flag:id} — negated flag check</li>
 *   <li>{@code unlock:id} — checks ProgressTracker.isUnlocked(id)</li>
 *   <li>{@code !unlock:id} — negated unlock check</li>
 *   <li>{@code counter:id>=N}, {@code counter:id>N}, {@code counter:id<N}, {@code counter:id<=N}, {@code counter:id==N}</li>
 * </ul>
 */
public final class SceneConditionExpression {
    private static final Pattern COUNTER_PATTERN =
        Pattern.compile("^(!?)counter:([\\w.\\-]+)(>=|>|<=|<|==)(-?\\d+)$");
    private static final Pattern FLAG_PATTERN =
        Pattern.compile("^(!?)(flag|unlock):([\\w.\\-]+)$");

    public enum Kind { FLAG, UNLOCK, COUNTER }
    public enum CounterOp { GT, GTE, LT, LTE, EQ }

    private final Kind kind;
    private final String id;
    private final boolean negated;
    private final CounterOp counterOp;
    private final int counterThreshold;

    private SceneConditionExpression(Kind kind, String id, boolean negated, CounterOp counterOp, int counterThreshold) {
        this.kind = kind;
        this.id = id;
        this.negated = negated;
        this.counterOp = counterOp;
        this.counterThreshold = counterThreshold;
    }

    public static SceneConditionExpression parse(String raw) {
        Validation.requireNonBlank(raw, "Condition expression is required.");
        Matcher counterMatcher = COUNTER_PATTERN.matcher(raw);
        if (counterMatcher.matches()) {
            boolean neg = !counterMatcher.group(1).isEmpty();
            String id = counterMatcher.group(2);
            CounterOp op = switch (counterMatcher.group(3)) {
                case ">=" -> CounterOp.GTE;
                case ">"  -> CounterOp.GT;
                case "<=" -> CounterOp.LTE;
                case "<"  -> CounterOp.LT;
                case "==" -> CounterOp.EQ;
                default -> throw new IllegalArgumentException("Unknown counter operator in: " + raw);
            };
            int threshold = Integer.parseInt(counterMatcher.group(4));
            return new SceneConditionExpression(Kind.COUNTER, id, neg, op, threshold);
        }
        Matcher flagMatcher = FLAG_PATTERN.matcher(raw);
        if (flagMatcher.matches()) {
            boolean neg = !flagMatcher.group(1).isEmpty();
            Kind kind = flagMatcher.group(2).equals("flag") ? Kind.FLAG : Kind.UNLOCK;
            String id = flagMatcher.group(3);
            return new SceneConditionExpression(kind, id, neg, null, 0);
        }
        throw new IllegalArgumentException("Unrecognised condition expression: " + raw);
    }

    public Kind kind() { return kind; }
    public String id() { return id; }
    public boolean negated() { return negated; }
    public CounterOp counterOp() { return counterOp; }
    public int counterThreshold() { return counterThreshold; }
}
