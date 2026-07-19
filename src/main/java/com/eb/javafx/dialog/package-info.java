/**
 * Authored branching-dialog engine — an authored graph of {@link com.eb.javafx.dialog.DialogChain
 * chains} of {@link com.eb.javafx.dialog.DialogNode nodes} (line / thought / narration / choice),
 * with choice-availability gates ({@link com.eb.javafx.dialog.DialogChoiceRequirement}), per-line
 * side-effects (item grants, movement, screen keys, location/room/phone unlocks), scenes, an
 * editable working copy, a chain-at-a-time playback {@link com.eb.javafx.dialog.DialogDirector
 * director}, and per-playthrough progress ({@link com.eb.javafx.dialog.StoryLine}). Loaded from JSON
 * by {@link com.eb.javafx.dialog.DialogJsonLoader} (on the engine's {@code SimpleJson}/{@code
 * JsonData}, no external JSON dependency).
 *
 * <p><b>Relation to the other engine systems.</b> This is a distinct model that <i>coexists</i> with
 * the scene executor and the conversation line-bank rather than replacing them:</p>
 * <ul>
 *   <li>{@code com.eb.javafx.storyline} — the storyline/event engine decides <i>which</i> chain
 *       plays and when. Its {@code EventTrigger.DialogChain(id)} names a chain here by id and records
 *       the chosen option as the event's status; this package is the chain content + playback it
 *       drives. (Wiring that seam directly is future work.)</li>
 *   <li>{@code com.eb.javafx.scene} — {@code SceneStep}/{@code SceneChoice}/{@code SceneTransition}
 *       is the engine's other branching model (code-registered gates/effects). This package's gates
 *       and grants are instead <b>JSON-authorable</b> and translation/review-friendly.</li>
 *   <li>{@code com.eb.javafx.state} — a choice's state-path gate resolves through
 *       {@code StateQuery}.</li>
 * </ul>
 *
 * <p>Promoted from the AltLife game (was {@code com.altlife.javafx.gamecontent.story}); the game
 * supplies the {@link com.eb.javafx.dialog.PartnerStats} implementation, the item/screen/location
 * vocabulary, and text resolution.</p>
 */
package com.eb.javafx.dialog;
