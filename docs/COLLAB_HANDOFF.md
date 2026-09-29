# Collaboration handoff

## Current production baseline

- Android app: v2.3.0
- package: `com.openai.threekingdoms`
- current source of truth: `app/src/main/java/com/openai/threekingdoms/`
- original art is optional/local and not committed

## Research → integration gate

1. Researcher records a candidate in `ASSET_MAPPING.csv`.
2. Evidence must point to an original APK/master/resource relationship.
3. A second pass independently checks the evidence.
4. Only HIGH-confidence mappings enter production Java data.
5. Build and ADB regression follow every production integration.

## Current priority

Resolve the 16 `ch_*_l` asset IDs in `OriginalArtData.java`
against original character/card/master identifiers.

Appearance-based guesses are explicitly insufficient.
