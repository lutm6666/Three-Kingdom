# Claude audit: v2.5 Master Reconstruction provenance and migration (issue #12)

Baseline: `main` @ `c74c113` (v2.5.0). Read-only audit; no Java, Gradle,
workflow, image or binary was changed. No art asset was identified (all 16
`ch_*_l` remain UNVERIFIED; nothing below relies on appearance). Build/tests
were not run. The PowerShell validators were not executed (no PowerShell here);
all results are from reading.

Labels: **OV** = ORIGINAL_VERIFIED, **RC** = RECONSTRUCTED, **CU** = CUSTOM,
**UNK** = UNKNOWN / needs evidence.

Evidence available in the repo for *any* original value: none. The repo states
the original `master.bin` was never recovered (`RECONSTRUCTED_MASTER_V1.md`,
`EVIDENCE_CORRECTION.md`). No document cites a wiki URL/card No. for a
GameData value. Under the issue's rule (direct APK/native evidence, or an
independent public source *for that exact value*) **no numeric value in
`GameData.java` currently meets the bar from repo contents alone.** What the
repo does support is only structural: `libgame.so` resource templates and the
`master*.bin -> zlib -> JSON` pipeline (`EVIDENCE_CORRECTION.md`), which carry
no stat, skill or enemy values.

## 1. Headline findings

1. **The v2.5 file `master_reconstruction_v1.json` is not used by the app.**
   Nothing in `app/src/main/java` references it; only
   `tools/verify_reconstruction_master.ps1` does. `GameData.ROSTER` is still the
   live source. `ReconstructedMasterData` reads the *other* file
   (`reconstructed_master_v1.json`) and only produces a validity summary; it
   feeds no gameplay value. So "Master layer exists" is true only on disk.
2. **Two masters disagree on the same generals.** The five ORIGINAL_VERIFIED
   generals in `master_reconstruction_v1.json` are the same five that the issue
   #9 audit had already downgraded to record-level `PARTIAL` in
   `reconstructed_master_v1.json` (`card_no: null`, no per-field source). The
   v2.5 file re-promotes them to `ORIGINAL_VERIFIED` with no new evidence
   (regression of the #9 correction). It also drops `card_no`, `identity_note`,
   per-field confidence and the `PARTIAL` marks on 趙雲 / 周瑜 leader skills.
3. **Provenance is one flag per general** (`DataProvenance` on the whole
   record), so a mechanic and its value cannot be classified separately, which
   the issue requires.
4. **The `verifiedOriginal` flag changes gameplay math**, not just labels:
   `PlayerData.effectiveHp/Atk/Recovery` (`PlayerData.java:260-279`) use linear
   Lv1→Max interpolation for OV and `+55 HP / +12 ATK per level` for RC;
   `GameData.leaderHpMultiplier/leaderDamageMultiplier/leaderHealMultiplier`
   gate the invented leader buffs on `!verifiedOriginal`. Downgrading a general
   therefore changes its stats in play; this must be handled in migration
   (see §5).
5. **Troop-type ID conflict.** `master_reconstruction_v1.json` (`troop_types`)
   uses SWORD=0, CAVALRY=1, SPEAR=2, BOW=3, BARBARIAN=4, GUI_MOU=5, SHEN_SUAN=6
   (same as `GameData`) and labels them all ORIGINAL_VERIFIED, while
   `reconstructed_master_v1.json` `enums.original_parser_troop_types` records
   the original parser order BOW=0, SWORD=1, SPEAR=2, CAVALRY=3, SHEN_SUAN=4,
   GUI_MOU=5, BARBARIAN=6. The numeric IDs cannot be both original. The
   *names* of the seven troop types are plausible OV (parser enum); the *IDs*
   in `GameData` are **CU** (internal). Same for faction IDs (WEI=0…QUN=4 is
   documented in the other JSON as the enum, but no source is stored).
6. **The two JSON files use different schemas and different validators**
   (`tools/validate_reconstructed_master.ps1` vs
   `tools/verify_reconstruction_master.ps1`); the second only checks ranges and
   the provenance vocabulary and cannot detect any of the above.

## 2. Audit of every `DataProvenance.ORIGINAL_VERIFIED` in `GameData.java`

Five generals are OV (`GameData.java:193,203,208,223,233`). For each: repo
evidence = none beyond maintainer attestation (Javadoc "source-backed public
game database entry", `DataProvenance.java:6-7`); the entry is not cited.
`sourceVariant` / rarity strings look like wiki card-face text but no URL or
card No. is recorded, so identity linkage (card_no) is also unrecorded.

| id | General | Field | Value(s) | Evidence in repo | Recommendation |
|---|---|---|---|---|---|
| 1 | 關羽 美髯公 | provenance | OV | attestation only | **Downgrade to RC** until cited |
| | | Lv1/Max HP,ATK,REC | 520/300/60, 3208/1230/191 | none | UNK-attested; RC |
| | | max level 99, rarity ★6 | | none | RC |
| | | active skill 青龍咆哮 50×ATK, CD 30→20 | | text in JSON only; mechanic (self-ATK × faction attack) plausible | mechanic RC; value 50/CD UNK-attested |
| | | leader 義侠の武 "enemy 蜀 damage halved" | 0.5 | description only | mechanic UNK-attested; 0.5 follows text (RC) |
| 3 | 趙雲 一陣の風 | HP/ATK/REC | 280/249/59, 1260/872/177 | none | RC |
| | | skill 長坂一騎駆け 10 s free move, CD 20→14 | | none; **also two divergent descriptions** across the two JSONs ("下一次…延長至10秒" vs "10秒間自由移動") | RC |
| | | leader 神威に至る槍術 | incoming ×1.0 in code | JSON itself marks exact multiplier `null`/PARTIAL | mechanic UNK; value **UNK**; code applies nothing |
| 4 | 諸葛亮 臥龍雌伏 | stats 287/264/53, 718/554/111 | | none | RC |
| | | skill 奇門遁甲 WEI→SHU, CD 10→5 | | none | RC |
| | | leader 八卦陣 SHU HP ×1.5 | 1.5 | described in text | mechanic UNK-attested |
| 7 | 呂布 戦鬼 | stats 341/277/22, 1705/970/176 | | none | RC |
| | | skill 天下無双 30×ATK ignore DEF, CD 35 | | none; ignore-DEF is hard-wired as `g.id == 7` (`PuzzleBoardView.java:472`) | RC |
| | | leader 吠虎の猛勇 full HP → ATK ×3.5 | 3.5 | none | UNK-attested |
| 9 | 周瑜 小覇王盟友 | stats 320/291/76, 1280/1019/243 | | none | RC |
| | | skill 孫呉の業火 8× 吳 ATK all enemies, CD 28→15 | | none | RC |
| | | leader 借刀殺人の計 GUI_MOU ×2.2 + "strong follow-up attack" | 2.2 | JSON: `follow_up_ratio: null`, PARTIAL. Code implements only ×2.2, not the follow-up | ×2.2 UNK; follow-up mechanic UNIMPLEMENTED/UNK |

**Recommendation:** all five → `RECONSTRUCTED` (or a new
`ATTESTED_UNCITED` tier) until per-field citations exist. Do not silently drop
the numbers; they are probably useful. Caveat that the label is what is being
downgraded, not the values, and see §5 step 2 for keeping gameplay identical.
Counter-view: if the maintainer can supply the wiki page/card No. for each of
the five, they may be re-promoted per field (mechanic vs value separately),
after the second-pass cross-check required by `COLLAB_HANDOFF.md`. Leader
skills of 趙雲 and 周瑜 are not eligible in any case (exact values unknown).

Also within the OV entries: `skillAux` values are internal enum codes (SHU=2,
QUN=4, WU=1) and `skillType` codes (0,1,2,10..13) are **CU**.

## 3. Hard-coded production data and classification

### 3.1 Generals / cards (`GameData.ROSTER`, `DropCardData.CARDS`)

| Field group | Where | Class | Note |
|---|---|---|---|
| Five OV generals' numbers | `GameData.java:193-237` | RC / UNK-attested | §2 |
| Five RC generals (劉備, 張飛, 曹操, 孫權, 貂蟬) all fields | `GameData.java:188-232` | **CU** | Labelled "重建"/"暫定測試技能", "v1.2 test data". They are invented test values, not reconstructions of an original record; the label RECONSTRUCTED overstates. Recommend **CU**. Names/factions/troop types of these historical figures are not evidence of any original card. |
| `sourceVariant`, `rarity` strings ("重建" as a rarity) | ROSTER | rarity "重建" = CU; variant text for OV rows UNK | Rarity is a display string, should be an int + star render. |
| `recruitPrice` 800–1500, `defaultTeam` {0..4} | `GameData.java:271-284` | CU | No source; only 5–9 have prices. |
| Draw-drop card table | `DropCardData.CARDS` (339 lines) | Names/faction/troop/card No.: UNK-attested (note text says "已校正", no source); stat fields 0 with `statsVerified=false` | Contains card Nos. (195, 197, 205, 54, 96, …) and 張梁 No.64 with full stats, all uncited. Notes like "原作攻略確認" name a source category, not a source. |
| Faction / troop names | `GameData.factionName/troopTypeName` | names OV-plausible; IDs CU | see §1.5 |
| Leader-skill multipliers hard-wired to `id` | `GameData.java:135-183` | see 3.3 | |

### 3.2 Active skills

| Item | Where | Class |
|---|---|---|
| Names, descriptions, CD/CDmin of 5 OV skills | ROSTER | UNK-attested (RC) |
| Skill parameters of 5 test skills (heal 900/750/1000, delay 2, dmg 1050, CD 4–5) | ROSTER | CU |
| Skill *type* dispatch (HEAL/DAMAGE/DELAY/SELF_ATK/TEAM_FACTION_ATK/CONVERT/FREE_MOVE) | `GameData.java:24-30`, `PuzzleBoardView.java:450-525` | CU (engine design). Effect existence for the 5 OV skills = UNK-attested |
| Zhao Yun move time 10000 ms hard-coded as `ZHAO_YUN_MOVE_MS` instead of using `skillValue` | `PuzzleBoardView.java:43,514` | CU; **bug-risk: data field `skillValue=10` is displayed but ignored** |
| `ignoreDefense = g.id == 7` and 100 defense-ignore | `PuzzleBoardView.java:472-476` | UNK (mechanic attested by description) — must become a skill flag |
| CD start value / cooldown decrement / CDmin usage | PuzzleBoardView | UNK — `skillCdMin` is shown in UI but no code applies the reduction from level-ups found; unverified |
| Skill seal | `AI_SKILL_SEAL` | CU (no original evidence anywhere in repo) |

### 3.3 Leader skills (`GameData.java:135-183`)

Hard-coded by `id`, not data:

| id | Effect | Class |
|---|---|---|
| 0 劉備 HP ×1.20 (only when RC) | CU |
| 1 關羽 incoming from SHU enemies ×0.5 | mechanic UNK-attested; value follows text |
| 1,2,3 SHU dmg ×1.25 (only when RC) — id 1 and 3 are OV so this branch is dead for them; fires for 張飛 only | CU (dead-code for OV ids; note the `case 1: case 3:` labels are misleading) |
| 4 諸葛亮 SHU HP ×1.5 | UNK-attested |
| 4 combos≥4 ×1.20 (only when RC) — dead for OV id 4 | CU dead code |
| 5 曹操 WEI ×1.30, 6 孫權 WU ×1.30 | CU |
| 7 呂布 ATK ×3.5 at full HP | UNK-attested |
| 8 貂蟬 heal ×1.5 | CU |
| 9 周瑜 GUI_MOU ×2.2 | UNK; follow-up not implemented |
| 3 趙雲 "damage greatly reduced", move-time extension | value UNK; code returns 1.0 and comment admits it (`GameData.java:177`). Move-time extension is not implemented at all (only the active skill uses 10 s) |

Contamination: the `!verifiedOriginal` branches (`GameData.java:137,153-170,182`)
are pre-v2.4.1 leftovers. Because leader effects are keyed on `id` *and* on
provenance, changing provenance to RC on 關羽/趙雲/諸葛亮/周瑜/呂布 would
re-activate these invented buffs on cards that already have original-text
leader skills (e.g., 呂布 id 7 is caught by earlier `return` so is safe; ids
1,3,4 would gain ×1.25/×1.20). This makes a naive downgrade a **gameplay
regression** — see §5.

### 3.4 Enemy stats and actions (`StageData.java`)

| Field | Class | Note |
|---|---|---|
| Enemy names, faction per pool entry | UNK-attested | "原作共通出現 8/9 種", "B3 前記錄 3 種" (`StageData.java:354,365,382`) — asserted, uncited |
| ATK, TURN with `attackVerified/turnVerified=true` | UNK-attested (labelled VERIFIED) | Circular: the flags live in the same reconstruction; the JSON only copies them. Not OV by the issue's rule |
| ATK/TURN with flags false | RC | 白秘玉/大宛馬/青色秘藥/張曼成/鐵門峽張寶/張梁 |
| 青秘玉・左慈 ATK 20 (flags true in Java) | **suspect** | Already PARTIAL in `reconstructed_master_v1.json` (#9) but Java still passes `true,true` → `statMark()`/`attackMark()` show it as verified. Java/JSON inconsistency |
| HP, DEF (all; `hpDefenseVerified` hard-coded `false` in factory `enemy()`) | RC | Correctly unmarked |
| Same enemy, different stats per stage (黃巾槍兵 15/51/122 ATK, 220/520/700 HP) | RC/UNK | plausible per-quest scaling, undocumented |
| `EnemyAction` types: attack/scout/power-up/convert/skill-seal, preemptive | CU | No enemy in `STAGES` uses anything but `通常攻擊 100%`; the action engine is untested by data and has no original evidence for any parameter. Keep engine, keep out of Master v1 data except default |
| Enemy attack interval semantic "turn" | UNK (mechanic attested) | |
| Stage 1 (`cao_yellow_turban_01`) enemy `hp` for 孫軍弓兵 etc. | RC | |

### 3.5 Encounter pools

Pool membership (8/3/9 enemies): UNK-attested. Roll rule (1–2 enemies, equal
weight, `Random(seed)`, no repeats within wave): **RC/CU** (StageData note says
so). `materializeRun` boss-last, wave count from `waveCount`: wave count
attested-in-Java only. Seed derivation `runSeed ^ 0x5A17D20FL ^ (stage.id<<40)`
in `DropData.roll`: CU. Note `roll()` picks `count` distinct enemies but the
JSON's `weighting: "equal"` does not record the no-repeat rule or the
`Random` algorithm; a Master-driven roll must keep `java.util.Random` to keep
seeds reproducible.

### 3.6 Stage metadata, rewards, advantage rules

| Field | Class |
|---|---|
| id/name/chapter strings | UNK-attested |
| `difficulty`, `stamina` (1/3, 2/5, 4/5) | UNK (JSON says UNKNOWN; Java presents them in the UI without a mark) |
| `coinReward`, `expReward` (70/30, 340/190, 470/340) | RC |
| `waveCount` 3/3/5 | attested in Java notes ("原作：3合戰"); uncited → UNK-attested |
| `sourceNote` text | uncited assertions; not evidence |
| Advantage pairs `ADV_*` | **UNK / contradicted**: three different per-stage relations (HAN>WEI,WEI>QUN | +QUN>HAN | WEI>QUN,HAN>QUN,QUN>HAN, note the third is *not* a cycle: `WEI>QUN, HAN>QUN, QUN>HAN`) vs the JSON's global WEI>WU>SHU>WEI cycle. Also `ADV_WEI_QUN_HAN` contains both HAN>QUN and QUN>HAN, so both attacker and defender are "advantaged": `factionMultiplier` returns 2.0 first for either direction (mutual advantage, `PuzzleBoardView.java:577-585`). Consider whether that is intended |
| Multipliers ×2 / ×0.5 | UNK-attested (JSON PARTIAL) |
| Player-side faction relation in Java is stage-only; no global default is used at all | so JSON `unit_advantage.default` is currently **dead data** |

### 3.7 Finisher thresholds/effects (`FinisherRules.java`, `PuzzleBoardView.java:299-315`)

| Item | Class |
|---|---|
| Existence of per-troop finishers, finisher names | UNK-attested |
| Stage-1 threshold 7 (Sword 5) | "原作明確資料" per code comment → UNK-attested; uncited |
| Stage-2/3 thresholds (10/14, sword 8/12) | RC (code says "推定值") |
| Damage multipliers 1.2/1.5/2.0 and spear 1.5/1.8/2.2 | UNK / RC — no source; not marked reconstructed except thresholds |
| Bow DEF reduction 20/25/50 %, barbarian ATK reduction 10/20/30 % | UNK; hard-coded in the view, not in `FinisherRules` |
| Cavalry all-target, Guimou/Shensuan effects | only cavalry implemented; 鬼謀/神算/劍/etc. produce a name and multiplier only |
| Charge = sum of groups by faction into troop (`FinisherRules.compute`) | UNK — mechanic rule reconstructed |
| Combo `1+0.25*(n-1)`, unit match factor `groups + excess/4`, 5 s move | JSON says VERIFIED with no source → UNK-attested; `board.confidence VERIFIED` too |

Contamination: `※高階門檻重建` is only shown in the summary for stage ≥ 2,
while multipliers for stages 1-3 carry no marker.

### 3.8 Other hard-coded data outside the list

- `DropData` (items, per-stage enemy→drop mapping, 50 % rate): RC/UNK; the
  `possibleDrop` function is a hand-written if-chain by `stageId` and enemy
  *name* (fragile: `張寶` maps to different items by stage; names such as 黃巾槍兵
  appear in several stages).
- `EnhancementData`: Next.5 cumulative table (Lv1–15) UNK-attested
  ("原作 Wiki"), no page cited; `star1/2` base growth 100/200 RC; same-faction
  ×1.5 (JSON: `same_faction_growth_multiplier` under a PARTIAL group) UNK;
  `enhancementCost = level*100*count` RC; `growthType` returns Next.5 for
  every card, so `growthTypeVerified` false is correct.
- `EquipmentData` weapons 青銅劍/環首刀/方天畫戟 (+20/35/60): **CU**, and the
  source string "舊重建版遺留裝備" = pre-v1.8 assumption still shipped;
  `rewardWeaponForStage` returns -1 always (dead).
- `PlayerData.expToNext`: `100 + (level-1)*40` **CU**, marked as such in a
  comment.
- `PlayerData.MAX_LEVEL`, HP/ATK per-level +55/+12: CU.
- `OriginalArtData`: 16 UNVERIFIED entries, no card_no/name — clean.

## 4. Provenance contamination check

| Check | Result |
|---|---|
| Reconstructed/test values presented as original | **Yes, systematic**: "ORIGINAL_VERIFIED" on five generals with no citation; UI label "原作核實" (`MainActivity.java:1033-1040`) and text "原作 Lv.1 … / Lv.Max …" (`:1044-1059`) show blue "verified" text; `StageData` `attackVerified=true` flags; 青秘玉 ATK 20 as verified in Java |
| Old pre-v2.4.1 assumptions surviving | Yes: `!verifiedOriginal` leader branches; five "v1.2 test" generals with `重建` rarity; legacy weapons; `+55/+12` growth; recruit prices; the `EXP` curve |
| Appearance-based art identity | None found. `OriginalArtData` all UNVERIFIED with no card_no; both JSONs `art_asset_id: null`, `identities_verified 0`. `docs/PIXEL_IDENTITY_CANDIDATE_35400027.md` proposes an asset↔No.157 link via raster comparison against a public card image (p.rakda3.net). It is an art-derived match, not a serial/card relation from game data, so it stays LOW/UNVERIFIED and must not be used here (`ASSET_MAPPING.csv` correctly keeps it UNVERIFIED). Note it is also No.157 〖長坂単騎駆〗趙雲, which is a different card variant than the 趙雲 一陣の風 in `GameData`; nothing should link them |
| Values copied from the retracted test master | No `ch_15200025/35300032/56400035` IDs, and no card Nos. 102/260/350 in Java or JSON. Caveat: `DropCardData` card Nos. (54, 64, 96, 195, 197, 205…) and `EnhancementData` were not traced; their provenance is undocumented. The retracted test master's parser-shaped rows share the same offline origin as the reconstruction, so any value whose only source is that file is unusable — the repo cannot show which values came from it (`master.json` SHA-256 is recorded, its content is not in the repo). |
| Circular verification | The stage `attackVerified` flags → JSON `VERIFIED` → the JSON is described as the audit basis of Java (`RECONSTRUCTED_MASTER_V1.md`). Same loop as issue #9 finding 2 |
| Doc drift | `docs/COLLAB_HANDOFF.md` still states baseline v2.4.1; README says v2.5.0 and does not mention which JSON is authoritative; `RECONSTRUCTED_MASTER_V1.md` names the JSON "canonical data source" while the app reads Java |
| `DataProvenance` Javadoc | "source-backed public game database entry" allows OV without a stored citation; the doc should require a `source` field |

## 5. Minimal migration order for Master Reconstruction v1

Goal: remove hard-coded production data with **byte-identical gameplay** (same
seeds → same waves, same damage) at every step. Each step is one reviewable
change with a golden test comparing old Java constants against the loaded
Master.

0. **Docs/data prerequisites (no code)**
   a. Decide the single canonical file. Recommendation: keep
      `master_reconstruction_v1.json` (v2.5 schema) as the loader target and
      fold in the `reconstructed_master_v1.json` audit fields (`card_no`,
      `identity_note`, per-field confidence, stage/enemy field confidence,
      `mechanics`), then retire the other or mark it legacy.
   b. Add to the schema: `provenance` **per field group** and
      `source` (URL/card No. or "attested by maintainer"), enabling
      mechanic/value split.
   c. Add `CUSTOM` handling for the five test generals and downgrade the five
      OV generals (§2), keeping all numeric values.
   d. One validator that runs on the one file; add checks noted in the #9
      audit (OV requires `source`, parent tier ≤ children).
1. **Introduce `MasterRepository` + parser (no callers yet)**. Loads the JSON
   from assets; unit tests only. Keep `GameData` untouched. Build must still
   work with the asset absent? (CLAUDE.md requires no original artwork only,
   the JSON is committed so it is fine.)
2. **Generals + active skills + leader skills**, in this order within one
   step:
   - Add explicit *effect fields* to the JSON so leader logic is data, not
     `id` switches: `leader.effects: [{kind, faction|troop, multiplier,
     condition}]`; skill `flags: [ignore_defense]`, `duration_ms`.
   - Make `GameData.ROSTER` a view built from Master. Preserve the current
     numbers and — critically — **replace the `verifiedOriginal` gates by
     explicit per-record fields** (`growth_model: "linear"|"flat_per_level"`,
     `leader_effects` already listing the active buffs) so a provenance
     downgrade does not change gameplay (§3.3, §1.4).
   - Use `skillValue` for Zhao Yun's move duration instead of the constant.
   - Golden test: for each general and level 1..max, HP/ATK/REC and every
     leader multiplier from Master equal the pre-migration values.
3. **Enemies + encounter pools + stage metadata + rewards**. Move
   `StageData.STAGES`/pools into Master with the existing per-field
   confidences. Keep `java.util.Random` and the exact draw sequence in
   `EncounterPool.roll`; golden test for several seeds against the current
   `materializeRun`. Set 青秘玉 ATK/TURN flags to PARTIAL in Master and make
   Java honour them (this changes only the `*` marks, not damage).
4. **Advantage rules** as data per stage (`pairs`, `multipliers`), and remove
   the unused global default or tag it "unused". Decide explicitly how mutual
   pairs resolve (§3.6).
5. **Finisher rules**: thresholds, multipliers, DEF/ATK reductions, names
   (`FinisherRules` + `PuzzleBoardView.java:299-315`) with per-stage
   confidence (stage 1 attested; 2–3 RC).
6. **Drop tables and enhancement** (`DropData.ITEMS`, `possibleDrop`,
   drop rate, `EnhancementData` curve/growth/cost, `DropCardData`) keyed by
   stage_id + enemy_id rather than enemy *name*, seed formula kept.
7. **Delete legacy CU data** only after an explicit decision: weapons
   (`EquipmentData`), `expToNext`, `recruitPrice`. Either keep them in Master
   as CU or remove them; do not leave them in Java.
8. **Retire `ReconstructedMasterData` summary** or point it at the same file;
   ensure it fails loudly if provenance rules are violated.

Do steps 0a–0d before any Java change; steps 2 and 3 are the riskiest and
need the golden tests. Never re-promote anything to OV during migration.

## 6. Recommended immediate actions (all doc-level)

1. Downgrade the five OV generals in `master_reconstruction_v1.json` and
   `GameData.java` (the latter is a later reviewed step) → RC with a
   "source not stored" note, or supply per-field citations.
2. Mark the five test generals CU, not RC.
3. Reconcile `troop_types`/`factions` IDs vs the parser enum.
4. Replace the `true,true` flags on 青秘玉・左慈 in `StageData.java` when
   Java is next touched.
5. Update `COLLAB_HANDOFF.md` baseline from v2.4.1 to v2.5.0 and name the
   authoritative master file.
6. Keep all 16 art assets UNVERIFIED; do not use `DropCardData`'s "No."
   values, `PIXEL_IDENTITY_CANDIDATE_35400027.md` or any stat similarity as
   identity evidence.

## Open questions for the maintainer/ChatGPT

- For each OV general: which wiki page / card No. / APK offset supplied each
  number? (per-field citation needed)
- Are mutual advantage pairs (`HAN>QUN` and `QUN>HAN`) in stage 3 intended?
- Is `skillCdMin` used anywhere (level-based CD reduction)?
- Source of the "原作明確資料" first-stage finisher threshold of 7.

---

# Appendix: earlier audit of `reconstructed_master_v1.json` (issue #9)

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
