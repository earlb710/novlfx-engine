import com.eb.javafx.events.EventSequencer;
import com.eb.javafx.events.GameCommand;
import com.eb.javafx.events.GameCommandDispatcher;
import com.eb.javafx.events.GameEvent;
import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.events.GameEventQueue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates the reusable {@code events} package: the fire-and-forget {@link GameEventBus}, the
 * FIFO {@link GameEventQueue}, type-keyed command dispatch via {@link GameCommandDispatcher}, and the
 * completion-gated {@link EventSequencer} that guarantees ordering — turn events finish, then their
 * bonus events, before a barrier opens the next turn.
 *
 * <p>Expected output prints the bus event and its history size, the drained queue count, the events
 * emitted by a dispatched command, and the sequencer execution order
 * {@code [turnEndA, turnEndB, bonus, BARRIER, turn2]} (bonus events complete before the barrier, and
 * the next turn only starts after the barrier).</p>
 */
public final class EventsDemo {
    private EventsDemo() {
    }

    public static void main(String[] args) {
        demoBus();
        demoQueue();
        demoCommandDispatch();
        demoSequencer();
    }

    /** Fire-and-forget publish/subscribe with a deterministic history and an unsubscribe handle. */
    private static void demoBus() {
        GameEventBus bus = new GameEventBus();
        Runnable unsubscribe = bus.subscribe(
                "route.changed",
                event -> System.out.println("Bus received " + event.type() + " -> " + event.payload()));

        bus.publish(GameEvent.now("route.changed", "menu", Map.of("to", "office")));
        unsubscribe.run();                                                   // stop listening
        bus.publish(GameEvent.now("route.changed", "menu", Map.of("to", "home"))); // recorded, not delivered

        System.out.println("Bus history for route.changed: " + bus.history("route.changed").size());
    }

    /** FIFO buffer: enqueue during a turn, drain at a controlled point. */
    private static void demoQueue() {
        GameEventQueue queue = new GameEventQueue();
        queue.enqueue(GameEvent.now("npc.spawned", "spawner", Map.of("id", "alex")));
        queue.enqueue(GameEvent.now("npc.spawned", "spawner", Map.of("id", "sam")));

        List<GameEvent> drained = queue.drain();
        System.out.println("Queue drained " + drained.size() + " event(s); empty now: " + queue.isEmpty());
    }

    /** Type-keyed command routed to its handler, whose emitted events are also published to the bus. */
    private static void demoCommandDispatch() {
        GameEventBus bus = new GameEventBus();
        GameCommandDispatcher dispatcher = new GameCommandDispatcher();
        dispatcher.register("give.item", command ->
                List.of(GameEvent.now("item.given", command.targetId(), command.payload())));

        GameCommand command = new GameCommand("c1", "give.item", "player", Map.of("item", "key"), Instant.EPOCH);
        List<GameEvent> emitted = dispatcher.dispatch(command, bus);

        System.out.println("Command emitted " + emitted.size() + " event(s); bus history: " + bus.history().size());
    }

    /**
     * Serial, completion-gated ordering: turn-end events finish, then the bonus event they spawned,
     * before the barrier opens the next turn. Uses instant completions so the demo runs headless.
     */
    private static void demoSequencer() {
        List<String> order = new ArrayList<>();
        EventSequencer sequencer = new EventSequencer();

        // First turn-end event awards a bonus event that must finish before the next turn.
        sequencer.enqueue(control -> {
            order.add("turnEndA");
            control.spawn(bonus -> {
                order.add("bonus");
                bonus.complete();
            });
            control.complete();
        });
        sequencer.enqueue(control -> {
            order.add("turnEndB");
            control.complete();
        });
        // Barrier: the next turn is gated behind every bonus event above.
        sequencer.enqueueBarrier(() -> {
            order.add("BARRIER");
            sequencer.enqueue(control -> {
                order.add("turn2");
                control.complete();
            });
        });
        sequencer.run();

        System.out.println("Sequencer order: " + order + "; idle: " + sequencer.isIdle());
    }
}
