<!-- Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause -->
# Plugin Hub review notes

This repository contains **Ayra's Pings only**: manually selected Flee, Go, Attack and Use communications. There are no boss trainers, encounter helpers, automated danger zones or other plugins in this source tree.

## Rules reviewed

Reviewed on 2026-11-23 against the current official sources:

- [Plugin Hub submission instructions](https://github.com/runelite/plugin-hub/blob/master/README.md)
- [Review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)
- [Rejected features and forbidden APIs](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Code conventions](https://github.com/runelite/runelite/wiki/Code-Conventions)
- [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
- [Packager's disallowed API list](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/resources/net/runelite/pluginhub/packager/disallowed-apis.txt)
- [Packager's standard build](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/resources/net/runelite/pluginhub/packager/standard-build.gradle)

## Gameplay and input

Each ping is a deliberate user action. Mouse button 4/5 primes one ping; the next left-click selects the tile/visible entity and dragging selects the action. Keyboard-modifier and ground-context-menu alternatives are available. Pings only draw visual communication and play a bundled chime. They do not walk, attack, use items, invoke game menu actions or send OSRS packets.

The plugin does not infer attacks, prayers, projectile impacts or safe/unsafe positions. There are no encounter-specific rules. Outlines apply only to explicitly selected NPCs, players, world objects and already rendered ground-item models. They expire with the ping and are dropped when the referenced local entity despawns or its index is reused. Existing game-disabled minimaps remain disabled.

## Networking and disclosure

The only networking is registration and sending of `TacticalPingMessage` through RuneLite's existing injected `WSClient` and `PartyService`. No custom server, HTTP endpoint, API key or paid service is used. There are no calls to `changeParty`, reconnect timers, background player-location streams or changes to other plugins' settings.

Sharing and receiving have independent settings. The sharing setting and README disclose the transmitted fields: clicked tile, world, instance/template coordinates, action, protocol version, random ping identifier, timestamp and optional manually selected target identity. Player targets include the selected player's normalized name and index. No account credentials, chat contents or equipment are collected.

Incoming messages require current party membership, matching world/plane/loaded scene, and matching instance/template coordinates. Target metadata is bounded and validated; actor identity and proximity are checked once before storing a local reference. Invalid, stale and duplicate messages are rejected. Sender/global rate limits and bounded active/history collections limit work and audio spam.

## Build, resources and validation

- Java 11 source/bytecode, BSD-2-Clause license, matching discovery properties/plugin descriptor, default-enabled installation behavior, and 48 x 48 icon.
- `build=standard`; no extra runtime dependencies or custom production build requirements.
- Uses `InterfaceID` packed widget IDs instead of the disallowed `WidgetInfo`/`WidgetID` API.
- No production reflection, native code, subprocesses, runtime downloads, filesystem reads/writes, custom Gson/HTTP clients, synthetic input, chat modification, `Client.menuAction` or `Client.runScript`.
- Original PCM WAV resources use RuneLite's injected `AudioPlayer` classpath-resource overload and shared executor. The audio synthesis source is an offline developer tool, excluded from the runtime JAR.
- Tests, preview rendering and the audio authoring tool are outside the production source set. No local updater, build output, reference screenshot or other plugin is committed.

Validation on 2026-11-23:

- 48 automated tests covering animation, target identity/expiry, packet validation/isolation/deduplication, scene lifecycle, minimap layouts/rotation/zoom, input priming/cancellation/immediate drag, dependency wiring and bundled chimes.
- Production compilation using the official standard-build dependency list against the Hub's RuneLite version, 1.13.8.
- Compatibility checks against RuneLite 1.13.7.
- Offline rendering using the actual Java painter. The user has also reported successful local sideload testing.

Independent two-client/raid-instance testing has not been performed by the automated checks. The upstream review and CI determine acceptance; this document does not claim RuneLite or Jagex approval.
