# Pokémon Money Pack Opener (Fabric, Minecraft 26.1.2)

A **client-side** Pokémon booster-pack simulator with animated reveals, rarity glow, big
Legendary/Mythical cinematics, inventory/history and a simulated "Sell All".

> **It is a simulator.** It never sends packets, never reads or writes your DonutSMP balance and
> never talks to the server. Every "$" shown is a configurable display number.
> Check DonutSMP's rules on client mods yourself; this mod is purely visual and sends no input to the server.

## Use
* Press **O** (rebind in Options → Controls → Key Binds → *Pokémon Money Pack Opener*).
* **Open 1x / 5x / 10x**. Odds and $ ranges are always shown on the left.
* **Click or Space** skips the animation (rewards are already saved when the pack opens).
* **Inventory & History**: scrollable lists (history keeps the last 1000 pulls).
* **Sell All**: totals your inventory, adds it to "Sold (simulated)", clears the inventory.
* **Reload Config** re-reads the JSON without restarting.

## Config – `config/pokemon_pack_opener.json`
| Key | Meaning |
|---|---|
| `rarities.<tier>.weight` | relative odds (normalised to 100%). Defaults 15/25/25/20/12/3 |
| `rarities.<tier>.min / max` | sell-value range in $ (exact value is rolled in this range) |
| `priceMultiplier` | scales every min/max, handy to match real /sell prices quickly |
| `priceOverrides` | exact $ for one Pokémon, e.g. `"Pikachu": 42000` (ignores multiplier) |
| `valueRounding` | rolled values are rounded to this step (default 100) |
| `soundEnabled`, `soundVolume` | audio |
| `flashEffects`, `screenShake` | turn off for photosensitivity / comfort |

Your data lives in `config/pokemon_pack_opener_data.json`.

## Build
Requires **JDK 25**.

The easy, safest route (it also gives you the Gradle wrapper):
1. Generate a blank Fabric template for **26.1.2** at https://fabricmc.net/develop/template/ (no extras needed).
2. Copy this project's `src/` folder over the template's `src/`, replacing `fabric.mod.json`.
3. `./gradlew build` → jar in `build/libs/`. Drop it into `mods/` with Fabric Loader ≥ 0.19.1 and Fabric API.

Or build this folder directly (`build.gradle`, `gradle.properties` included): run `gradle wrapper`
once, then `./gradlew build`.

## Notes
* Each Pokémon has a unique procedurally generated pixel-art badge tinted by its types (no
  copyrighted sprites are shipped). To use real art, replace `Gfx.icon()` with texture blits.
* All sounds are vanilla sound events.

## Build without installing anything (GitHub Actions)
1. Create a free GitHub repo and upload everything in this folder (including the hidden `.github` folder).
2. Open the repo's **Actions** tab, wait for "Build mod" to finish (a few minutes).
3. Open the finished run, download the **pokemon-pack-opener-jar** artifact, unzip it.
4. Put `pokemon-pack-opener-1.0.0.jar` (not the `-sources` one) in `mods`, with Fabric API.
