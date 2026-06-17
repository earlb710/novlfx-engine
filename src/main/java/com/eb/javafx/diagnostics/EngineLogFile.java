package com.eb.javafx.diagnostics;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Mirrors everything written to {@link System#out} and {@link System#err} into a log file, so a
 * novlfx game's diagnostic output (the {@code [Class] ...} {@code println} lines used across the
 * engine and games) is captured to disk for after-the-fact inspection.
 *
 * <p>The file is <b>truncated on install</b>, so each run starts with a clean log.  Console output
 * is preserved — writes go to both the original stream and the file (a "tee"), and the file stream
 * auto-flushes so a crash still leaves the captured output on disk.</p>
 *
 * <h2>Usage</h2>
 * <p>Call once, as early as possible in the host's {@code main} (before the JavaFX launch), so it
 * captures boot logging too:</p>
 * <pre>{@code
 * public static void main(String[] args) {
 *     EngineLogFile.installDefault();   // -> log.txt in the working directory, fresh each run
 *     Application.launch(GameApplication.class, args);
 * }
 * }</pre>
 *
 * <h2>Threading</h2>
 * <p>Writes from the redirected {@code out} / {@code err} are serialised on the shared file stream,
 * so concurrent logging from multiple threads can't interleave mid-line bytes into a corrupt file.</p>
 */
public final class EngineLogFile {

    /** Default log file name, resolved against the process working directory. */
    public static final String DEFAULT_LOG_FILE = "log.txt";

    private static volatile boolean installed;

    private EngineLogFile() {
    }

    /** Installs the tee to {@value #DEFAULT_LOG_FILE} in the working directory (fresh each run). */
    public static void installDefault() {
        install(Path.of(DEFAULT_LOG_FILE));
    }

    /**
     * Redirects {@link System#out} / {@link System#err} to tee into {@code logFile} (truncated), in
     * addition to their original console streams.  Idempotent within a run — a second call is a
     * no-op (the file is only truncated by the first install of the process).
     */
    public static synchronized void install(Path logFile) {
        if (installed) {
            return;
        }
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            OutputStream file = Files.newOutputStream(logFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            Object lock = new Object();
            System.setOut(tee(System.out, file, lock));
            System.setErr(tee(System.err, file, lock));
            installed = true;
            System.out.println("[EngineLog] Logging to " + logFile.toAbsolutePath());
        } catch (IOException ex) {
            // Logging is best-effort — never let a missing/locked file stop the game from starting.
            System.err.println("[EngineLog] Could not open log file '" + logFile + "': " + ex);
        }
    }

    private static PrintStream tee(PrintStream console, OutputStream file, Object lock) {
        OutputStream both = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                synchronized (lock) {
                    console.write(b);
                    file.write(b);
                }
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                synchronized (lock) {
                    console.write(b, off, len);
                    file.write(b, off, len);
                }
            }

            @Override
            public void flush() throws IOException {
                synchronized (lock) {
                    console.flush();
                    file.flush();
                }
            }
        };
        return new PrintStream(both, /*autoFlush*/ true, StandardCharsets.UTF_8);
    }
}
