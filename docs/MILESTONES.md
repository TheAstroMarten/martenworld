# MartenWorld — Milestones

Each milestone ends with an installable debug APK from CI and a short demo you can
run on a phone. Items are deliberately small; estimates assume part-time evenings.

## M0 — Skeleton (about 1 week)

Goal: an app that installs, opens, and proves the pipeline end to end.

- [ ] Android project: Kotlin, Compose, Material 3, Hilt, min SDK 26
- [ ] GitHub Actions: lint + unit tests + debug APK artifact on every push
- [ ] Bottom navigation shell: Worlds / Play / Settings (placeholders)
- [ ] Connections screen: add an OpenAI-compatible endpoint, store key securely,
      fetch model list, "test connection" button
- [ ] Minimal streaming test: send one message, stream the reply to the screen
- [ ] `docs/DESIGN.md` and this file committed; `CHANGELOG.md` started

Demo: install APK, add your endpoint, see a streamed reply.

## M1 — First playable scene (about 3 to 4 weeks)

Goal: create a world, write a few sheets, pick a party, and play a scene with
lore and a transparent prompt.

**Data**
- [ ] File layout from DESIGN §4 with schema versions and a migration hook
- [ ] World create/edit (name, tagline, setting block, cover)
- [ ] Companion sheet create/edit (all fields in DESIGN §4.2)
- [ ] Player sheet create/edit (DESIGN §4.3)
- [ ] Lorebook entries create/edit, keyword and always-on
- [ ] Room index rebuilt from files on launch and on change

**Prompting**
- [ ] Prompt presets: ordered, toggleable blocks with placeholders; one sensible
      default preset shipped, genre-neutral
- [ ] Context assembler: render blocks, trigger lore, trim history to budget
- [ ] Token estimator with per-block estimates
- [ ] **Preview** screen showing the exact message array before sending, and
      "show what was sent" on any past message

**Play**
- [ ] Session create: choose player character and up to five companions
- [ ] Party strip with reorder
- [ ] Streaming scene view with speaker attribution parsed from the reply
- [ ] Message actions: regenerate, swipe between alternatives, edit, delete,
      continue, stop generation
- [ ] Story state bottom sheet: scene, threads, summary, notes, all editable
- [ ] Generation survives leaving the app (foreground service)

**Export**
- [ ] Export one character as JSON; import a character JSON into a world
- [ ] Export a world as a zip

Demo: hand-write one world with 3 to 5 companions and a player, play a ten-turn
scene, open Preview and see every block and which lore fired.

## M2 — Living story (about 3 weeks)

Goal: scenes that run long and a world that remembers.

- [ ] Automatic summarization into story state when history is trimmed, with a
      review step before the summary replaces history
- [ ] Author's note block at configurable depth
- [ ] Character-bound lore entries (only fire when the character is in the party)
- [ ] Multiple sessions per world; resume any
- [ ] Prompt preset import/export; world default preset
- [ ] Full backup and restore; optional scheduled backup to a chosen folder
- [ ] Portrait assets and image import
- [ ] Polish: text scaling, dark theme audit, error states, empty states

Demo: a session that runs well past the context window without losing the plot;
export everything, wipe the app, restore, continue.

## M3 — Collection (about 2 to 3 weeks)

Goal: finding companions becomes part of play.

- [ ] Recruitment state on sheets (available / recruited / locked) with conditions
- [ ] Structured-output protocol: the model can propose `recruit`, `storyState`
      updates, and `threads` changes; the app validates and asks before applying
- [ ] Cast screen shows locked and recruited states; recruiting through play
- [ ] Per-session cast state separate from the world's authored cast

Demo: meet a new character in a scene and recruit them into the party.

## M4 — Settlement minigame (later)

Goal: a loosely coupled management layer that reads aptitudes from sheets.

- [ ] World-defined structures and aptitude keys
- [ ] Assign cast members to structures; simple per-day resolution
- [ ] Events from the minigame can seed story state (optional hooks)

Scope and design to be decided when M3 is done.

## Parking lot

Ideas noted but not scheduled: on-device inference, image generation for scenes,
voice input, import converters for other character formats, sharing worlds as
single-file bundles, multiple player characters per world.
