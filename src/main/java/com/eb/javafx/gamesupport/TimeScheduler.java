package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** In-memory scheduler for data-only commands keyed by generic game time. */
public final class TimeScheduler {
    private final CodeTableDefinition timeSlots;
    private final List<TimeScheduledCommand> commands = new ArrayList<>();

    public TimeScheduler(CodeTableDefinition timeSlots) {
        this.timeSlots = Validation.requireNonNull(timeSlots, "Time slots are required.");
    }

    public void schedule(TimeScheduledCommand command) {
        TimeScheduledCommand checkedCommand = Validation.requireNonNull(command, "Scheduled command is required.");
        if (!timeSlots.contains(checkedCommand.runAt().timeSlotId())) {
            throw new IllegalArgumentException("Unknown scheduled command time slot: " + checkedCommand.runAt().timeSlotId());
        }
        if (commands.stream().anyMatch(existing -> existing.id().equals(checkedCommand.id()))) {
            throw new IllegalArgumentException("Scheduled command already exists: " + checkedCommand.id());
        }
        commands.add(checkedCommand);
    }

    public List<TimeScheduledCommand> commands() {
        return Collections.unmodifiableList(commands);
    }

    public List<TimeScheduledCommand> dueAtOrBefore(GameDateTime currentTime) {
        Validation.requireNonNull(currentTime, "Current time is required.");
        return commands.stream()
                .filter(command -> compare(command.runAt(), currentTime) <= 0)
                .toList();
    }

    public List<TimeScheduledCommand> drainDueAtOrBefore(GameDateTime currentTime) {
        List<TimeScheduledCommand> due = new ArrayList<>();
        Iterator<TimeScheduledCommand> iterator = commands.iterator();
        while (iterator.hasNext()) {
            TimeScheduledCommand command = iterator.next();
            if (compare(command.runAt(), currentTime) <= 0) {
                due.add(command);
                iterator.remove();
            }
        }
        return List.copyOf(due);
    }

    private int compare(GameDateTime left, GameDateTime right) {
        int dayComparison = Integer.compare(left.day(), right.day());
        if (dayComparison != 0) {
            return dayComparison;
        }
        return Integer.compare(slotIndex(left.timeSlotId()), slotIndex(right.timeSlotId()));
    }

    private int slotIndex(String timeSlotId) {
        List<CodeDefinition> slots = timeSlots.codes();
        for (int index = 0; index < slots.size(); index++) {
            if (slots.get(index).id().equals(timeSlotId)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Unknown time slot: " + timeSlotId);
    }
}
