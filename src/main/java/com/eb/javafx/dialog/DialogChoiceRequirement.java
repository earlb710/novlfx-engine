package com.eb.javafx.dialog;


import java.util.List;
import java.util.Map;

/**
 * Availability gate for a {@link DialogChoice} option — decides whether the option is
 * offered (and, when unmet, whether it is hidden or shown greyed with a reason).
 *
 * <p>Predicate families, all evaluated at prompt-build time:</p>
 * <ul>
 *   <li><b>Relationship-stat thresholds</b> on the conversation partner — {@code minStats} /
 *       {@code maxStats} keyed by a canonical stat name (see {@link #statValue}).</li>
 *   <li><b>Required items</b> — every id in {@code requiresItems} must be held.</li>
 *   <li><b>Story flags</b> — every id in {@code requiresFlags} must be set, and none in
 *       {@code forbidsFlags} may be.</li>
 *   <li><b>Prior choices</b> — every {@code {chainId, nodeId, optionId}} in
 *       {@code requiresChoices} must have been the recorded pick at that node.</li>
 * </ul>
 *
 * <p>{@code hideWhenUnmet} chooses the presentation when the gate fails: {@code true} drops the
 * option entirely; {@code false} (default) shows it disabled/greyed with {@code reason} as a
 * tooltip.</p>
 *
 * <h3>JSON format (on a choice option)</h3>
 * <pre>{@code
 * "requires": {
 *   "minLove": 30, "minTrust": 10, "maxFear": 40,
 *   "requiresItem": "lab-keycard",          // or "requiresItems": ["a","b"]
 *   "flag": "met-marsh",                     // or "flags": ["a","b"]   (all required)
 *   "notFlag": "betrayed-her",               // or "notFlags": ["x"]    (none may be set)
 *   "choiceMade": { "chain": "marsh.ch1.meet", "node": "marsh.help.choice", "option": "accept" },
 *   "reason": "She doesn't trust you enough yet.",
 *   "hideWhenUnmet": false
 * }
 * }</pre>
 *
 * <p>Stat checks need a partner; when the partner is unknown (null) a stat threshold is treated
 * as <b>unmet</b> (fail closed). Item / flag / choice checks go through the {@link Resolver}.</p>
 */
public record DialogChoiceRequirement(
        Map<String, Integer> minStats,
        Map<String, Integer> maxStats,
        List<String> requiresItems,
        List<String> requiresFlags,
        List<String> forbidsFlags,
        List<String[]> requiresChoices,
        List<StateCondition> requiresState,
        String reason,
        boolean hideWhenUnmet) {

    /** Resolves item / flag / prior-choice / state-path predicates against live game state. */
    public interface Resolver {
        boolean hasItem(String itemId);
        boolean hasFlag(String flagKey);
        boolean choiceMade(String chainId, String nodeId, String optionId);

        /**
         * Resolves a state-engine path (e.g. {@code "mc.energy"}, {@code "time.weekend"},
         * {@code "current.room.people"}) to its live value — a {@code Long} / {@code Boolean} /
         * {@code String} / {@code List<String>}, or {@code null} when unresolvable. Backed by
         * {@code AltLifeStateQuery.plainValue}. Default returns {@code null} (state predicates then
         * fail closed) for resolvers that don't wire it.
         */
        default Object stateValue(String path) {
            return null;
        }
    }

    /**
     * One state-path predicate: resolve {@code path} via {@link Resolver#stateValue} and compare the
     * live value against {@code expected} with {@code op}. Lets a gate depend on any state the
     * {@code AltLifeStateQuery} can address — MC/world state the fixed stat/item/flag keys can't
     * express (energy, money, time, room occupancy, …).
     */
    public record StateCondition(String path, Op op, Object expected) {
        /** {@code MIN}/{@code MAX} = numeric ≥/≤; {@code EQ}/{@code NE} = value equality; {@code HAS}
         *  = the value (an array) contains {@code expected}. */
        public enum Op { MIN, MAX, EQ, NE, HAS }

        public StateCondition {
            path = path == null ? "" : path.trim();
            op   = op == null ? Op.EQ : op;
        }

        /** True when {@code actual} (the resolved live value) satisfies this predicate. Fails closed
         *  on a null / unusable value. */
        public boolean test(Object actual) {
            if (path.isEmpty()) {
                return true;
            }
            if (actual == null) {
                return false;   // unresolvable state → fail closed
            }
            switch (op) {
                case MIN: {
                    Long a = asLong(actual), e = asLong(expected);
                    return a != null && e != null && a >= e;
                }
                case MAX: {
                    Long a = asLong(actual), e = asLong(expected);
                    return a != null && e != null && a <= e;
                }
                case EQ:  return String.valueOf(actual).equalsIgnoreCase(String.valueOf(expected));
                case NE:  return !String.valueOf(actual).equalsIgnoreCase(String.valueOf(expected));
                case HAS: return actual instanceof List<?> list
                        && list.stream().anyMatch(x ->
                                String.valueOf(x).equalsIgnoreCase(String.valueOf(expected)));
                default:  return false;
            }
        }

        private static Long asLong(Object o) {
            if (o instanceof Number n) {
                return n.longValue();
            }
            try {
                return o == null ? null : Long.parseLong(String.valueOf(o).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public DialogChoiceRequirement {
        minStats        = minStats        == null ? Map.of() : Map.copyOf(minStats);
        maxStats        = maxStats        == null ? Map.of() : Map.copyOf(maxStats);
        requiresItems   = requiresItems   == null ? List.of() : List.copyOf(requiresItems);
        requiresFlags   = requiresFlags   == null ? List.of() : List.copyOf(requiresFlags);
        forbidsFlags    = forbidsFlags    == null ? List.of() : List.copyOf(forbidsFlags);
        requiresChoices = requiresChoices == null ? List.of() : List.copyOf(requiresChoices);
        requiresState   = requiresState   == null ? List.of() : List.copyOf(requiresState);
        reason          = (reason == null || reason.isBlank()) ? null : reason.trim();
    }

    /** Backward-compatible form without state predicates. */
    public DialogChoiceRequirement(Map<String, Integer> minStats, Map<String, Integer> maxStats,
            List<String> requiresItems, List<String> requiresFlags, List<String> forbidsFlags,
            List<String[]> requiresChoices, String reason, boolean hideWhenUnmet) {
        this(minStats, maxStats, requiresItems, requiresFlags, forbidsFlags, requiresChoices,
                List.of(), reason, hideWhenUnmet);
    }

    /** Backward-compatible form without flag / choice / state predicates. */
    public DialogChoiceRequirement(Map<String, Integer> minStats, Map<String, Integer> maxStats,
            List<String> requiresItems, String reason, boolean hideWhenUnmet) {
        this(minStats, maxStats, requiresItems, List.of(), List.of(), List.of(), List.of(),
                reason, hideWhenUnmet);
    }

    /** True when this gate imposes no constraints at all. */
    public boolean isEmpty() {
        return minStats.isEmpty() && maxStats.isEmpty() && requiresItems.isEmpty()
                && requiresFlags.isEmpty() && forbidsFlags.isEmpty() && requiresChoices.isEmpty()
                && requiresState.isEmpty();
    }

    /**
     * Evaluates the gate.  {@code partner} is the conversation partner's {@link PartnerStats}
     * (nullable — stat checks fail closed when it's null).  {@code resolver} answers item / flag /
     * prior-choice / state questions (typically backed by the game's item + story-flag + state
     * stores); when null, any item/flag/choice/state predicate is treated as unmet.
     */
    public boolean isMet(PartnerStats partner, Resolver resolver) {
        for (Map.Entry<String, Integer> e : minStats.entrySet()) {
            Integer v = partner == null ? null : partner.namedStat(e.getKey());
            if (v == null || v < e.getValue()) {
                return false;
            }
        }
        for (Map.Entry<String, Integer> e : maxStats.entrySet()) {
            Integer v = partner == null ? null : partner.namedStat(e.getKey());
            if (v == null || v > e.getValue()) {
                return false;
            }
        }
        if (!requiresItems.isEmpty() || !requiresFlags.isEmpty()
                || !forbidsFlags.isEmpty() || !requiresChoices.isEmpty() || !requiresState.isEmpty()) {
            if (resolver == null) {
                return false;
            }
            for (String itemId : requiresItems) {
                if (!resolver.hasItem(itemId)) return false;
            }
            for (String flag : requiresFlags) {
                if (!resolver.hasFlag(flag)) return false;
            }
            for (String flag : forbidsFlags) {
                if (resolver.hasFlag(flag)) return false;
            }
            for (String[] c : requiresChoices) {
                if (c.length == 3 && !resolver.choiceMade(c[0], c[1], c[2])) return false;
            }
            for (StateCondition c : requiresState) {
                if (!c.test(resolver.stateValue(c.path()))) return false;
            }
        }
        return true;
    }

}
