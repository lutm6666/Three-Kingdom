# Claude audit: reconstructed Master v1 (issue #9)

Scope: `reconstructed_master_v1.json`, `RECONSTRUCTED_MASTER_V1.md`,
`EVIDENCE_CORRECTION.md`, `tools/validate_reconstructed_master.ps1`, and
`StageData.java` (read-only, for cross-reference). No Java, Gradle, workflow,
image or binary was modified. The validator was not executed (PowerShell/Python
were not available in this run); results below come from manual reading.

## Confirmed OK

- **Art identity (goal 3):** `art_assets.identities_verified = 0`; all six
  characters have `art_asset_id: null` and `art_identity_confidence: UNKNOWN`.
  The validator enforces the 0.
- **Retracted mappings (goal 4):** no `ch_*` serial, sprite ID, or card number
  102 / 260 / 350 appears in the JSON. The only `card_no` is 64 (張梁), which is
  not one of the withdrawn mappings and has no art link.
- **Stage split (goal 5):** HP/DEF, rewards, encounter rule (`equal`, 1-2) are
  RECONSTRUCTED everywhere. The flags match the `attackVerified`/`turnVerified`
  arguments in `StageData.java` for every enemy, and the pools/boss waves match
  Java (names, HP, ATK, TURN, DEF, faction).
- Seed and drop rate are RECONSTRUCTED; enhancement curve `Next.5` and low-rarity
  base growth are RECONSTRUCTED.

## Findings and corrections

1. **Characters over-labelled.** All six records were `VERIFIED` and the doc said
   "fully verified identity/stat endpoints". Five have `card_no: null`, and the
   repository stores no source for any stat or skill. Record-level confidence is
   now `PARTIAL` with an `identity_note`. Per-field stats/skills remain
   `VERIFIED` only as maintainer-attested; `meta.audit_note` says so. If the
   maintainer can cite the source, record it per field; otherwise downgrade.
2. **Circularity risk.** The stage ATK/TURN "VERIFIED" flags are the same flags
   that exist in production Java, which is itself part of this reconstruction.
   The repo contains no independent original source for them, so they are
   unauditable, not disproven. Left as-is but flagged in `audit_note`.
3. **Faction advantage was VERIFIED but contradicted by production.**
   `unit_advantage`, `mechanics.faction_advantage` said VERIFIED for the global
   `WEI>WU>SHU>WEI, HAN<>QUN` cycle. `StageData.java` uses stage-specific pairs
   (`HAN>WEI, WEI>QUN` / `WEI>QUN, QUN>HAN` / with cycles) that the JSON does not
   record. Downgraded to `PARTIAL` with a note.
4. **`enhancement` group was VERIFIED while containing a RECONSTRUCTED child**
   (`low_rarity_material_base_growth`). Group downgraded to `PARTIAL`.
5. **Suspicious value:** stage `cao_guangzong_01` 青秘玉・左慈 has ATK 20 with
   550 HP, while other enemies in that pool have ATK 48-191. It equals the
   reconstructed stage-1 左慈 ATK (20). ATK/TURN downgraded VERIFIED → `PARTIAL`
   pending source.
6. **`composition.confidence: VERIFIED` scope was ambiguous.** It covers only
   boss-wave names/count (plus boss ATK/TURN in stage 3). Added a `scope` note.
   For `cao_iron_gate_01` the boss 張寶 ATK/TURN are RECONSTRUCTED although the
   composition is VERIFIED; this is consistent given the scope.
7. **Unlabelled fields:** `difficulty`, `stamina`, `wave_count` carry no
   confidence label (listed under `unlabeled_fields`). Java notes claim the
   wave count is from the original; the others have no stated source.
8. Wording overstatements fixed in `RECONSTRUCTED_MASTER_V1.md`.

## Validator gaps (not changed; outside allowed files)

- Does not reject a `VERIFIED` parent containing RECONSTRUCTED children.
- Does not check `art_asset_id` stays null, or `identities_verified` against
  actual mapped assets.
- Does not require `VERIFIED` characters to have `card_no`/source.
- Does not validate `advantage`/`enhancement` confidence fields or stage
  `composition` content against Java.
- After this audit `VERIFIED_CHARACTERS` prints 0; the validator still passes
  (all values used are in the allowed set).

## Not done

No original values were added or invented.
