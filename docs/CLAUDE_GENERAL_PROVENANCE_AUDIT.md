# Claude audit: provenance of the 10-general roster (issue #11)

No Java, Gradle, workflow, APK, image or binary file was modified.
Companion table: `GENERAL_PROVENANCE.csv` (130 rows: 10 generals x 13 field groups).

## Bottom line

**No field of any of the 10 generals can be classified ORIGINAL_VERIFIED from
this audit.** The five cards currently flagged `DataProvenance.ORIGINAL_VERIFIED`
(關羽, 趙雲, 諸葛亮, 呂布, 周瑜) should all be downgraded until someone can read
the public card pages. The five `RECONSTRUCTED` cards (劉備, 張飛, 曹操, 孫權,
貂蟬) are really mostly CUSTOM (self-declared `暫定測試技能`).

## Access limitation (read this first)

- The only public database found is the SanPazu fandom wiki,
  `https://3pz.fandom.com/zh/wiki/武将图鉴` (card index) with per-card pages such as
  `【軍神】関羽`, `【長坂単騎駆】趙雲`, `【三顧の禮】諸葛亮`.
- In this run `WebFetch` on every fandom URL returned **HTTP 402**, `curl` was not
  permitted, and `web.archive.org` was blocked. Web search returned page titles and
  a few snippets only, no stat tables or skill text.
- Therefore no value below was compared against a source page. This is a
  limitation of the run, not evidence the values are wrong. Search snippets are
  used only as leads, never as verification.

## Classification legend (per issue)

- ORIGINAL_VERIFIED: directly source-backed (none found).
- RECONSTRUCTED: derived from documented original behaviour, exact value unavailable.
- CUSTOM: deliberately invented for playability.
- UNRESOLVED: evidence insufficient.

The CSV column `recommended_provenance` uses these four values. `UNRESOLVED` rows
map to `RECONSTRUCTED` in the Java enum (which has no UNRESOLVED) via
`production_action = keep (provenance RECONSTRUCTED)`. Evidence codes in the CSV:
`A` = claim only, no retrievable source, repo files are not independent; `P` =
placeholder, `GameData.java` labels it `重建`/`暫定`.

## Repo evidence and why it is not independent

- `reconstructed_master_v1.json` has `card_no: null` for 關羽/趙雲/諸葛亮/呂布/周瑜 and
  an `identity_note` "link to public card data is not evidenced"; its own
  `audit_note` (added in issue #9) says stat/skill `VERIFIED` labels are
  maintainer-attested. It is part of the reconstruction, so it cannot verify
  `GameData.java` (circular).
- `DropCardData.java:255` states 〖美髯公〗 = No.180 and a different low-star 關羽 =
  No.80, with no URL. It is the only card-number claim for any roster member.
  Recorded as a lead: read the public No.180 page and compare.
- `EVIDENCE_CORRECTION.md` / `MASTER_DIRECT_MATCHES.md`: test-master rows are excluded.

## Per-general summary

| # | General | Java says | Audit result | Action |
|---|---|---|---|---|
| 0 | 劉備 孝行息子 | RECONSTRUCTED | Rarity literal `重建`; skill `暫定測試技能`; leader `（暫定）` | Stats/skills/leader -> CUSTOM; name/faction/troop -> RECONSTRUCTED |
| 1 | 關羽 美髯公 | ORIGINAL_VERIFIED | All UNRESOLVED; No.180 lead only | Downgrade |
| 2 | 張飛 闘鬼 | RECONSTRUCTED | Same as 劉備 | CUSTOM |
| 3 | 趙雲 一陣の風 | ORIGINAL_VERIFIED | All UNRESOLVED; public card 【長坂単騎駆】 exists but epithet differs | Downgrade |
| 4 | 諸葛亮 臥龍雌伏 | ORIGINAL_VERIFIED | All UNRESOLVED; public card 【三顧の禮】 exists, epithet differs | Downgrade |
| 5 | 曹操 東郡太守 | RECONSTRUCTED | Placeholder | CUSTOM |
| 6 | 孫權 若き虎の覚悟 | RECONSTRUCTED | Placeholder | CUSTOM |
| 7 | 呂布 戦鬼 | ORIGINAL_VERIFIED | All UNRESOLVED; no matching public title located | Downgrade |
| 8 | 貂蟬 美女連環 | RECONSTRUCTED | Placeholder | CUSTOM |
| 9 | 周瑜 小覇王盟友 | ORIGINAL_VERIFIED | All UNRESOLVED; no matching public title located | Downgrade |

## Source conflicts / leads

1. Card titles on the fandom wiki are `【epithet】name`. The titles surfaced for
   趙雲 (`長坂単騎駆`) and 諸葛亮 (`三顧の禮`) do not match Java's `一陣の風` and
   `臥龍雌伏`. They could be different cards of the same person (wiki has many per
   person) or a naming error in Java. Unresolved.
2. A search-engine summary attributes skill `武神咆哮` to 【軍神】關羽 (a different
   card from 美髯公). It neither confirms nor contradicts `青龍咆哮`. Unverified.
3. Skill `長坂一騎駆け` resembles the title `長坂単騎駆`; this is naming similarity
   (LOW) and not identity evidence.

## Cross-cutting findings worth fixing on integration

1. **Rarity is not a rarity for 5 cards** (`重建` stored in the rarity slot) and the
   star notation of the others is inconsistent: 關羽 `★`x6, 呂布 `★`x6+`☆`, 趙雲/周瑜
   `★`x5+`☆`, 諸葛亮 `★`x4+`☆`x2. Use an integer rarity plus a separate provenance.
2. **REGRESSION RISK - downgrading changes gameplay.** `GameData.java:153-170`
   applies invented leader bonuses to any card with `!verifiedOriginal`. If 關羽 (id 1),
   趙雲 (3) or 諸葛亮 (4) become RECONSTRUCTED, they instantly gain
   `SHU attackers x1.25` (ids 1-3) and 諸葛亮 gains `combo>=4 x1.20`, neither of which
   is in their described leader skills. 呂布 (7) and 周瑜 (9) are handled before
   the guard and 關羽's incoming 0.5, 趙雲's none and 諸葛亮's HP 1.5 are
   ungated, so those are safe. Integrate by keying gameplay behaviour off
   explicit per-card data, not `verifiedOriginal`, and drop the `case 1,2,3,4`
   bonuses for cards that have a described (non-invented) leader skill.
3. **Description vs. engine mismatches (unverified against battle code):**
   呂布 skill claims 無視防禦 but uses the same `SKILL_SELF_ATK` type as 關羽;
   周瑜 leader text promises a follow-up attack but only the x2.2 is implemented;
   趙雲 leader has no numeric effect (correct: value unknown).
4. **Suspicious values to verify first:** 呂布 skill CD 35 with min 35; 呂布 Lv1
   recovery 22 vs Lv.Max 176; 關羽 max level 99 (all others 50); 關羽 leader text
   "enemy Shu deals half damage" on a Shu leader.
5. Uniform max level 50 and round numbers in the 5 placeholders are defaults.

## Recommended production changes (do in a reviewed step)

- Set provenance RECONSTRUCTED for ids 1, 3, 4, 7, 9 (do not keep ORIGINAL_VERIFIED).
- Set provenance CUSTOM for id 0, 2, 5, 6, 8 skills/stats/leaders; replace `重建`
  in the rarity slot.
- **Keep** every numeric value for playability. **Remove** nothing now; the
  only removals to consider later are the `!verifiedOriginal` leader bonuses noted above.
- Do not restore any value to ORIGINAL_VERIFIED until a per-field URL for the exact
  card page (and its epithet) is recorded here.

## Next steps to actually verify

1. From a browser or an unblocked network, open the card pages on
   `https://3pz.fandom.com/zh/wiki/…` for 美髯公 (No.180), 一陣の風, 臥龍雌伏, 戦鬼,
   小覇王盟友 and record Lv1/Max stats, skill text, CD and leader text per field.
2. Cross-check each against a second independent source (e.g. an archived
   Japanese wiki), then set `HIGH` evidence rows in `GENERAL_PROVENANCE.csv`.
3. Do not derive identity from art; do not use `reconstructed_master_v1.json`.
