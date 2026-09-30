# Reconstructed Master v1

This file is the canonical data source for the offline rebuild:

`app/src/main/assets/data/reconstructed_master_v1.json`

It is **not** claimed to be the lost original server Master.

## Why reconstruction is now the primary path

Reverse engineering of the original Taiwan 1.2.5.2 client confirmed this pipeline:

```text
master*.bin
  -> zlib/deflate
  -> JSON
  -> RFMasterDataManager
```

The original asset hosts are no longer resolvable. Wayback did not preserve the
Master payload, and the useful Common Crawl search did not yield the asset hosts.
No local or Library copy of `master.bin`, `masterN.bin`, or `list.bin` was found.

Development therefore proceeds with a provenance-first reconstructed Master
rather than inventing an unverifiable "original" payload.

## Confidence rules

- VERIFIED: directly supported by original APK/parser/native evidence or independently checked public game data.
- RECONSTRUCTED: required for a playable rebuild, but the exact original value/algorithm is unavailable.
- PARTIAL: identity or some fields are verified while important details remain unknown.
- UNKNOWN: insufficient evidence.

## v1 contents

- 6 character records, record-level confidence PARTIAL: five have `card_no: null`
  (no evidenced link to public card data) and no per-field source is stored in
  this repository; stat/skill VERIFIED labels are maintainer-attested
- 3 early Cao Cao route stages
- enemy ATK/TURN verification flags (maintainer-attested; one enemy downgraded
  to PARTIAL, see `CLAUDE_MASTER_V1_AUDIT.md`)
- `unit_advantage` / `faction_advantage` are PARTIAL: production stages use
  stage-specific advantage pairs that differ from the global default
- explicitly reconstructed HP/DEF, rewards, encounter weighting, and seed/drop defaults
- 0 verified original-art identity mappings

## Gate

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/validate_reconstructed_master.ps1
```

A Master change must pass this validator before merge.

## Runtime numeric integration

At Activity startup, `applyRoster` loads canonical character numeric endpoints,
max levels and skill cooldowns into the five matching existing playable cards.
Binding requires an explicit offline `runtime_roster_id`, exact name + variant,
symbolic faction/troop agreement, and
an unchanged active skill name. Parser troop integer codes are never reused as
runtime codes. Existing save IDs, rarity presentation, skill execution, leader
logic and gameplay compatibility flags stay unchanged. Those compatibility
flags are not independent provenance evidence (see Claude audit).

張梁 remains research-only: no playable ID is inferred or assigned.
The other five legacy test characters retain their fallback Java records.
All rows are prepared before the roster is committed; rejected input logs an
error and retains the previous roster without a partial update. This is a
numeric migration, not a claim that skills/stages are fully data-driven.

Host regression: `python tools/test_roster_loader.py --json-jar <org.json jar>`
with JDK 17; optional `--compiler-jar <ecj jar>` supports a JRE-only host.
It checks canonical load, a changed JSON HP value, enum mismatch rejection,
atomic rollback and stable save IDs. Android stubs are restricted to tests.
APK compilation and device validation are still required before release.

## Canonical file decision

`reconstructed_master_v1.json` is the only runtime Master and the target of the
canonical validator/Android build. `master_reconstruction_v1.json` is a legacy
alternative draft schema, retained for migration comparison only. Its separate
PowerShell verifier checks that draft, not active runtime readiness. Never use
its data to override the canonical file implicitly.

`runtime_roster_id` is an offline save ID, not an original serial/card number or
art identity link. Required playable rows are IDs 1, 3, 4, 7, 9; each must appear
exactly once and agree with the existing identity. 張梁 has null runtime ID and
stays research-only. Reordered rows work; renamed/missing rows reject the full
update. Enum keys are defined beside GameData constants and use those constants.

Saved levels are bounded for gameplay/UI by the current max level. Reading an
old level above that cap preserves the original preference value so a later cap
increase can restore it. `addExp` also starts from the bounded level. Existing
stat interpolation already clamps its input.

`tools/android_smoke.py` runs only on an explicitly named Android emulator and
clears that emulator's app data. It checks launch, visible Master load status,
team swap/save, a real board drag, process restart, saved team and legacy level
cap handling. It refuses physical-device serials.
