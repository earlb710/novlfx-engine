## Phase 3 — Ren'Py Parity: Audio Completeness

Four audio features added to reach Ren'Py parity.

### Voice channel + auto-advance (3.1)
`AudioService.VOICE_CHANNEL = "voice"` added alongside existing channel constants. `AudioService.initialize()` registers the voice channel and reads `voiceVolume` from preferences. `PreferencesService` gains `voiceEnabled` (boolean, default true), `voiceVolume` (double, default 1.0), and `autoAdvanceOnVoiceEnd` (boolean, default false), each with a load path, accessor, and save method. `SceneStep` gains `voiceRef()` and `withVoiceRef(String)` metadata accessors. `SceneExecutionResult` gains `voiceRequest()` returning `Optional<SoundRequest>` and `withVoiceRequest(SoundRequest)` immutable wither. `SceneExecutor` populates `voiceRequest` on DIALOGUE and NARRATION steps that carry a `voiceRef`. Auto-advance is adapter-driven: adapter calls `SceneExecutor.advance()` when voice ends; the engine receives a normal advance signal.

### Scene transition sounds (3.2)
`VisualTransitionDefinition` gains a nullable `soundRef` field and `withSoundRef(String)` wither. `VisualTransitionRequest.soundRef()` delegates to the definition. Existing constructor and all callers unchanged.

### Music queue (3.3)
`SoundRequest.queued(String audioRef, String channelId)` static factory builds a non-looping full-volume request. `AudioService` gains `queueMusic(SoundRequest)` (single-depth queue — replaces any pending entry), `clearQueue(String channelId)`, and `queuedRequest(String channelId)` returning `Optional<SoundRequest>`. `stopChannel` now also clears any queued entry for that channel.

### Audio channel priorities and ducking (3.4)
`DuckingPolicy` enum: `NONE`, `REDUCE_TO_PERCENT`, `PAUSE`. `AudioChannelConfig` value object: channelId, priority (int), defaultVolume (double), duckingPolicy, duckingPercent. `AudioService.registerChannel(AudioChannelConfig)` overload stores configs keyed by channel ID. `AudioService.channelConfig(String)` returns the registered config or a default (priority 0, NONE) for unregistered channels. Engine defines the contract; adapters query `channelConfig()` and apply volume changes at play time.

**Files changed:**
- `src/main/java/com/eb/javafx/prefs/PreferencesService.java`
- `src/main/java/com/eb/javafx/audio/AudioService.java`
- `src/main/java/com/eb/javafx/audio/SoundRequest.java`
- `src/main/java/com/eb/javafx/audio/DuckingPolicy.java` (new)
- `src/main/java/com/eb/javafx/audio/AudioChannelConfig.java` (new)
- `src/main/java/com/eb/javafx/scene/SceneStep.java`
- `src/main/java/com/eb/javafx/scene/SceneExecutionResult.java`
- `src/main/java/com/eb/javafx/scene/SceneExecutor.java`
- `src/main/java/com/eb/javafx/transitions/VisualTransitionDefinition.java`
- `src/main/java/com/eb/javafx/transitions/VisualTransitionRequest.java`
