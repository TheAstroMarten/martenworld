# MartenWorld — Design Document

Status: draft v0.1 (initial direction). This document captures the agreed direction
for the project so that implementation decisions can be checked against it. It will
change; when it does, bump the version and note what changed at the bottom.

## 1. What this is

MartenWorld is an Android app: an LLM-driven, world-focused interactive story game
with a light character-collection layer and, later, a loosely coupled
settlement-management minigame.

It is **not** a chat client with character cards bolted on. The organizing unit is
the **World**. A world carries its own setting, lore, cast of characters, and story
state. The player assembles a **party** from the world's cast and plays scenes in
that world. The language model writes the story; the app owns the structure.

The design is **setting- and system-agnostic**. Nothing in the data model, UI copy,
or default prompts assumes a genre, a ruleset, dice, stats, or combat. A world can
be anything its author writes.

## 2. Principles

1. **World first.** Everything belongs to a world. Characters, lore, story state,
   and (later) settlements are scoped to one.
2. **Structure over free text.** Characters are structured sheets with typed
   fields, not prose cards. The app renders them into prompt text; the same fields
   later drive game systems. Free text is for description and voice only.
3. **The model narrates, the app governs.** The LLM never owns authoritative state.
   The app decides what goes into context, keeps story state, and applies any
   structured changes the model proposes.
4. **Transparent prompts.** Every request is built from modular, user-editable
   blocks, and the user can preview the exact payload before (or after) it is sent.
5. **Local-first, bring your own model.** All data lives on the device. The user
   supplies an OpenAI-compatible endpoint and key. No account, no server of ours.
6. **Your data is files.** Every meaningful object is a separate, human-readable
   file that can be exported, backed up, diffed, and re-imported independently.
7. **Mobile-native UX.** Designed for one-handed phone use: bottom navigation,
   large touch targets, streaming text that stays readable, offline-tolerant.

## 3. Core loop (first playable)

1. Open a world → see its cast and current story state.
2. Pick a party (up to 5 companions) alongside the player character.
3. Enter a scene. The app assembles context from world, party, lore, and story
   state and streams the model's narration.
4. The player responds in free text (and later, via structured choices).
5. Repeat. Periodically the app summarizes older history into story state so the
   scene can continue indefinitely within the context budget.

Collection (finding and recruiting new cast members through play) and the
settlement minigame (assigning cast to structures by aptitude) come later and
read from the same character data.

## 4. Data model

All identifiers are UUIDs. Every top-level object carries `schemaVersion` so saves
can be migrated. Dates are ISO 8601.

```
World
├── world.json             identity, description, setting prompt, settings
├── lorebook/
│   └── <entry-id>.json    keyword- or always-triggered lore entries
├── characters/
│   └── <character-id>.json   companion sheets (one file each)
├── player/
│   └── <player-id>.json      player character sheet(s), separate schema
├── sessions/
│   └── <session-id>/
│       ├── session.json   party composition, story state, settings snapshot
│       └── messages.jsonl one message per line, append-only
└── assets/
    └── <id>.<ext>         portraits and other images, referenced by id

App-level (not world-scoped)
├── prompts/
│   └── <preset-id>.json   modular prompt presets (block order, toggles, text)
└── connections/
    └── <connection-id>.json  endpoint URL, model id, params (key in Keystore)
```

### 4.1 World

Identity (id, name, tagline, author), a cover image id, a **setting block** (the
world's own prose: premise, tone, rules of the fiction), tags, and world-level
defaults (which prompt preset to use, max party size, default player character).

### 4.2 Companion character sheet

Companions and the player character use **different schemas** because they play
different roles in prompts and in the game. A companion sheet is designed to be
read by both the LLM (rendered to prose) and by game systems (typed fields).

| Field | Type | Purpose |
|---|---|---|
| `id`, `schemaVersion` | | identity, migration |
| `name`, `title` | string | display and prompt |
| `portrait` | asset id | UI |
| `summary` | short text | one-line description for lists and compact prompts |
| `description` | text | appearance and background, for full prompts |
| `voice` | text | how they speak; style guidance for the model |
| `traits` | list of strings | personality tags; game systems may key off them |
| `aptitudes` | map string → number | e.g. `{"craft": 3, "lead": 1}`; keys are defined by the world, not the app |
| `relationships` | list of {characterId, label, note} | ties to other cast members |
| `tags` | list of strings | author-defined filtering |
| `recruitment` | {state, condition?} | `available` / `recruited` / `locked`; collection layer |
| `custom` | map | author extensions the app passes through verbatim |

Aptitude keys and trait vocabularies are **world-defined** so the app never ships
a genre. The world may declare the vocabulary in `world.json` for UI purposes.

### 4.3 Player character sheet

Smaller and framed from the player's point of view: `name`, `pronouns`,
`portrait`, `description`, `goals`, `notes`, `custom`. No aptitudes or recruitment.
The prompt builder renders this as "the protagonist" rather than as a cast member.

### 4.4 Lorebook entry

`title`, `content`, `keys` (trigger words/phrases), `alwaysOn`, `priority`,
`scanDepth` (how many recent messages to scan), `caseSensitive`, `enabled`,
`tags`. Entries can also be bound to characters so they activate only when that
character is in the party.

### 4.5 Session and story state

A session is one playthrough in a world. It stores the party (ordered list of
companion ids plus the player id), a snapshot of the prompt preset and connection
used, and the **story state**:

- `scene` — where and when the current scene is
- `threads` — open plot threads or objectives, each with a short status
- `summary` — rolling summary of everything that has scrolled out of context
- `notes` — author/player pinned facts that must always be present

Story state is editable by the player at any time. The app proposes updates to it
during summarization; later, the model may be asked to propose updates as
structured output that the app validates before applying.

### 4.6 Messages

Append-only JSONL. Each message: `id`, `role` (`player` | `narrator` |
`character` | `system`), `speakerId` (for character lines), `content`,
`createdAt`, `swipes` (alternative generations), `activeSwipe`, `tokenEstimate`,
`meta` (model, finish reason, latency). Edits create a new version rather than
overwriting, so history is auditable.

## 5. Prompt system

### 5.1 Modular blocks

A **prompt preset** is an ordered list of blocks. Each block has a `type`, an
`enabled` flag, a `role` (system / user / assistant), and either literal text
with placeholders or a reference to a generated block. Built-in block types:

- `instructions` — author text (style, format, pacing). Supports placeholders
  like `{{world.name}}`, `{{player.name}}`, `{{party.names}}`.
- `world` — renders the world's setting block
- `player` — renders the player sheet
- `party` — renders each party member's sheet (compact or full)
- `lore` — triggered and always-on lorebook entries
- `storyState` — scene, threads, summary, notes
- `history` — recent messages, newest last, trimmed to budget
- `authorNote` — a short text inserted at a configurable depth from the end
- `custom` — any literal text

Blocks are reorderable and individually toggleable. Presets live at app level and
can be exported/imported as single JSON files. A world can name a default preset.

### 5.2 Context assembly

Given a token budget (from the connection's context size minus a reserve for the
response), the builder:

1. Renders all enabled non-history blocks and measures them.
2. Scans the last `scanDepth` messages for lorebook keys; includes matches by
   priority until the lore budget cap is reached.
3. Fills the remainder with history, newest first, dropping whole messages from
   the oldest end.
4. If history had to be dropped and the oldest dropped message is newer than the
   last summarization point, flags the session for a summary pass.

Token counting uses a local estimator (a tokenizer bundled for common families,
with a character-based fallback), and the preview shows the estimate per block.

### 5.3 Preview

Before sending, the user can open **Preview** and see the exact message array that
will be posted, block by block, with token estimates, which lore entries fired and
why, and what was trimmed. After sending, the same view is available from the
message's menu, showing what was actually sent. This is a first-class screen, not
a debug toggle.

### 5.4 Model interaction

Phase one is plain text generation with streaming. Each turn is one request;
multi-character replies are produced by instructing the model to write the scene
including party members' lines, attributed by name, which the app parses into
`character` messages where unambiguous. Later phases may add structured output for
story-state updates and recruitment events.

## 6. LLM connectivity

- **OpenAI-compatible chat completions** (`/v1/chat/completions`) with SSE
  streaming. This covers OpenAI, OpenRouter, Together, Groq, local servers
  (llama.cpp, Ollama, LM Studio, KoboldCpp's OAI endpoint), and most proxies.
- Multiple saved **connections**: base URL, model id, context size, sampling
  params, custom headers. API keys are stored in Android Keystore-backed encrypted
  storage, never in exported files.
- A **model list** fetch (`/v1/models`) where the endpoint supports it; manual
  entry otherwise.
- Clear error surfaces: HTTP status, provider error text, and a retry path.
- Future: on-device inference through the same interface.

## 7. Mobile UX

Bottom navigation with three tabs: **Worlds**, **Play**, **Settings**.

- **Worlds**: list of worlds with cover and tagline; world detail with tabs for
  Cast, Lore, Sessions, and About (setting text). Create/edit everything inline.
- **Cast**: grid of portraits; filter by tag or recruitment state; long-press to
  add to party. Sheet editor uses grouped sections with sensible keyboard types.
- **Party**: a horizontal strip at the top of the Play screen showing the player
  plus up to five companions; tap a portrait for their sheet, drag to reorder.
- **Play**: the scene. Streaming text, speaker labels and portraits for character
  lines, swipe left/right on a message for alternatives, long-press for edit,
  regenerate, continue, delete, and "show what was sent". Composer pinned to the
  bottom with a send/stop button. Story state is one tap away in a bottom sheet.
- **Settings**: connections, prompt presets, appearance, export and backup.

Conventions: Material 3, dark theme only for now with orange as the single accent
(more theming later), text scaling honored,
no horizontal scrolling of prose, state survives rotation and process death,
generation continues in a foreground service so leaving the app does not cancel it.

## 8. Export, backup, and import

- Every object is already a file (section 4), so **export** is a matter of
  copying. Three granularities, all through the Android share sheet or a chosen
  folder (Storage Access Framework):
  1. **One character** — a single `.json` (plus its portrait if requested).
  2. **One world** — a `.zip` of the world folder, optionally without sessions.
  3. **Everything** — a `.zip` of all worlds plus app-level presets. Keys are
     never included.
- **Import** mirrors export: a character json into a chosen world, a world zip,
  or a full backup. Conflicts are resolved by id with a prompt to replace, keep
  both, or skip.
- Optional **automatic backup** to a user-chosen folder (e.g. a synced cloud
  folder) on a schedule.
- All files are pretty-printed JSON with stable key ordering so they diff well in
  version control.

## 9. Tech stack

| Concern | Choice | Why |
|---|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 | Best fit for a streaming, text-heavy mobile UI; first-class Android |
| Min SDK | 26 (Android 8.0) | Covers nearly all active devices; modern APIs without compat shims |
| Architecture | Single module to start, MVVM with unidirectional state; split into feature modules when it hurts | Avoid premature structure |
| Persistence | Files as the source of truth (section 4) + Room as a query/index cache rebuilt from files | Keeps "your data is files" honest while giving fast lists and search |
| Serialization | kotlinx.serialization with explicit schema versions | Multiplatform-friendly, no reflection |
| HTTP | Ktor client (OkHttp engine) with SSE | Streaming support, clean coroutines |
| Images | Coil | Compose-native loading and caching |
| DI | Hilt | Standard, boring |
| Background | WorkManager for backups; foreground service for in-flight generation | |
| Secrets | EncryptedSharedPreferences / Keystore | API keys stay on device |
| Testing | JUnit + Turbine for view models; Compose UI tests for the play screen; golden tests for prompt assembly | Prompt assembly is the riskiest logic and the most testable |
| CI | GitHub Actions: lint, unit tests, assemble debug APK on every push; attach APK to the run and to tagged releases | Enables development and testing without a desktop |

### Why not Flutter, React Native, or Godot

Flutter would be the only serious contender and only pays off if iOS is a goal.
React Native adds a JS runtime for no gain here. Godot is a poor fit for a UI that
is mostly text, lists, and editors.

## 10. Development workflow

- Trunk-based: short-lived branches, PRs into the default branch, squash merges.
- Every push builds a debug APK in CI. Install it on a phone directly from the
  Actions run (or from a release tag) — no desktop needed.
- Conventional commit messages; a `CHANGELOG.md` updated per milestone.
- Design changes are made by editing this document in the same PR as the code.

## 11. Out of scope for now

Multiplayer, cloud sync, accounts, monetization, iOS, in-app model downloads,
image generation, voice. None of these are ruled out; they are simply not part of
the first three milestones.

## 12. Open questions

- Should the player be able to have multiple player characters per world, or one
  per session? (Current lean: one per session, chosen from the world's `player/`
  folder.)
- How much of the companion sheet should be visible to the model by default: full
  sheet for everyone in the party, or compact for all but the "focus" character?
- Whether to adopt a structured-output protocol for story state in milestone 2 or
  defer until the minigame needs it.

## Changelog

- v0.1 — initial direction agreed in project discussion.
