package com.eb.javafx.driver;

import com.eb.javafx.state.StateQuery;
import com.eb.javafx.util.SimpleJson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * A localhost-only HTTP control channel for automated testing of a running novlfx game — the primary
 * command/response path for an external test agent. Engine-generic: the game supplies a
 * {@link DriverBackend} (world snapshot + command vocabulary); the engine owns the transport, the
 * generic UI automation ({@link UiDriver}), and the addressable {@code /state} query
 * ({@link StateQuery}).
 *
 * <p><b>Off by default.</b> Starts only when {@code -Dnovlfx.driver=true} (or an explicit
 * {@code -Dnovlfx.driverPort=<port>}) is passed — so a shipped build never opens a socket. The legacy
 * {@code -Daltlife.driver} / {@code -Daltlife.driverPort} names are also honoured. Binds to the
 * loopback interface exclusively.</p>
 *
 * <p>Every request is marshalled onto the JavaFX application thread (all game state is FX-thread
 * confined) via {@link Platform#runLater}, and the HTTP worker thread blocks on the result before
 * writing the response. That marshalling is the single point upholding the "all game state on the FX
 * thread" invariant — never read game state from a worker thread.</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code GET /health}  — {@code {"ok":true,"fxThread":true}} once the FX runtime answers
 *       (a jammed or not-yet-started FX thread yields {@code 503}).</li>
 *   <li>{@code GET /observe} — the {@link DriverBackend#observe()} world snapshot.</li>
 *   <li>{@code GET /state?path=…} — the {@link StateQuery} addressable read (empty path → listing).</li>
 *   <li>{@code POST /cmd} — dispatches a command to {@link DriverBackend#command(String, String)}.</li>
 * </ul>
 */
public final class DriverServer {

    /** System property: {@code true} to enable the driver channel. */
    public static final String ENABLE_PROPERTY = "novlfx.driver";
    /** System property: explicit port (also enables the channel). Defaults to {@link #DEFAULT_PORT}. */
    public static final String PORT_PROPERTY = "novlfx.driverPort";
    /** Legacy enable property honoured for existing run scripts / tooling. */
    public static final String LEGACY_ENABLE_PROPERTY = "altlife.driver";
    /** Legacy port property honoured for existing run scripts / tooling. */
    public static final String LEGACY_PORT_PROPERTY = "altlife.driverPort";
    public static final int DEFAULT_PORT = 8760;

    /** How long an HTTP worker waits for the FX thread before giving up with 503. */
    private static final long FX_TIMEOUT_SECONDS = 10;

    private static HttpServer server;
    private static DriverBackend backend;

    private DriverServer() {
    }

    /**
     * Starts the driver channel if enabled by system property. No-op when disabled, and no-op if
     * already started. Call once during boot, after the game has finished bootstrapping. The
     * {@code backend} supplies the world snapshot + command vocabulary; state namespaces must already
     * be registered with {@link StateQuery} by this point. Failures to bind are logged, not thrown, so
     * they never abort boot.
     */
    public static synchronized void maybeStart(Stage primaryStage, DriverBackend driverBackend) {
        if (server != null) {
            return;
        }
        boolean enabled = Boolean.getBoolean(ENABLE_PROPERTY)
                || Boolean.getBoolean(LEGACY_ENABLE_PROPERTY)
                || System.getProperty(PORT_PROPERTY) != null
                || System.getProperty(LEGACY_PORT_PROPERTY) != null;
        if (!enabled) {
            return;
        }
        backend = driverBackend;
        // Bind the stage so the UI-introspection commands (screen / press) can read the live scene.
        UiDriver.bind(primaryStage);
        int port = Integer.getInteger(PORT_PROPERTY, Integer.getInteger(LEGACY_PORT_PROPERTY, DEFAULT_PORT));
        try {
            HttpServer s = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
            AtomicInteger threadId = new AtomicInteger();
            s.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "novlfx-driver-http-" + threadId.incrementAndGet());
                t.setDaemon(true);
                return t;
            }));
            s.createContext("/health", DriverServer::handleHealth);
            s.createContext("/observe", ex -> handleFxJson(ex, DriverServer::observe));
            s.createContext("/state", DriverServer::handleState);
            s.createContext("/cmd", DriverServer::handleCmd);
            s.start();
            server = s;
            Runtime.getRuntime().addShutdownHook(
                    new Thread(DriverServer::stop, "novlfx-driver-shutdown"));
            System.out.println("[DriverServer] listening on http://127.0.0.1:" + port
                    + " (endpoints: /health, /observe, /state, /cmd)");
        } catch (IOException e) {
            System.out.println("[DriverServer] failed to start on port " + port + ": " + e);
        }
    }

    /** Stops the channel if running. Safe to call more than once. */
    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private static String observe() {
        return backend == null ? "{\"ok\":false,\"error\":\"no-backend\"}" : backend.observe();
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        // Round-trip through the FX thread so /health also proves the app thread is responsive,
        // not merely that the socket accepted a connection.
        String body = runOnFxThread(() -> "{\"ok\":true,\"fxThread\":true}");
        respond(exchange, body == null ? 503 : 200,
                body == null ? "{\"ok\":false,\"error\":\"fx-thread-timeout\"}" : body);
    }

    private static void handleFxJson(HttpExchange exchange, Supplier<String> fxWork)
            throws IOException {
        String body = runOnFxThread(fxWork);
        respond(exchange, body == null ? 503 : 200,
                body == null ? "{\"error\":\"fx-thread-timeout\"}" : body);
    }

    /** Addressable state query: {@code /state?path=mc.energy} (empty path → namespace listing). */
    private static void handleState(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String path = query.get("path");
        String out = runOnFxThread(() -> StateQuery.query(path));
        respond(exchange, out == null ? 503 : 200,
                out == null ? "{\"ok\":false,\"error\":\"fx-thread-timeout\"}" : out);
    }

    /**
     * Dispatches a driver command. Reads {@code cmd} / {@code arg} from the query string first
     * ({@code /cmd?cmd=move&arg=university}); if absent, falls back to a JSON request body
     * ({@code {"cmd":"enter","arg":"Living Room > Bedroom 1"}}) — handy when the arg contains spaces.
     * Parsing happens on the worker thread; only the command execution hops to the FX thread.
     */
    private static void handleCmd(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String cmd = query.get("cmd");
        String arg = query.get("arg");
        if (cmd == null) {
            byte[] raw = exchange.getRequestBody().readAllBytes();
            if (raw.length > 0) {
                try {
                    Object parsed = SimpleJson.parse(new String(raw, StandardCharsets.UTF_8), "cmd-body");
                    if (parsed instanceof Map<?, ?> body) {
                        Object c = body.get("cmd");
                        Object a = body.get("arg");
                        if (c != null) {
                            cmd = String.valueOf(c);
                        }
                        if (a != null) {
                            arg = String.valueOf(a);
                        }
                    }
                } catch (RuntimeException malformedBody) {
                    respond(exchange, 400, "{\"ok\":false,\"error\":\"malformed-json-body\"}");
                    return;
                }
            }
        }
        final String fCmd = cmd;
        final String fArg = arg;
        String out = runOnFxThread(() ->
                backend == null ? "{\"ok\":false,\"error\":\"no-backend\"}" : backend.command(fCmd, fArg));
        respond(exchange, out == null ? 503 : 200,
                out == null ? "{\"ok\":false,\"error\":\"fx-thread-timeout\"}" : out);
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(key, value);
        }
        return out;
    }

    /**
     * Runs {@code work} on the JavaFX application thread and returns its result, or {@code null} if
     * the FX thread does not answer within {@link #FX_TIMEOUT_SECONDS} (or the FX runtime is not up
     * yet). The calling HTTP-worker thread blocks until the FX thread completes the work.
     */
    private static String runOnFxThread(Supplier<String> work) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            Platform.runLater(() -> {
                try {
                    future.complete(work.get());
                } catch (RuntimeException | Error ex) {
                    future.completeExceptionally(ex);
                }
            });
        } catch (IllegalStateException fxNotStarted) {
            return null;
        }
        try {
            return future.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return "{\"error\":\"driver-exception\",\"detail\":\""
                    + escape(String.valueOf(e.getCause())) + "\"}";
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }
}
