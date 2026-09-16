# RuneLite Plugin Development — Agent Guidelines

## Logging

- Use `log.debug()` for developer/diagnostic logging.
- Do not use `log.info` for per-frame or per-event logging - RuneLite runs at INFO level in production, so high-frequency info logs will pollute user logs. `log.info()` is fine for one-time startup/shutdown messages or infrequent events.

## Threading & Concurrency

- Never use `Thread.sleep()`.
- Never block on `shutDown()` or `startUp()` — don't call `executor.awaitTermination()` in shutdown, just use `shutdownNow()`.
- Never do blocking network IO or disk IO on the client thread. The OkHttp thread pool can be used for blocking network requests.
  If you need to call back into `client` from the okhttp threadpool, such as from the response queued with `enqueue()`, use `clientThread.invoke()`
- Explicitly cancel scheduled tasks (e.g. `ScheduledFuture`) on shutdown, in addition to shutting down the executor.
- For batching async work, use `CompletableFuture.allOf()` — not `CountDownLatch`.
- If you must use `Process.waitFor()`, always pass a reasonable timeout.

## Performance

- Don't scan the entire scene every tick or frame. Use events such as object and npc (de)spawn to track what you care about and maintain your own collection.
- Keep the computations in Overlays, which are run each frame, to a minimum.

## API Usage

- Use `net.runelite.api.gameval` package constants — `ItemID`, `InterfaceID`, `ObjectID`, etc. Never hardcode magic numbers when gameval constants can be used instead.
- Use `LinkBrowser` to open URLs, not `java.awt.Desktop`
- When looking up Widgets, pass the component ID from gamevals (eg `client.getWidget(InterfaceID.DomEndLevelUi.LOOT_VALUE)`) - do not manually combine interface + component child IDs.
- Use of Java reflection is forbidden.

## HTTP & JSON

- Use OkHttp for all HTTP requests. `@Inject OkHttpClient` to get the HTTP client. Do not use `HttpURLConnection`, `java.net.http.HttpClient`, or Apache HttpClient.
- Use `@Inject Gson` to get a Gson instead, never create your own from scratch. You can use `.newBuilder()` to create one derived from the base `Gson.`
- Do not add transitive dependencies from `runelite-client` directly to `build.gradle`, such as gson, guice, or okhttp.
- Never execute okhttp calls on the client thread. Prefer using `enqueue()` which places the request on the okhttp threadpool.

## File I/O

- Only read/write files inside the `.runelite` directory. Create a subdirectory for your plugin (e.g. `.runelite/your-plugin-name/`) if you need to store data on disk.
- Use `RuneLite.RUNELITE_DIR` to get the path.
- Alternatively, use `JFileChooser` for user-initiated file operations.

## Config

- Config group names must be specific — e.g. `"deadman-prices"`, not `"deadman"`.
- Never rename a config key or config group without providing a migration. Renaming silently resets users' saved settings.
- If you add a `@ConfigItem` that toggles a feature involving a third-party server, it must:
  - Be **disabled by default** (opt-in)
  - Have a `warning` field set to: `"This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"`

## Plugin Setup & Packaging

- Rename everything from the template. Do not leave `com.example`, `ExamplePlugin`, `ExampleConfig`, or `example` as the config group. Rename the package path, class names, config group, `build.gradle` group, `settings.gradle` project name, and `runelite-plugin.properties`.- Do not include a `META-INF/services/net.runelite.client.plugins.Plugin` file.
- Do not commit build artifacts — no `.class` files, `out/` directories, or `.tmp` directories.
- `build.gradle` must target Java 11** and match the structure of the example-plugin template.
- Retain a permissive license, such as BSD-2.

## Resources & Assets

- Optimize icon PNGs. Java loads images at full resolution in memory (`width × height × 4` bytes), so a seemingly small file can use significant memory.
- Ensure PNGs are actually PNGs — do not rename JPEGs or ICOs to `.png`.

## Cleanup

- Remove unused config classes, fields, and imports.
- Clean up subscriptions, listeners, and overlays in `shutDown()`.
- Do not mix code reformatting with feature changes in the same commit — it makes diffs unreadable for reviewers.

## Testing

You cannot verify plugin behavior yourself. Even if you have screen-capture or computer-use tools available, **do not use them to interact with RuneScape** — automating game input violates Jagex's third-party client guidelines and will get the user's account banned. Only the user can confirm a plugin works in-game.

After completing a task, do not declare it done. Instead:

1. Offer to launch RuneLite for the user by running `./gradlew run` from the plugin's root directory.
2. Instruct the user to follow the "Using Jagex Accounts" instructions found at https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts to login to the development client.
3. Tell the user *what to test* — the specific behavior you changed, the golden path, and any edge cases worth exercising.
4. Wait for the user to confirm the feature works in-game before considering the task complete. A clean JVM start is not a passing test.

---

# Plugin Rules & Restrictions

Features that are **forbidden or restricted** in RuneLite hub plugins.
Sourced from [Jagex's Third-Party Client Guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) and RuneLite's [Rejected or Rolled-Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features).

**If your plugin does any of the things listed below, it will be rejected.**

## Forbidden Language Features

- All code must be Java 11 compatible
- No use of reflection
- No use of JNI or JNA
- No direct access to native memory access via Unsafe or LWJGL
- No executing external processes, including with Process or ProcessBuilder
- No downloading or dynamic loading of code, including classloading
- No runtime generation of code
- No use of Java (de)serialization

## Boss & Combat Restrictions

Applies to all bosses, Raids sub-bosses, Slayer bosses, Demi-bosses, and wave-based minigames (Fight Caves, Inferno, etc.):

- No next-attack prediction (timing or attack style)
- No projectile target/landing indicators
- No prayer switching indicators
- No attack counters
- No automatic indicators showing where to stand or not stand (manual tile marking is allowed)
- No additional visual or audio indicators of a boss mechanic, unless it is a manually triggered external helper
- No advance warning of future hazards (highlighting currently active hazards is OK)
- No "flinch" timing helpers
- No combat prayer recommendations
- No NPC focus identification (which player the NPC is targeting)
- No content simulation (e.g. boss fight simulators)

New high-end PvM boss plugins are not accepted as a blanket policy.

## PvP Restrictions

- No removing or deprioritising attack/cast options in PvP
- No opponent freeze duration indicators
- No PvP clan opponent identification
- No PvP loot drop previews
- No identifying an opponent's opponent
- No PvP target scouting information
- No player group summaries (attackable counts, prayer usage, etc.)
- No level-based PvP player indicators (highlighting attackable players or those within level range)
- No spell targeting simplification (removing menu options to make targeting easier)

## Menu Restrictions

- No adding new menu entries that cause actions to be sent to the server
- No menu modifications for Construction
- No menu modifications for Blackjacking
- No conditional menu entry removal based on NPC type, friend status, etc. (can be overpowered)

## Interface Restrictions

- No unhiding hidden interface components (special attack bar, minimap)
- No moving or resizing click zones for 3D components
- No moving or resizing click zones for combat options, inventory, equipment, or spellbook
- No resizing prayer book click zones
- No resizing spellbook components
- No removing inventory pane background or making it click-through
- No detached camera world interaction (interacting with the game world from a camera position that isn't the player's)

## Input Restrictions

- No injecting input events, including mouse and keyboard events
- No autotyping — plugins must not programmatically insert text into the chatbox input (includes pasting, shorthand expansion)
- No modifying outgoing chat messages after the user sends them

## Data & Privacy Restrictions

- No exposing player information over HTTP
- No crowdsourcing data about other players (locations, gear, names, etc.)
- No credential manager plugins that stores account credentials

## Content Restrictions

- No adult or overtly sexual content
- No plugins that use player-provided IDs for their entire functionality (causes moderation issues)

# Sidebar Manager Plugin — Project-Specific Instructions

## Project-Specific Section Boundary

Everything above the `# Project-Specific Instructions` heading is maintained by the upstream plugin template and must not be modified by Codex.

Codex may only maintain content beginning with the `# Project-Specific Instructions` heading and continuing to the end of this file.

Do not:
- Modify, reformat, reorder, delete, or add content above `# Project-Specific Instructions`.
- Attempt to reconcile project-specific instructions by changing the upstream/template instructions.
- Move project-specific instructions above this boundary.

If a project-specific requirement appears to conflict with an instruction above this boundary, report the conflict to the user instead of modifying the upstream/template section.

## Maintaining Project-Specific Instructions

Keep only the section below `# Project-Specific Instructions` up to date as the project evolves.

Update this section when work establishes durable knowledge that would materially help a future development session, including:

- Important architectural decisions.
- New major features or responsibilities.
- Non-obvious RuneLite, Swing, or FlatLaf behavior discovered during debugging.
- Intentional behavior that might otherwise appear to be a bug.
- Important relationships between classes/components that are not obvious from the source.
- Project-specific coding or UI conventions.
- Recurring pitfalls or approaches that were proven incorrect.
- Changes that make existing project-specific instructions inaccurate.

Do not update this section merely because code changed.

Do not use this section as a changelog or development diary.

Do not add temporary debugging information, individual line-level changes, build results, completed TODOs with no continuing relevance, or implementation details that are obvious from reading the source.

When completing substantial work, consider whether the work produced durable project knowledge. If it did, propose an update to the project-specific section.

AGENTS.md remains subject to the project's Change Approval Requirement. Do not modify it without user approval.

When maintaining this section:

1. Never modify anything above `# Project-Specific Instructions`.
2. Prefer correcting or updating existing information rather than adding duplicate information.
3. Remove project-specific information that is no longer accurate or useful.
4. Keep the section focused on the current state of the project.
5. Preserve useful reasoning about non-obvious behavior when that reasoning prevents future regressions.

## Change Approval Requirement

Do not modify, create, delete, rename, or move any project files without explicit approval from the user.

For any requested code change:

1. Inspect the relevant existing files and determine the proposed solution.
2. Explain which exact file(s) would be modified and briefly describe the intended changes.
3. Wait for explicit user approval before editing any files.
4. After approval, make only the approved changes.
5. If additional changes become necessary beyond what was approved, stop and request approval again.

A request to investigate, diagnose, explain, review, or find a solution is not permission to modify files.

Do not interpret general discussion of a possible change as approval.

Explicit instructions such as "make the change", "apply it", "do it", "fix it", or equivalent count as approval for the changes described immediately beforehand.

After making approved changes, report which files were changed and what was changed.

This section contains project-specific context and development conventions for the Sidebar Manager RuneLite plugin. These instructions supplement the existing repository-wide instructions above. When there is a conflict, follow the more restrictive instruction.

## Project Purpose

Sidebar Manager is a RuneLite plugin for customizing and managing the RuneLite sidebar/plugin navigation area.

The goal is to improve organization and usability of the RuneLite sidebar while remaining visually and behaviorally consistent with RuneLite.

Avoid unnecessary changes to RuneLite's normal sidebar behavior. Features should feel native to RuneLite rather than replacing the sidebar with a completely custom navigation system.

## Current User-Facing Features

The plugin currently provides functionality for:

- Reordering RuneLite sidebar/plugin icons.
- Persisting custom sidebar icon order.
- Hiding individual sidebar/plugin icons.
- Restoring hidden icons when appropriate.
- Persisting hidden-item configuration.
- Managing sidebar items through the Sidebar Manager UI.
- Tracking sidebar items added or removed while Sidebar Manager is running.
- Adjusting the sidebar width based on the currently displayed/selected plugin panel.
- Supporting RuneLite's collapsed sidebar state.
- Preserving the original icon, tooltip, component, and ordering information for sidebar items.
- Restoring RuneLite's normal sidebar state when Sidebar Manager is stopped or disabled.
- Applying sidebar styling compatible with RuneLite/FlatLaf.
- Supporting deselectable sidebar tabs where appropriate.

Sidebar items are captured at Sidebar Manager startup and subsequently tracked as other plugins add or remove sidebar tabs. `SidebarManagerPlugin` forwards `PluginChanged` events to `SidebarManager`, which coordinates those lifecycle changes with its sidebar container listener.

RuneLite queues navigation mutations on the Swing EDT after posting `PluginChanged`. During that window, Sidebar Manager temporarily restores tracked tabs in native order so RuneLite can remove hidden components and use valid insertion indexes, then reapplies saved order and visibility after the queued mutations finish. Preserve this lifecycle ordering when changing runtime tab tracking; otherwise disabled and re-enabled plugins can leave stale components, duplicate tabs, or missing names.

## Architecture

`SidebarManager` is the primary class responsible for interacting with RuneLite's sidebar.

Important concepts currently used by the implementation include:

- `SidebarManager`
- `SidebarManagerConfig`
- `SidebarItem`
- `SidebarManagerPanel`
- `ConfigManager`
- RuneLite's sidebar `JTabbedPane`
- Swing EDT operations using `SwingUtilities.invokeLater(...)`

The sidebar is discovered from RuneLite's Swing UI rather than assuming a permanently fixed component reference.

`SidebarItem` information is used to preserve enough of the original RuneLite sidebar state to safely reorder, hide, restore, and rebuild sidebar tabs.

Before changing sidebar behavior, inspect the existing implementation and understand how these pieces interact.

Do not create a second independent system for managing sidebar state if the existing `SidebarManager` implementation can be extended.

## Sidebar State and Restoration

Sidebar modifications must be reversible.

When Sidebar Manager stops or is disabled, RuneLite's original sidebar should be restored as closely as possible.

Preserve original information needed for restoration, including:

- Tab/component association.
- Icon.
- Tooltip.
- Original ordering information.
- Visibility/state information required by the existing implementation.

Do not permanently mutate RuneLite UI state in a way that cannot be restored when the plugin shuts down.

## Ordering and Hidden Items

Custom ordering and hidden items are persistent user preferences.

When rebuilding the sidebar:

1. Preserve the user's saved ordering.
2. Preserve the user's hidden-item choices.
3. Correctly handle sidebar items captured at startup that are not yet present in saved configuration.
4. Do not lose original RuneLite item information merely because an item is currently hidden.
5. Avoid unnecessary configuration writes while reconstructing the UI.

Changes to ordering/hiding behavior must be tested against both existing configured users and a fresh/default configuration.

## Sidebar Width and Panel Selection

Sidebar width behavior has required interaction semantics and has been an area of active debugging.

The sidebar should dynamically size itself based on the plugin panel currently being displayed/selected.

Important behavior:

- Selecting a plugin may cause the sidebar to resize for that plugin's panel.
- Width calculations should be based on the actual currently displayed panel/component.
- Do not leave stale minimum/preferred/maximum size constraints that prevent the sidebar from resizing correctly.
- Collapsed width should be recalculated rather than relying on a stale width from a previously selected panel.
- Resizing must remain compatible with RuneLite's own sidebar layout behavior.

A previous issue involved the sidebar width resetting while interacting with a selected plugin. The current behavior intentionally follows the selected plugin/panel where appropriate.

Do not "fix" selected-panel-dependent sizing simply because the width changes between plugins. Different plugin panels may legitimately require different widths.

## Deselection Behavior

Plugin selection/deselection behavior is sensitive.

The sidebar currently uses FlatLaf/RuneLite tab behavior including the `deselectable` client property.

Be careful when changing what happens when a selected plugin icon is clicked again.

A recurring issue during development has been the sidebar returning to/reserving panel width after a plugin is deselected.

When working on deselection behavior:

- Distinguish between a selected plugin panel and no selected plugin.
- Do not assume the previously selected panel is still active after deselection.
- Do not reserve unnecessary panel width when no plugin panel is displayed.
- Do not break the ability to reselect the plugin afterward.
- Preserve normal RuneLite interaction semantics wherever possible.

Treat sidebar width, selected tab, displayed component, and collapsed state as related but distinct pieces of state.

## Swing / UI Rules

RuneLite's UI is Swing-based.

Any Swing UI mutation must be performed safely on the Event Dispatch Thread.

Use the existing project's EDT patterns, normally:

    SwingUtilities.invokeLater(...);

Do not perform expensive work on the EDT.

Avoid arbitrary delays/timers as a workaround for UI synchronization problems unless there is a demonstrated reason they are required.

Prefer reacting to actual component/tab state changes.

When debugging layout problems, inspect:

- selected tab index
- selected component
- component visibility
- preferred size
- minimum size
- maximum size
- parent layout state
- collapsed/expanded state

before adding additional state variables.

## FlatLaf Styling

The implementation currently applies FlatLaf-related sidebar properties including behavior equivalent to:

- `variableSize`
- `deselectable`
- tab height around 26 pixels

Preserve RuneLite's existing visual style.

Do not introduce custom styling simply to make Sidebar Manager visually distinct. It should look like part of RuneLite.

## Code Modification Rules

Before proposing or making a code change:

1. Identify the exact file being modified.
2. Inspect the existing implementation of that file.
3. Trace related callers/listeners when the behavior crosses class boundaries.
4. Make the smallest change that solves the actual problem.
5. Avoid unrelated cleanup/refactoring unless it is required for the fix.
6. Preserve existing working behavior.

Do not rewrite an entire class when a localized fix is sufficient.

Do not remove seemingly unusual sidebar logic without first determining why it exists. Several pieces of the implementation exist to work around RuneLite/Swing lifecycle and layout behavior.

## Java Formatting Preferences

Use conventional, compact Java formatting.

Keep short method calls and statements on one line.

Preferred:

    sidebarManager.refreshPanel(panel);

Do not unnecessarily format short calls like this:

    sidebarManager.refreshPanel(
        panel
    );

Split expressions/calls across multiple lines only when they are genuinely long or doing so materially improves readability.

Avoid excessive vertical formatting.

Do not perform formatting-only changes in files being modified for functional work.

## Working With Existing Code

The current repository is the source of truth for implementation details.

These instructions describe intent and historical context, but Codex must inspect the current source before making assumptions about:

- method names
- class responsibilities
- listeners
- sidebar state
- config keys
- component hierarchy
- startup/shutdown behavior

If these instructions and the current implementation appear inconsistent, investigate the discrepancy before changing code.

Do not blindly modify the implementation to match stale documentation.

## Debugging Approach

For UI bugs, determine the state transition that causes the problem before changing code.

For sidebar issues, consider the sequence:

    initial state
        ->
    plugin selected
        ->
    panel displayed
        ->
    sidebar resized
        ->
    user interacts with sidebar/panel
        ->
    plugin deselected or another plugin selected
        ->
    sidebar collapsed/resized/restored

Determine exactly which transition fails.

Prefer fixing the incorrect transition instead of continuously forcing the desired size/state on every UI event.

Avoid fixes that work only while the mouse/focus remains inside the icon pane or selected component.

## Regression Checks

Changes involving the sidebar should account for at least these scenarios:

- RuneLite starts with Sidebar Manager enabled.
- Sidebar Manager is enabled after RuneLite has already started.
- A plugin is selected.
- The selected plugin is clicked/deselected.
- Another plugin is selected.
- A narrow plugin panel is selected after a wide one.
- A wide plugin panel is selected after a narrow one.
- Sidebar is collapsed.
- Sidebar is reopened.
- An icon is hidden.
- A hidden icon is restored.
- Icons are reordered.
- RuneLite/plugin state changes dynamically.
- Sidebar Manager is disabled.
- RuneLite's original sidebar is restored.

Do not consider a sidebar UI fix complete based only on the first successful selection.

## Build and Verification

After meaningful Java changes, run the repository's Gradle build/tests appropriate to the project.

Do not claim a UI issue is fully fixed merely because the project compiles.

Compilation verifies the implementation builds; visual/interaction behavior still requires testing in RuneLite.

When handing a UI change back for testing, clearly state:

- which file(s) changed
- what behavior changed
- what specific interaction should be tested
- any regression cases particularly relevant to the change

## Scope Control

Do not make speculative architectural changes while fixing a small UI issue.

In particular, avoid:

- replacing the existing sidebar management architecture
- broad Swing refactors
- unrelated configuration redesign
- mass renaming
- dependency changes
- formatting entire files
- changing unrelated plugin behavior

unless explicitly requested.

A focused fix should produce a focused diff.

## Release Philosophy

The plugin is being prepared for an initial public release.

Prioritize:

- predictable behavior
- preserving user configuration
- clean startup/shutdown
- native RuneLite appearance
- avoiding regressions
- understandable user-facing configuration
- stability over adding unnecessary features

Do not add features solely because they are technically possible.

When evaluating remaining work for the initial release, distinguish between:

- actual bugs
- usability problems
- cosmetic polish
- optional future features

Do not block an initial release solely on optional enhancements.

## Communication Preferences

When explaining a proposed code change:

- Always name the exact file being edited before showing its code.
- Explain the reason for the change briefly.
- Prefer providing only the changed method/block when that is sufficient.
- Provide the entire file when explicitly requested or when multiple interconnected edits make a partial replacement error-prone.
- Clearly identify whether code is a replacement, addition, or deletion.
- Do not present speculative fixes as confirmed fixes.
- For UI issues, wait for actual RuneLite testing before treating the behavior as verified.

The developer prefers practical changes that can be pasted/tested directly over large theoretical explanations.
