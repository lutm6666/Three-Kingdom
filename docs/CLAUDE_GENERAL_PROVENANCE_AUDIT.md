# Claude audit: provenance of the current 10 generals (issue #11)

Scope: the 10 `GameData.ROSTER` entries. Only `docs/` was touched. No Java,
Gradle, workflow, APK, image or binary was modified. Per-field detail is in
`docs/GENERAL_PROVENANCE.csv`.

## Method and limits

- Java values were treated as claims. The repo's own reconstructed/test master
  and the earlier assistant conclusions were not used as evidence.
- Source: the public SanPazu database `https://p.rakda3.net/sanpuz/`
  (card pages `/sanpuz/b<No>`, number lists `/sanpuz/bls/<range>`). No artwork
  was used for identity.
- **Limits.** Pages were read through a summarising fetch tool, not raw HTML.
  `curl` was not permitted and `web.archive.org` was blocked, so there is **no
  archived or second independent source**. Every `ORIGINAL_VERIFIED` below is
  therefore *single public-database* evidence. It meets the issue's definition
  ("directly source-backed") but **not** the CLAUDE.md HIGH bar (needs an
  independent cross-check). The DB is a fan transcription, not original
  master data.
- The number lists for ranges 1-50, 101-300 were read. Ranges 51-100 and 301+
  were not scanned, so "variant not found" means "not found in the scanned
  ranges plus search results".

## Summary

| # | General | DB card | Java provenance | Audit result |
|---|---------|---------|-----------------|--------------|
| 0 | 劉備 | No.10 (label only) | RECONSTRUCTED | Stats/skills CUSTOM; variant label misleading |
| 1 | 關羽 | No.180 | ORIGINAL_VERIFIED | All fields match except `skillCdMin=20` (unsourced) |
| 2 | 張飛 | No.146/147 (no 闘鬼 found) | RECONSTRUCTED | CUSTOM; troop type conflicts (DB: 槍兵) |
| 3 | 趙雲 | No.156 | ORIGINAL_VERIFIED | Numbers match; leader name/description need downgrade |
| 4 | 諸葛亮 | No.274 | ORIGINAL_VERIFIED | All fields match |
| 5 | 曹操 | No.4 (label only) | RECONSTRUCTED | CUSTOM; variant label misleading |
| 6 | 孫權 | No.5-8 (no match) | RECONSTRUCTED | CUSTOM; variant string not found |
| 7 | 呂布 | No.191 | ORIGINAL_VERIFIED | Lv1 stats conflict; rest match |
| 8 | 貂蟬 | No.151 (label only) | RECONSTRUCTED | CUSTOM; variant label misleading |
| 9 | 周瑜 | No.154 | ORIGINAL_VERIFIED | All fields match |

## Values that should be downgraded

1. **趙雲 leader name** `神威に至る槍術` — that is the leader skill of
   No.157 【長坂単騎駆】. No.156 (`/b156`) shows `神速の槍術`. Java pairs the
   No.156 stats with No.157's leader name. → UNRESOLVED.
2. **趙雲 leader description / effect** — Java's text is a paraphrase
   ("大幅"), and its code comment says the multiplier is unknown. The fetch
   reported a 30% damage reduction on both 156 and 157, contradicting that
   comment; unconfirmed either way. → UNRESOLVED. Do not implement a number yet.
3. **呂布 Lv1 stats** (341/277/22) — the page shows Lv1 as HP 50 / ATK 341 /
   REC 277 (two fetches). Java's `22` exists nowhere, and Java looks
   column-shifted relative to the page. `HP 50` is itself implausible, so the
   page may be mis-keyed. → UNRESOLVED until raw HTML or a second source is seen.
   LvMax (1705/970/176) matches.
4. **關羽 `skillCdMin=20`** — the page shows a single cooldown of 30. The start
   value is verified; the minimum is not. → UNRESOLVED.
5. **Variant labels on the five non-verified generals** (劉備 孝行息子,
   曹操 東郡太守, 貂蟬 美女連環, 張飛 闘鬼, 孫權 若き虎の覚悟). The first three
   name real cards (No.10, No.4, No.151) whose real stats and skills are
   entirely different from Java's. The last two were not found. A label that
   looks like a card ID invites circular "verification" later. Suggest removing
   the variant or marking it as custom.
6. **張飛 `BARBARIAN`** — both 張飛 cards found are 槍兵. No source supports 蠻兵.

Note for the migration: `leaderHpMultiplier` / `leaderDamageMultiplier` gate
custom leader behaviour on `!verifiedOriginal`. Changing a record's provenance
changes battle behaviour, so flip provenance and logic together.

## Per-general notes

### 劉備 — RECONSTRUCTED / CUSTOM
Nearest card: No.10 `https://p.rakda3.net/sanpuz/b10` (★3, Lv15, 剣兵, 蜀,
143/80/30 → 343/319/61, 大器の片鱗 cd12, 義侠の心 Shu ATK ×1.5). Java's rarity
"重建", Lv50, all stats, 仁德 heal, and the ×1.20 HP leader skill are custom.
Faction and troop agree with No.10 (RECONSTRUCTED, not verified).

### 關羽 — mostly ORIGINAL_VERIFIED
No.180 `https://p.rakda3.net/sanpuz/b180`: ★6, 蜀, 槍兵, Lv99 (139 broken),
Lv1 520/300/60, Lv99 3208/1230/191, 青龍咆哮 ATK×50 Shu attack cd30, 義侠の武
halves Shu damage. All match. Java "max" stats are Lv99, not the limit-break
3528/1430/311. Only `skillCdMin` is flagged.

### 張飛 — CUSTOM
Found only `https://p.rakda3.net/sanpuz/b146` (【蛇矛猛者】★5 槍兵, Lv50,
310/267/40 → 1395/935/140, 長坂の大喝 delay 5 cd24, 義侠の憤怒) and
`https://p.rakda3.net/sanpuz/b147` (【万夫不当】★6). Java's 長坂怒吼, delay 2,
cd5, ×1.25 leader and BARBARIAN are invented.

### 趙雲 — numbers verified, leader skill not
No.156 `https://p.rakda3.net/sanpuz/b156`: ★5.5 (★★★★★☆), 騎兵, Lv50,
280/249/59 → 1260/872/177, 長坂一騎駆け cd 20→14. All match. Conflict:
No.157 `https://p.rakda3.net/sanpuz/b157` (★6, Lv99) has
`神威に至る槍術`; No.156 has `神速の槍術`.

### 諸葛亮 — ORIGINAL_VERIFIED
No.274 `https://p.rakda3.net/sanpuz/b274`: all fields match (287/264/53 →
718/554/111, cd 10→5, 八卦陣 Shu HP ×1.5). Awakening effect (50% damage cut for
1 turn) is not modelled.

### 曹操 — CUSTOM
No.4 `https://p.rakda3.net/sanpuz/b4`: ★5.5, 魏, 剣兵, Lv99, 672/604/103 →
2016/1026/206, 乱世の奸雄 cd 10→7, 魏武の至強 Wei ATK ×2. Java's numbers,
skill and ×1.30 leader are invented.

### 孫權 — CUSTOM
Cards found: `https://p.rakda3.net/sanpuz/b5` 碧眼児 (name only),
`https://p.rakda3.net/sanpuz/b6` 無邪気な狩 (★3), `https://p.rakda3.net/sanpuz/b7`
若獅子, `https://p.rakda3.net/sanpuz/b8` 孫呉当主. `若き虎の覚悟` was not found.
All Java numbers, the heal skill and ×1.30 leader are invented.

### 呂布 — mostly ORIGINAL_VERIFIED
No.191 `https://p.rakda3.net/sanpuz/b191`: ★6.5 glyph string, 群, 騎兵, Lv50,
LvMax 1705/970/176, 天下無双 ATK×30 defence-ignoring cd 35→35, 吠虎の猛勇
×3.5 at full HP. Only Lv1 conflicts (see above).

### 貂蟬 — CUSTOM
No.151 `https://p.rakda3.net/sanpuz/b151` (★6, 群, 鬼謀, Lv99, 1056/725/253 →
2375/1268/606, 美女連環の計 enemy HP −30% cd 30→18, 秘めたる本心) and No.150
`https://p.rakda3.net/sanpuz/b150` (【絶世の美】★5). Java's 閉月 heal and 桃 heal
×1.5 leader are invented. The DB spells the name 貂蝉.

## Source conflicts

- 趙雲 leader name: No.156 vs No.157 (above). Also the 30% figure versus the
  Java "exact multiplier unknown" comment.
- 呂布 Lv1 row versus the Java values (above).
- Different cards of the same character differ in level cap and stats, so a
  name alone (e.g. 曹操, 貂蟬) never identifies a row.

## Recommendations

1. Migrate to field-group provenance instead of one flag per general; the
   verified generals already need mixed labels (趙雲, 呂布, 關羽).
2. Keep 諸葛亮 and 周瑜 ORIGINAL_VERIFIED (single-source). Keep 關羽 except the
   cooldown minimum. Keep 趙雲 and 呂布 verified only for the fields in the CSV
   marked ORIGINAL_VERIFIED.
3. Before anything is called HIGH, get a raw-HTML or archived second copy of
   the DB pages, or original `master.bin` data.
4. Scan DB ranges 51-100 and 301+ to settle the 張飛 and 孫權 variants.

No original values were added or invented.
