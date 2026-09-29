# Claude independent asset review (issue #3)

Scope: the three direct master matches in `MASTER_DIRECT_MATCHES.csv`.
No identity was taken from artwork appearance. The other 13 rows stay UNVERIFIED.

## Method
1. Stem check: the asset-id stem must equal the `character_data` master key.
2. Public source: card number -> card name, with the card's stats, from a public
   fan database of 三国志パズル大戦 (Sangokushi Puzzle Taisen), URL pattern
   `https://p.rakda3.net/sanpuz/b<card_no>`.
3. Cross-check: the source's stats must reproduce the master row's stats. The
   stat match is what ties the master `card_no` to the public numbering.

## Results

| asset_id | stem = master_key | card_no | public name | source stats vs master | confidence |
|---|---|---|---|---|---|
| ch_15200025_l | 15200025 = 15200025 | 102 | 【大斧】徐晃 (Xu Huang) | rarity 3, cost 6, Lv25, HP 170→1040, ATK 86→269, heal 5→21: all equal | HIGH |
| ch_35300032_l | 35300032 = 35300032 | 260 | 于禁 (Yu Jin), awakens to No.261 | rarity 2, cost 3, Lv20, HP 145, ATK 96, heal 31: all equal | HIGH |
| ch_56400035_l | 56400035 = 56400035 | 350 | 成公英 (Cheng Gongying), awakens to No.351 | rarity 2, cost 5, Lv15, HP 142, ATK 159, heal 43: all equal | HIGH |

Sources:
- https://p.rakda3.net/sanpuz/b102
- https://p.rakda3.net/sanpuz/b260
- https://p.rakda3.net/sanpuz/b350

## Uncertainty
- The stem = master_key check used the CSV rows only. `assets/data/master.json` is
  not in the repo, so the master data was not re-read.
- The link between the `sgpz_` assets and 三国志パズル大戦 is not stated in the repo.
  It rests on the exact stat matches across 5-6 fields on all three cards, which
  is very unlikely by chance.
- For No.260 and No.350 the source lists only Lv1 stats. Master max-level stats
  (290/192/62 and 256/334/86) were not verified. Cost, rarity, max level and Lv1
  stats all match.
- Skill and leader-skill IDs (990002, 990101, 990001) could not be compared to the
  source's skill names, and skill effects were not checked.
- The database is a single fan-run site, but the name is confirmed by numeric
  stats from the master, not by the site alone. A second independent site was not
  found (the game ended service in 2016).
- Variant: 102 is the 【大斧】 version. 260 and 350 are the base (pre-awakening)
  cards, since 261 and 351 are the awakened forms.
- The candidate names have not been checked against the current Java data, and no
  Java was changed. Integration needs the reviewed step from `COLLAB_HANDOFF.md`.

## Second independent pass (Claude Code, 2026-09-30)

This pass was run after the three mappings were already integrated in `343d35f`.
It deliberately avoided `p.rakda3.net` and used a second, separately run fan wiki:
三国志パズル大戦 攻略Wiki【さんぱず攻略】 on Gamerch (`https://gamerch.com/sanpuzz/`).
Card numbers came from its faction lists (魏 `629752`, 群 `630042`) and its
No.1-500 index (`631024`); stats came from each card page.

| card_no | Gamerch page | name | Lv1 HP/ATK/heal | max HP/ATK/heal | max Lv | skill / leader skill | vs master |
|---|---|---|---|---|---|---|---|
| 102 | [630116](https://gamerch.com/sanpuzz/630116) | 【大斧】徐晃 | 170/86/5 | 1040/269/21 | 25 | 不敗の名将 / 斥候展開 | all equal; skill 990002 + leader 990101 both present |
| 260 | [630152](https://gamerch.com/sanpuzz/630152) | 于禁 | 145/96/31 | 290/192/62 | 20 | none / none | all equal; skill and leader both empty |
| 350 | [634526](https://gamerch.com/sanpuzz/634526) | 成公英 | 142/159/43 | 256/334/86 | 15 | 流麗な策 / none | all equal; skill 990001 present, leader empty |

Result: all three confirmed. HIGH stands.

What this pass adds over the first:
- A second independent card-number → identity source.
- Max-level stats for No.260 and No.350 now match the master (the first pass could
  only check Lv1).
- Skill presence lines up with the master: a card has a skill / leader skill on
  Gamerch exactly when the master row has a `skill_id` / `leader_skill_id`.
  Skill IDs themselves still cannot be mapped to names.

Discrepancy found:
- No.350's awakened form. The first pass recorded "awakens to No.351". Gamerch lists
  the awakened 【西涼忠臣】成公英 as No.365 (群 list, `630042`).
  The `variant` column now names the awakened card instead of a number. This does
  not affect the No.350 identity.

Caveats still open:
- `assets/data/master.json` is still not in the repo, so the stem = master_key check
  still relies on `MASTER_DIRECT_MATCHES.csv`.
- Both sources are fan wikis for a game that ended service on 2016-09-20. Their stat
  tables agree with each other and with the master, but they may share an upstream
  origin, such as the in-game encyclopedia.
- Gamerch's card pages do not show the card number themselves. The number comes from
  the list pages that link to them.
