# Collaboration handoff

## Current production baseline

- Android app: v2.4.0
- package: `com.openai.threekingdoms`
- current source of truth: `app/src/main/java/com/openai/threekingdoms/`
- art mapping production file: `app/src/main/java/com/openai/threekingdoms/OriginalArtData.java`
- original art is optional/local and not committed

## Research → integration gate

1. Researcher records a candidate in `ASSET_MAPPING.csv`.
2. Evidence must point to an original APK/master/resource relationship.
3. A second pass independently checks the evidence.
4. Only HIGH-confidence mappings enter production Java data.
5. Build and ADB regression follow every production integration.

## Current priority

Three of 16 `ch_*_l` asset IDs are now HIGH-confidence and integrated:
- `ch_15200025_l` → No.102 → 〖大斧〗徐晃
- `ch_35300032_l` → No.260 → 于禁
- `ch_56400035_l` → No.350 → 成公英

Resolve the remaining 13 against original character/card/master identifiers.

Appearance-based guesses are explicitly insufficient.
