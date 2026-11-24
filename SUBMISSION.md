<!-- Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause -->
# Plugin Hub review notes

This repository contains **Ayra's Pings only**: 1–8 manually selected communication slots with configurable designs. There are no boss trainers, encounter helpers, automated danger zones or other plugins in this source tree.

## Rules reviewed

Baseline reviewed on 2026-11-23; customization and wheel changes checked on 2026-11-24 against the current official sources:

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

Sharing and receiving have independent settings. The sharing setting and README disclose the transmitted fields: clicked tile, world, instance/template coordinates, original action, slot number, RGB color, bundled symbol/sound names, custom text, protocol version, random ping identifier, timestamp and optional manually selected target identity. Player targets include the selected player's normalized name and index. No account credentials, chat contents or equipment are collected.

Incoming messages require current party membership, matching world/plane/loaded scene, and matching instance/template coordinates. Target metadata is bounded and validated; actor identity and proximity are checked once before storing a local reference. Invalid, stale and duplicate messages are rejected. Sender/global rate limits and bounded active/history collections limit work and audio spam.

Design data is a bounded optional protocol-1 extension. RGB values are restricted to 24-bit color, symbol/sound IDs resolve only to bundled enums, and text is limited to 64 Unicode code points without invisible controls or line breaks. Text is literal Java2D text; menu brackets are escaped. Missing, malformed or unknown designs use the original action design. Slots 5–8 retain a legacy Go action for older clients. No audio files, images, arbitrary asset paths or URLs are sent or loaded.

The sidebar uses collapsible groups for general settings and creates customization accordions only for enabled slots. The native configuration also groups general settings into four accordions; design fields remain persisted but hidden there, with no empty slot sections. Changing the count updates the sidebar and quick-click options immediately without resetting disabled designs. Own-group ConfigManager writes and public ClientToolbar/ColorPickerManager APIs are used; there is no reflection or modification of another plugin's UI.

Ping lifetimes are clamped to 800–60,000 ms on each receiving client. Long lifetimes use a prompt entrance and exit with a stationary hold, while the store still expires them at their full configured lifetime. Existing scene/party/despawn cleanup and bounded active markers remain in place.

The local wheel count does not restrict receiving slots from other players. Recipients may override sender designs with their own corresponding slot designs. Volume, mute, scene labels, reduced motion and outlines remain local controls. The wheel uses equal clockwise sectors: two halves, four quadrants, or two sectors per quadrant for eight. Counts 1–8 and configured quick-click slots are clamped; disabled slots cannot be sent.

## Build, resources and validation

- Java 11 source/bytecode, BSD-2-Clause license, matching discovery properties/plugin descriptor, default-enabled installation behavior, and 48 x 48 icon.
- `build=standard`; no extra runtime dependencies or custom production build requirements.
- Uses `InterfaceID` packed widget IDs instead of the disallowed `WidgetInfo`/`WidgetID` API.
- No production reflection, native code, subprocesses, runtime downloads, filesystem reads/writes, custom Gson/HTTP clients, synthetic input, chat modification, `Client.menuAction` or `Client.runScript`.
- Eight original PCM WAV resources use RuneLite's injected `AudioPlayer` classpath-resource overload and shared executor. The audio synthesis source is an offline developer tool, excluded from the runtime JAR.
- Tests, preview rendering and the audio authoring tool are outside the production source set. No local updater, build output, reference screenshot or other plugin is committed.

Validation on 2026-11-24:

- 76 automated tests covering animation, target identity/expiry, packet validation/isolation/deduplication, scene lifecycle, minimap layouts/rotation/zoom, input priming/cancellation/immediate drag, dependency wiring, all eight wheel counts, shared design/slot round trips and fallbacks, text validation, listener overrides, audio controls and all bundled chimes, conditional settings panels and saved values, accordion state, toolbar lifecycle, and minute-long ping expiry/animation.
- Production compilation using the official standard-build dependency list against the Hub's RuneLite version, 1.13.8.
- Compatibility checks against RuneLite 1.13.7.
- Offline rendering using the actual Java painter. The user has reported successful sideload testing of the earlier 1.1.0 release; the new customization is covered by automated client fixtures.

Independent two-client/raid-instance testing has not been performed by the automated checks. The upstream review and CI determine acceptance; this document does not claim RuneLite or Jagex approval.
