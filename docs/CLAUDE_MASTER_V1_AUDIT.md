# Claude audit: v2.5 Master Reconstruction provenance and migration (issue #12)

Base: `c74c113`. Read-only audit. No Java, Gradle, workflow, image, APK or binary
was modified. The build, tests and PowerShell validators were **not** run (no
PowerShell in this environment). None of the 16 `ch_*_l` art assets is identified,
and no art-appearance reasoning is used anywhere below. The earlier issue #9 audit
is kept as Appendix A.

Evidence available in-repo: `docs/EVIDENCE_CORRECTION.md` (native-binary
resource templates only), `RECONSTRUCTED_MASTER_V1.md`, and the two JSON files.
**No file in the repo cites a source (APK offset, native table, public DB URL)
for any stat, skill value or stage value.** "Maintainer-attested" is therefore
the strongest evidence that exists, and it is not "direct APK/native evidence or
an independent public source for that exact value".

Two vocabularies are kept separate, as requested:
- **Mechanic provenance (M):** does the rule/skill *kind* exist in the original?
- **Value provenance (V):** is the *exact number/string* proven?

Each is one of `OV` (ORIGINAL_VERIFIED), `RC` (RECONSTRUCTED), `CU` (CUSTOM),
`UNK` (UNKNOWN / needs evidence).

## 1. Headline findings

1. **`master_reconstruction_v1.json` is not used by the app.** Its only
   reference is `tools/verify_reconstruction_master.ps1`. `GameData.ROSTER` is
   the live source. `ReconstructedMasterData` loads the *other* file
   (`reconstructed_master_v1.json`) and only produces a validity summary.
2. **The v2.5 file contains generals only** (plus factions and troop_types).
   Enemies, pools, stages, rewards, advantage, finisher and drops have no v2.5
   home yet.
3. **Zero `ORIGINAL_VERIFIED` values have a stored source.** Recommendation:
   downgrade all five OV generals to `RECONSTRUCTED` (keep the numbers) until each
   field carries a citation. See section 2.
4. **The same five generals were already downgraded to PARTIAL in the issue #9
   file** for lack of source. v2.5 re-promotes them to OV with no new evidence.
5. **Troop-type ids contradict the original parser enum.**
   `reconstructed_master_v1.json` `original_parser_troop_types` is
   BOW0 SWORD1 SPEAR2 CAVALRY3 SHEN_SUAN4 GUI_MOU5 BARBARIAN6. Java and v2.5 use
   SWORD0 CAVALRY1 SPEAR2 BOW3 BARBARIAN4 GUI_MOU5 SHEN_SUAN6, yet v2.5 marks
   these ids `ORIGINAL_VERIFIED`. The names are plausible, but the *numeric ids*
   are app-local (RC) and must not be labelled OV. Use string keys (`"SWORD"`) in
   the new master.
6. **Downgrading provenance silently changes gameplay.**
   `General.verifiedOriginal` (= `provenance == OV`) is a behaviour switch, not a
   label:
   - `PlayerData.java:260-279`: stat growth is linear interpolation for OV and
     +55 HP / +12 ATK per level (recovery flat) otherwise.
   - `MainActivity.java:1063`.
   - The legacy leader buffs in `GameData.java:135-183`.

   Provenance and behaviour must be decoupled *before* any provenance change
   (migration step 2).
7. If ids 1, 3, 4 were downgraded without step 2, they would newly gain the
   legacy buffs (SHU x1.25 for ids 1-3, combo x1.20 for id 4) and change growth
   model.

## 2. Audit of every `DataProvenance.ORIGINAL_VERIFIED` (GameData.java)

Five records at `GameData.java:193, 203, 208, 223, 233` (ids 1, 3, 4, 7, 9).
Per-field evidence present in the repo: **none** beyond the maintainer's label.
`DOWNGRADE` means move to RC until cited.

| id | General | Field group | M | V | In-repo source | Recommendation |
|---|---|---|---|---|---|---|
| 1 | 關羽 美髯公 | name/variant, faction 蜀, 6 stars, maxLv 99 | OV plausible | UNK | none | DOWNGRADE |
| 1 | | Lv1/LvMax 520/300/60 → 3208/1230/191 | OV (cards have stat endpoints) | UNK | none | DOWNGRADE |
| 1 | | troop SPEAR | OV | UNK (enum id app-local) | none | DOWNGRADE |
| 1 | | skill 青龍咆哮: self ATK x50, 蜀, CD 30→20 | OV (self-ATK skill kind) | UNK | none | DOWNGRADE |
| 1 | | leader 義侠の武: enemy 蜀 damage x0.5 | OV (text) | RC ("half" is text-derived) | none | keep mechanic, DOWNGRADE value |
| 3 | 趙雲 一陣の風 | stats 280/249/59 → 1260/872/177, maxLv 50 | OV plausible | UNK | none | DOWNGRADE |
| 3 | | skill 長坂一騎駆け: free move 10 s, CD 20→14 | OV | UNK; **10 s is hard-coded** (`ZHAO_YUN_MOVE_MS`) and ignores `skillValue` | none | DOWNGRADE, fix in data step |
| 3 | | leader 神威に至る槍術: longer move time, less incoming damage | OV (text) | **UNK**; code applies nothing, correctly | comment at `GameData.java:177` | keep UNK, do not invent |
| 4 | 諸葛亮 臥龍雌伏 | stats 287/264/53 → 718/554/111 | OV plausible | UNK | none | DOWNGRADE |
| 4 | | skill 奇門遁甲: convert 魏→蜀, CD 10→5 | OV | UNK | none | DOWNGRADE |
| 4 | | leader 八卦陣: 蜀 HP x1.5 | OV (text) | UNK | none | DOWNGRADE |
| 7 | 呂布 戦鬼 | stats 341/277/22 → 1705/970/176 | OV plausible | UNK | none | DOWNGRADE |
| 7 | | skill 天下無双: ATK x30, "ignore defense", CD 35 | OV | UNK; skillType 10 is shared with 關羽, so check the code path honours "ignore DEF" | none | DOWNGRADE |
| 7 | | leader 吠虎の猛勇: full HP → ATK x3.5 | OV | UNK | none | DOWNGRADE |
| 9 | 周瑜 小覇王盟友 | stats 320/291/76 → 1280/1019/243 | OV plausible | UNK | none | DOWNGRADE |
| 9 | | skill 孫呉の業火: 吳 ATK x8 to all enemies, CD 28→15 | OV | UNK | none | DOWNGRADE |
| 9 | | leader 借刀殺人の計: 鬼謀 ATK x2.2 | OV | UNK; x2.2 applied, the "強力再攻擊" follow-up clause is **not implemented** | none | DOWNGRADE, note unimplemented clause |

Other "verified" flags that carry the same claim without a `DataProvenance`:
- `StageData` `attackVerified` / `turnVerified` per enemy, and `DropCardData`
  `statsVerified` / `identityVerified`. No source is stored for either.
- `identityVerified=true` in `DropCardData.partial(...)` for entries with
  `cardNo == null` (祖茂, 簡雍, 張寶).
- 青秘玉・左慈 ATK 20 (`StageData.java:370`) is flagged verified in Java but
  PARTIAL in the issue #9 JSON. It is anomalous next to its peers (ATK 48-191)
  and equals a reconstructed stage-1 value.

## 3. Hard-coded production data and classification

| Group | Location | M | V | Notes |
|---|---|---|---|---|
| Faction names WEI/WU/SHU/HAN/QUN | `GameData.java:4-8, 242` | OV | RC | ids match the parser enum in the issue #9 JSON |
| Troop types | `GameData.java:16-22, 253` | OV | RC ids; names UNK | ids conflict with parser enum (1.5) |
| Roster ids 1,3,4,7,9 | `GameData.java:193-237` | OV | UNK, treat as RC | section 2 |
| Roster ids 0,2,5,6,8 | `GameData.java:188-232` | CU | CU | labelled RC/"暫定測試" but they are invented v1.2 test data, so **CUSTOM not RC** |
| Test-general skills/leaders (仁德, 長坂怒吼, 魏武之威 ...) | `GameData.java` | CU | CU | text says 暫定 |
| Legacy leader multipliers 1.20/1.25/1.30, heal x1.5 | `GameData.java:137-182` | CU | CU | gated on `!verifiedOriginal` |
| Skill type constants 0,1,2,10-13 | `GameData.java:24-30` | OV/RC (kinds) | app-local ids | |
| Growth model (interp vs +55/+12) | `PlayerData.java:260-279` | RC (interp) / CU (linear) | CU | pre-v2.4.1 leftover for non-OV |
| EXP curve `100+(lv-1)*40` | `PlayerData.java:243-253` | CU | CU | comment says original table not imported |
| Recruit prices 900/900/1500/800/1000 | `GameData.java:271` | CU | CU | old economy |
| Weapons 青銅劍/環首刀/方天畫戟 +20/+35/+60 | `EquipmentData.java` | CU | CU | "舊重建版遺留裝備" |
| Enemy stats HP/ATK/TURN/DEF/faction | `StageData.java:340-423` | OV (enemies exist) | UNK; `hpDefenseVerified` always false | no source stored |
| Enemy AI actions | `StageData.java:23-60` | RC | UNK | every enemy uses only `attack x100%`; framework only |
| Encounter pool membership | `StageData.java:340-382` | RC ("原作共通出現" claimed) | UNK | |
| Pool rule 1-2, equal weight | `StageData.java:352` | RC | RC | stated as reconstruction |
| Boss waves | `StageData.java:394-423` | OV claimed (names, count) | UNK | |
| Wave count, difficulty, stamina | `StageData.java` | UNK | UNK | JSON `unlabeled_fields` |
| Coin/EXP rewards 70/30, 340/190, 470/340 | `StageData.java` | RC | RC | |
| Stage advantage pairs | `StageData.java:6-21, 391-419` | RC | RC | contradicts global default in JSON; stage 3 has HAN>QUN and QUN>HAN mutual |
| Advantage x2 / x0.5 | `PuzzleBoardView.java:581` | RC | RC | JSON says PARTIAL |
| Combo +0.25 per extra combo | `PuzzleBoardView.java:806` | RC | RC | |
| `unitMatchFactor` (groups + excess/4) | `PuzzleBoardView.java:979` | RC | RC | |
| Base move 5 s, Zhao Yun 10 s | `PuzzleBoardView.java:42-43` | RC | RC / UNK | |
| Finisher thresholds 7/10/14 (-2 sword) | `FinisherRules.java:71` | OV for stage 1 only, per code comment | UNK-source (stage 1); RC (2-3) | comment at :119 is not backed by a repo source |
| Finisher damage x1.2/1.5/2.0, spear x1.5/1.8/2.2 | `FinisherRules.java:84` | RC | UNK | |
| Bow DEF reduction 20/25/50 %, barbarian ATK reduction 10/20/30 % | `PuzzleBoardView.java:299-315` | RC | UNK | not in any JSON |
| Finisher names | `FinisherRules.java:102` | OV plausible | UNK | |
| Drop cards (16 entries) | `DropCardData.java` | OV for existence | card Nos. UNK-source; only 張梁 has full stats | 祖茂/簡雍/張寶 have `identityVerified` with null No. |
| Drop table stage → enemy name → item | `DropData.java:70-108` | RC | RC | keyed by *name* |
| Drop rate 50 % | `DropData.java:12` | RC | RC | stated in code |
| Drop RNG seed mix | `DropData.java:112` | CU | CU | must be preserved for determinism |
| Enhancement Next.5 table | `EnhancementData.java:12` | OV (Wiki, per comment) | UNK-source | no citation stored |
| Enhancement base growth 100/200, same-faction x1.5, cost `lv*100*n` | `EnhancementData.java` | RC / CU | RC / CU | |
| Encounter RNG (`java.util.Random(seed)` sequence) | `StageData.java:189-212` | CU | CU | seed golden tests depend on it |

## 4. Provenance contamination check

- **Reconstructed/test values presented as original:**
  - the 5 OV generals (no source);
  - stage `attackVerified` / `turnVerified`;
  - `identityVerified` with a null card No.;
  - the troop-id enum labelled OV in v2.5 while the sibling JSON records a
    different parser enum;
  - the finisher stage-1 "原作明確" comment with no source.
- **Pre-v2.4.1 leftovers:** legacy weapons, EXP curve, recruit prices, the
  `!verifiedOriginal` growth and leader buffs, the "重建" rarity string.
- **Test-general mislabel:** ids 0,2,5,6,8 are `RECONSTRUCTED` in Java and in
  the v2.5 JSON, but they are invented (CUSTOM).
- **Retracted test master:** no `ch_*` serial, sprite id, or card Nos.
  102/260/350 appears in code or either JSON. The repo cannot exclude that the
  five OV stat lines came from the same offline reconstruction workflow, which is
  a further reason to downgrade.
- **Appearance-based identity:** none found in production code.
  `PIXEL_IDENTITY_CANDIDATE_35400027.md` is art-derived and stays
  LOW/UNVERIFIED and unused.
- **Card Nos. in `DropCardData`** (195, 197, 205, 54, 96, 58, 68, 80, 207, 64,
  238): provenance not traced, so UNK-source until cited. The No.80 note
  correctly warns not to reuse No.180 data.

## 5. Minimal migration order (Master Reconstruction v1)

Goal: remove hard-coded production data without changing gameplay.

0. **One canonical file and schema.** Pick `master_reconstruction_v1.json`. Add
   `sources[]` and per-field `{mechanic, value, source_ref}`. Retire the duplicate
   semantics of `reconstructed_master_v1.json`. Downgrade OV generals to RC,
   relabel ids 0,2,5,6,8 to CU, use string enums. Docs/JSON only. The validator
   rejects OV without `source_ref`.
1. **Parser + repository, no callers.** Unit test: parse the JSON and compare to
   `GameData.ROSTER` field by field.
2. **Generals, skills, leaders.** Replace the `verifiedOriginal` gates with
   explicit per-record fields: `growth_model` (`linear_endpoints` |
   `legacy_55_12`) and `leader.effects[]` (filter, multiplier, condition). Same
   numbers in, same numbers out. Golden test over levels 1..max for all 10 ids and
   every leader multiplier. Move Zhao Yun's move time to `skill.value`.
3. **Enemies, pools, stages, rewards.** Keep `java.util.Random` and the draw
   order in `EncounterPool.roll` / `materializeRun`. Seed golden tests (for
   example 50 seeds x 3 stages).
4. **Advantage rules** per stage as `pairs[]`, plus the 2.0 / 0.5 multipliers.
5. **Finisher rules:** thresholds, per-troop multipliers, bow/barbarian
   reductions, names.
6. **Drops and enhancement** keyed by `stage_id + enemy_id` (not name). Preserve
   the seed-mix constant, the enhancement table and its factors.
7. **Legacy CU data:** keep as CU (flagged in UI) or remove: weapons, EXP curve,
   recruit prices.
8. **Retire or repoint `ReconstructedMasterData`** to the canonical file.

Steps 2 and 3 carry the regression risk. Run build and ADB after each.

## 6. Machine-actionable migration table

Target root: `app/src/main/assets/data/master_reconstruction_v1.json`.
`M` = mechanic provenance, `V` = value provenance (both as audited today).
`rec_V` = recommended value provenance after this audit. `src` = evidence present
in the repo (`none` = maintainer attestation only). Paths are JSONPath-like;
`{g}` is a general index. Semicolons inside braces list sibling keys. The CSV is
comma-separated with no embedded commas in fields.

```csv
java_symbol,java_location,current_java_label,target_path,type,M,V,src,rec_V,step,behavior_note
General.id/name/variant,GameData.java:186-237,OV(1;3;4;7;9)/RC,$.generals[{g}].{id;name;variant},int/string,OV,UNK,none,RC (ids 0 2 5 6 8: CU),2,identity text only
General.faction,GameData.java:186-237,none,$.generals[{g}].faction,enum-string,OV,RC,none,RC,2,use WEI/WU/SHU/HAN/QUN strings not ints
General.troopType,GameData.java:186-237,none,$.generals[{g}].troop_type,enum-string,OV,RC,conflicts with parser enum,RC,2,use SWORD etc strings; never persist int ids
General.rarity/maxLevel,GameData.java:186-237,none,$.generals[{g}].{rarity;max_level},string/int,OV,UNK,none,RC,2,rarity "重建" on CU rows should become null
General.hp/atk/recovery,GameData.java:186-237,none,$.generals[{g}].lv1.{hp;atk;recovery},int,OV,UNK,none,RC (ids 0 2 5 6 8: CU),2,
General.maxHp/maxAtk/maxRecovery,GameData.java:186-237,none,$.generals[{g}].lvmax.{hp;atk;recovery},int,OV,UNK,none,RC (ids 0 2 5 6 8: CU),2,
General.verifiedOriginal,GameData.java:41;95,derived from provenance,$.generals[{g}].growth_model,enum(linear_endpoints|legacy_55_12),CU,CU,n/a,CU,2,decouple: ids 1 3 4 7 9 -> linear_endpoints; ids 0 2 5 6 8 -> legacy_55_12
PlayerData.effectiveHp/Atk/RecoveryAtLevel,PlayerData.java:260-279,verifiedOriginal branch,$.growth_models.{linear_endpoints;legacy_55_12},object,RC,CU,none,CU,2,legacy = +55 HP and +12 ATK per level; recovery flat
General.skillName/skillDescription,GameData.java:186-237,none,$.generals[{g}].skill.{name;description},string,OV,UNK,none,RC (test rows: CU),2,
General.skillType,GameData.java:24-30 and rows,none,$.generals[{g}].skill.type,enum-string,OV,RC,none,RC,2,HEAL DAMAGE DELAY SELF_ATK TEAM_FACTION_ATK CONVERT FREE_MOVE
General.skillValue/skillAux,GameData.java:186-237,none,$.generals[{g}].skill.{value;aux},int,OV,UNK,none,RC,2,aux meaning differs per type so name it (faction/from/to)
General.skillCd/skillCdMin,GameData.java:186-237,none,$.generals[{g}].skill.{cd;cd_min},int,OV,UNK,none,RC,2,
ZHAO_YUN_MOVE_MS,PuzzleBoardView.java:43,hard-coded,$.generals[3].skill.value (seconds),int,OV,UNK,none,RC,2,code must read data; BASE_MOVE_MS -> $.mechanics.base_move_ms
General.leaderName/leaderDescription,GameData.java:186-237,none,$.generals[{g}].leader.{name;description},string,OV,UNK,none,RC (test rows: CU),2,
leaderHpMultiplier id4 SHU x1.5,GameData.java:136,OV branch,$.generals[4].leader.effects[0]={stat:hp;filter:{faction:SHU};mult:1.5},object,OV,UNK,none,RC,2,
leaderHpMultiplier id0 x1.20,GameData.java:137,legacy,$.generals[0].leader.effects[0]={stat:hp;filter:all;mult:1.2},object,CU,CU,n/a,CU,2,
leaderDamageMultiplier id7 x3.5 at full HP,GameData.java:146,OV branch,$.generals[7].leader.effects[0]={stat:atk;filter:all;cond:hp_full;mult:3.5},object,OV,UNK,none,RC,2,
leaderDamageMultiplier id9 x2.2 GUI_MOU,GameData.java:149,OV branch,$.generals[9].leader.effects[0]={stat:atk;filter:{troop:GUI_MOU};mult:2.2},object,OV,UNK,none,RC,2,follow-up attack clause unimplemented
leaderDamageMultiplier ids 1-3 SHU x1.25,GameData.java:155-158,legacy,$.generals[{1;2;3}].leader.effects,object,CU,CU,n/a,CU,2,ids 1 and 3 are OV and do NOT get this today; only id 2 does; preserve exactly
leaderDamageMultiplier id4 combos>=4 x1.20,GameData.java:159-160,legacy,$.generals[4].leader.effects,object,CU,CU,n/a,CU,2,id4 is OV so not applied today; preserve
leaderDamageMultiplier id5 WEI x1.30 and id6 WU x1.30,GameData.java:161-164,legacy,$.generals[{5;6}].leader.effects,object,CU,CU,n/a,CU,2,
leaderIncomingMultiplier id1 vs SHU x0.5,GameData.java:176,OV branch,$.generals[1].leader.effects[1]={dir:incoming;filter:{enemy_faction:SHU};mult:0.5},object,OV,RC,none,RC,2,
leaderIncomingMultiplier id3 unknown,GameData.java:177,comment,$.generals[3].leader.effects[1]={dir:incoming;mult:null},object,OV,UNK,comment only,UNK,2,keep null; no behavior
leaderHealMultiplier id8 x1.5,GameData.java:182,legacy,$.generals[8].leader.effects,object,CU,CU,n/a,CU,2,
GameData.recruitPrice,GameData.java:271-280,none,$.generals[{g}].recruit_price,int,CU,CU,n/a,CU,7,
GameData.defaultTeam,GameData.java:282,none,$.defaults.team,int[],CU,CU,n/a,CU,7,
Weapon.*,EquipmentData.java:19-23,legacy,$.equipment[].{id;name;atk_bonus},object,CU,CU,n/a,CU,7,
PlayerData.expToNext,PlayerData.java:243-253,legacy,$.progression.exp_curve,formula,CU,CU,n/a,CU,7,100+(lv-1)*40
Enemy.name/maxHp/attack/interval/faction/defense,StageData.java:340-423,per-flag booleans,$.enemies[].{name;hp;atk;turn;faction;def},object,OV,UNK,none,RC,3,add stable enemy_id; 黃巾槍兵 has 3 stat variants
Enemy.attackVerified/turnVerified/hpDefenseVerified,StageData.java:87-89,boolean,$.enemies[].field_prov.{atk;turn;hp;def},enum,-,-,none,per-field,3,replace booleans with enum
Enemy.preemptive/actions,StageData.java:90-91,default attack,$.enemies[].{preemptive;actions[]},object,RC,UNK,none,RC,3,actions[]={type;value;aux}; all current rows = attack 100
EncounterPool.enemies,StageData.java:340-382,none,$.stages[].pool[] (enemy_id),string[],RC,UNK,none,RC,3,order is behavior (index-based RNG); preserve order
EncounterPool.min/maxEnemiesPerWave,StageData.java:352,none,$.stages[].encounter_rule.{min;max;weighting},int,RC,RC,stated,RC,3,
EncounterPool.roll draw order,StageData.java:189-212,none,$.mechanics.encounter_rng,object,CU,CU,n/a,CU,3,java.util.Random; draws: count then distinct index
Stage.fixedBossWave,StageData.java:394-423,none,$.stages[].boss_wave[] (enemy_id),string[],OV,UNK,none,RC,3,
Stage.waveCount/difficulty/stamina,StageData.java:389-417,none,$.stages[].{wave_count;difficulty;stamina},int,OV,UNK,none,UNK,3,
Stage.coinReward/expReward,StageData.java:389-417,none,$.stages[].rewards.{coin;exp},int,RC,RC,none,RC,3,
Stage.name/chapter/sourceNote,StageData.java:386-418,none,$.stages[].{name;chapter;note},string,OV,UNK,none,RC,3,
Stage.advantagePairs,StageData.java:6-21;391-419,none,$.stages[].advantage.pairs[]=[att;def],string pairs,RC,RC,none,RC,4,contradicts global default; do not merge
factionMultiplier 2.0/0.5,PuzzleBoardView.java:581-590,none,$.mechanics.faction_advantage.{advantage;disadvantage},float,RC,RC,none,RC,4,JSON already PARTIAL
comboMultiplier +0.25,PuzzleBoardView.java:806,none,$.mechanics.combo_multiplier_per_extra_combo,float,RC,RC,none,RC,4,
unitMatchFactor excess/4,PuzzleBoardView.java:979-986,none,$.mechanics.match_factor.{group_weight;excess_divisor},object,RC,RC,none,RC,4,
FinisherRules.stageForCharge 7/10/14 (-2 sword),FinisherRules.java:71-82,comment says stage 1 original,$.finisher.thresholds[troop]=[t1;t2;t3],int[],OV,UNK (t1) / RC (t2 t3),comment only,RC,5,per-value split: t1 source uncited; t2 t3 RC
FinisherRules.damageMultiplierFor,FinisherRules.java:84-100,none,$.finisher.damage_mult[troop]=[s1;s2;s3],float[],RC,UNK,none,RC,5,SPEAR 1.5/1.8/2.2; others 1.2/1.5/2.0
bowDefenseReductionPercent 20/25/50,PuzzleBoardView.java:299,none,$.finisher.effects.BOW.def_reduction_pct,int[],RC,UNK,none,RC,5,
barbarianAttackReductionPercent 10/20/30,PuzzleBoardView.java:308,none,$.finisher.effects.BARBARIAN.enemy_atk_reduction_pct,int[],RC,UNK,none,RC,5,
finisherName,FinisherRules.java:102-116,none,$.finisher.names[troop][stage],string,OV,UNK,none,RC,5,
isGuaranteedThreshold,FinisherRules.java:118-121,none,$.finisher.thresholds_prov,enum,-,-,-,derive from per-value prov,5,remove hard-coded stage<=1
DropCardData.CARDS,DropCardData.java:114-328,statsVerified/identityVerified,$.drop_cards[].{id;name;faction;troop;rarity;card_no;stats;prov},object,OV,UNK,none,RC (card_no UNK until cited),6,identityVerified true with null card_no is inconsistent
DropData.ITEMS,DropData.java:40-61,none,$.items[].{id;name;type;rarity;level},object,RC,RC,none,RC,6,
DropData.possibleDrop,DropData.java:70-108,none,$.stages[].drops[]={enemy_id;item_id},object,RC,RC,none,RC,6,replace name matching with enemy_id
RECON_DROP_RATE_PERCENT,DropData.java:12,stated recon,$.mechanics.drop_rate_percent,int,RC,RC,stated in code,RC,6,50
DropData.roll seed mix,DropData.java:112-115,none,$.mechanics.drop_rng.seed_mix,long,CU,CU,n/a,CU,6,0x5A17D20F ^ (stage_id<<40)
EnhancementData.NEXT5_CUMULATIVE,EnhancementData.java:12-29,comment says wiki,$.enhancement.curves.NEXT5,int[],OV,UNK,none,RC,6,cite the Wiki page before OV
EnhancementData.RECON_STAR1/2_BASE_GROWTH,EnhancementData.java:8-9,stated recon,$.enhancement.base_growth_by_rarity,int,RC,RC,stated in code,RC,6,100/200
EnhancementData same-faction x1.5 and cost,EnhancementData.java:102;132,none,$.enhancement.{same_faction_mult;cost_per_level_per_material},number,CU,CU,n/a,CU,6,
EnhancementData.growthType,EnhancementData.java:39-42,stated recon,$.drop_cards[].growth_type,enum,RC,RC,stated in code,RC,6,per-card field instead of constant
```

## 7. Open questions for the maintainer

1. Can you cite an APK/native table or public page (URL + card No.) for any of
   the five OV stat lines? Without it they stay RC.
2. Is the troop-id numbering in Java intentionally app-local? If so, keep it out
   of persisted saves.
3. Should ids 0,2,5,6,8 remain playable as CU, or be replaced by real low-rarity
   cards in step 7?
4. What source underlies the "stage 1 finisher threshold is original" comment?

## Not done

No values were added or invented and nothing was executed. Card-No. provenance
for `DropCardData` and the contents of `OriginalArtData.java` were not traced.

---

# Appendix A: earlier audit (issue #9)

Scope: `reconstructed_master_v1.json`, `RECONSTRUCTED_MASTER_V1.md`,
`EVIDENCE_CORRECTION.md`, `tools/validate_reconstructed_master.ps1`, and
`StageData.java` (read-only, for cross-reference). No Java, Gradle, workflow,
image or binary was modified. The validator was not executed (PowerShell/Python
were not available in this run); results come from manual reading.

### Confirmed OK

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

### Findings and corrections

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

### Validator gaps (not changed; outside allowed files)

- Does not reject a `VERIFIED` parent containing RECONSTRUCTED children.
- Does not check `art_asset_id` stays null, or `identities_verified` against
  actual mapped assets.
- Does not require `VERIFIED` characters to have `card_no`/source.
- Does not validate `advantage`/`enhancement` confidence fields or stage
  `composition` content against Java.
- After this audit `VERIFIED_CHARACTERS` prints 0; the validator still passes
  (all values used are in the allowed set).
