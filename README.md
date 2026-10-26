# MartenWorld

An Android app: an LLM-driven, world-focused interactive story game with a
character-collection layer and, later, a settlement-management minigame.

- Design: [docs/DESIGN.md](docs/DESIGN.md)
- Plan: [docs/MILESTONES.md](docs/MILESTONES.md)
- Changes: [CHANGELOG.md](CHANGELOG.md)

## Building

Requires JDK 17 and the Android SDK (compileSdk 35).

```
./gradlew assembleDebug
```

Every push builds a debug APK in GitHub Actions. Open the run under the
**Actions** tab and download the `martenworld-debug-apk` artifact to install it
on a phone (allow installs from unknown sources for your browser or file
manager).
