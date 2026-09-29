# Collaboration handoff

## Current production baseline

- Android app: v2.4.1
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

All 16 current `ch_*_l` asset IDs are UNVERIFIED.

The three mappings formerly marked HIGH in issue #3 / PR #4 were withdrawn after
the provenance of the local test master was rechecked. See
`docs/EVIDENCE_CORRECTION.md`.

Resolve the 16 assets against original serial/card/master identifiers using
evidence independent of the reconstructed test master.

Appearance-based guesses are explicitly insufficient.
