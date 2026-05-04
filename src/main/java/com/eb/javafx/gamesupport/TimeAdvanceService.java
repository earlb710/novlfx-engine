package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.function.Consumer;

/** Coordinates clock advancement, hooks, and scheduled command delivery. */
public final class TimeAdvanceService {
    private final GameClock clock;
    private final TimeScheduler scheduler;
    private final List<TimeAdvanceHook> hooks;

    public TimeAdvanceService(GameClock clock, TimeScheduler scheduler, List<TimeAdvanceHook> hooks) {
        this.clock = Validation.requireNonNull(clock, "Game clock is required.");
        this.scheduler = Validation.requireNonNull(scheduler, "Time scheduler is required.");
        this.hooks = List.copyOf(Validation.requireNonNull(hooks, "Time advance hooks are required."));
    }

    public GameDateTime advanceSlot(Consumer<TimeScheduledCommand> commandConsumer) {
        Consumer<TimeScheduledCommand> checkedConsumer = Validation.requireNonNull(
                commandConsumer,
                "Scheduled command consumer is required.");
        GameDateTime previous = clock.currentTime();
        hooks.forEach(hook -> hook.beforeAdvance(previous));
        GameDateTime current = clock.advanceSlot();
        hooks.forEach(hook -> hook.afterAdvance(previous, current));
        scheduler.drainDueAtOrBefore(current).forEach(checkedConsumer);
        return current;
    }
}
