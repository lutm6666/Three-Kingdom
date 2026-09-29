# Three Kingdoms Puzzle Rebuild

Offline Android reconstruction/research project inspired by the discontinued
《三國志拼圖大戰 / 三国志パズル大戦》.

Current app version: **v2.4.1**.

## Current scope

- 6×5 drag puzzle board, combos and cascades
- faction / troop-type combat rules
- multi-enemy battles and enemy action engine
- early quest reconstruction with source-confidence markers
- deterministic encounter/drop seeds for regression
- drop-card inventory and enhancement prototype
- optional local import of original APK artwork

## Build

Use Android Studio, or on the original Windows environment run:

```bat
build-debug.bat
```

Original commercial-game PNG files are intentionally not committed.
See `tools/import_original_art.ps1`.

## Collaboration

Claude research and ChatGPT integration share evidence through `docs/`.
Do not promote guessed mappings into production data.
