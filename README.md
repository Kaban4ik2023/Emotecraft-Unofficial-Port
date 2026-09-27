# Emotecraft (Unofficial Port)

An unofficial port of Emotecraft 2.4.12 to **Minecraft 1.12.2**, maintained by **Kaban4ik**.

## Download and install

[Download Emotecraft-Unofficial-Port-2.4.12.jar](https://github.com/Kaban4ik2023/Emotecraft-Unofficial-Port/raw/refs/heads/main/downloads/Emotecraft-Unofficial-Port-2.4.12.jar)

Requires **Forge 14.23.5.2859 or newer for Minecraft 1.12.2** and **Java 8**. Place the JAR in your `mods` folder. No separate Player Animator installation is needed.

Install the mod on both the server and clients to synchronize emotes. Without the server mod, emotes play locally.

## Features

- Emote wheel with three pages of eight slots and the original textures.
- Searchable library with icons, animated previews and individual key bindings.
- Import custom emotes and ZIP packs through the in-game file browser.
- Body and limb bending, armor animation and held-item transforms.
- Nine built-in emotes and support for accompanying PNG icons and NBS music.

## Controls

| Key | Action |
| --- | --- |
| Hold **B** | Open the emote wheel; release to play the selected emote. |
| **N** | Open the emote library and settings. |
| **G** | Stop the current emote. |

In the library, select an emote and left-click a wheel slot to assign it. Right-click a slot to clear it. Scroll over the wheel to switch pages. Double-click an emote to play it.

You can also place `.json`, `.emotecraft` or Quark `.emote` files in the game's `emotes` folder. Enable Quark support in settings when importing `.emote` files. Companion `.png` and `.nbs` files must have the same base name as the emote.

## Compatibility

This is an independent port, not an official KosmX release. The interface and features differ from newer Minecraft versions. Multiplayer emotes are limited to 32,767 serialized bytes; larger emotes play locally. Modern first-person effects, server emote catalogs and integrations with other mods are not included. OptiFine and shader compatibility has not been verified.

## Build

Set `JAVA_HOME` to a **JDK 8** installation, then run:

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. Built JARs are placed in `build/libs`. Use `gradlew runClient` to launch the development client.

## Credits and license

- **Kaban4ik** — unofficial Forge 1.12.2 port.
- **KosmX and Emotecraft contributors** — [original Emotecraft](https://github.com/KosmX/emotes), licensed under [GPL-3.0](LICENSE).
- [Player Animator](https://github.com/KosmX/minecraftPlayerAnimator) — animation core, licensed under [MIT](LICENSE-playerAnimator.txt).
