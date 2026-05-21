package com.eb.javafx.util;

import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shared helpers for interacting with the JavaFX application thread.
 */
public final class UtilJavaFx {
    /**
     * Worker pool used by {@link #runAsync(Supplier, Consumer, Consumer)}.  Daemon threads
     * (single shared pool to keep the cost of UI background work bounded — image rasterisation,
     * resource loading, save serialisation, etc.) so they don't block JVM shutdown.
     */
    private static final ScheduledExecutorService WORKER_POOL = Executors.newScheduledThreadPool(
            Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1)),
            new ThreadFactory() {
                private final AtomicLong counter = new AtomicLong();
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "novlfx-ui-worker-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private UtilJavaFx() {
    }

    /**
     * Runs the action immediately when already on the JavaFX application thread, or schedules it with
     * {@link Platform#runLater(Runnable)} otherwise.
     *
     * @param runnable action to execute on the JavaFX application thread
     */
    public static void run(Runnable runnable) {
        Runnable action = Validation.requireNonNull(runnable, "JavaFX action is required.");
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * Runs {@code work} on a shared daemon background thread and hands the result back to
     * {@code onSuccess} on the JavaFX application thread.  Any exception thrown by {@code work}
     * is forwarded to {@code onError} (also on the FX thread) instead of escaping into the
     * worker pool and being silently swallowed.
     *
     * <p>This is the standard pattern for keeping the FX thread responsive while doing
     * potentially-slow work that produces an FX object: image rasterisation, file I/O, JSON
     * parsing, scene-graph construction from disk.  The contract is deliberately one-shot —
     * callers that need progress reporting or cancellation should use {@link javafx.concurrent.Task}
     * directly.</p>
     *
     * <p>{@code onError} may be {@code null} to ignore failures (the exception is still printed
     * to {@code System.err} so it doesn't disappear without trace).</p>
     *
     * @param work       work to perform on a background thread; must produce a non-null result
     *                   or throw — returning {@code null} delivers {@code null} to {@code onSuccess}
     * @param onSuccess  consumer invoked on the FX thread with the result of {@code work}
     * @param onError    consumer invoked on the FX thread when {@code work} throws (may be {@code null})
     * @param <T>        type of the value produced
     */
    public static <T> void runAsync(
            Supplier<T> work,
            Consumer<? super T> onSuccess,
            Consumer<? super Throwable> onError) {
        Supplier<T> task = Validation.requireNonNull(work, "Background work supplier is required.");
        Consumer<? super T> deliver = Validation.requireNonNull(onSuccess, "Success consumer is required.");
        WORKER_POOL.execute(() -> {
            T result;
            try {
                result = task.get();
            } catch (Throwable throwable) {
                if (onError != null) {
                    run(() -> onError.accept(throwable));
                } else {
                    System.err.println("[UtilJavaFx.runAsync] background work threw: " + throwable);
                    throwable.printStackTrace();
                }
                return;
            }
            T delivered = result;
            run(() -> deliver.accept(delivered));
        });
    }
}
