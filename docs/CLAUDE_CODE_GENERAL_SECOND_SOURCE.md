# Second-source check of the five sourced generals (issue #11)

Author: Claude Code (local session), 2026-09-30. Docs only. No Java, Gradle,
workflow, image or binary file was changed.

## Why this exists

Issue #11 was audited twice and the two runs disagree:

| run | branch | could read a card database? | conclusion |
|---|---|---|---|
| 1 | `claude/issue-11-20260930-1357` | yes, `p.rakda3.net/sanpuz` | 諸葛亮 and 周瑜 match fully; 關羽, 趙雲 and 呂布 match with open items |
| 2 | `claude/issue-11-20260930-1402` | no, the wiki it tried returned HTTP 402 | nothing could be verified |

Run 2 failed on access, not on the values. Run 1 is the usable one, but it is
single-source. This document adds a second source for the five generals run 1
could match, and uses it to settle run 1's open items.

## Source

三国志パズル大戦 攻略Wiki【さんぱず攻略】 on Gamerch, a fan wiki run separately from
rakda3. Each card has its own page. Card numbers come from the faction list pages
(蜀 `630040`, 呉 `630039`, 群 `630042`), because the card pages do not print the number.

## Result

Java values are from `GameData.ROSTER` at `c74c113`. They are identical to the
five playable rows in `reconstructed_master_v1.json`.

| id | Java card | Gamerch page | Lv1 HP/ATK/REC | max HP/ATK/REC | max Lv | skill, cooldown | leader | vs Java |
|---|---|---|---|---|---|---|---|---|
| 1 | 關羽 美髯公 | [633590](https://gamerch.com/sanpuzz/633590) | 520/300/60 | 3208/1230/191 | 99 | 青龍咆哮, 30→20 | 義侠の武 | all equal |
| 3 | 趙雲 一陣の風 | [633631](https://gamerch.com/sanpuzz/633631) | 280/249/59 | 1260/872/177 | 50 | 長坂一騎駆け, 20→14 | 神威に至る槍術 | all equal |
| 4 | 諸葛亮 臥龍雌伏 | [633558](https://gamerch.com/sanpuzz/633558) | 287/264/53 | 718/554/111 | 50 | 奇門遁甲, 10→5 | 八卦陣 | all equal |
| 7 | 呂布 戦鬼 | [634609](https://gamerch.com/sanpuzz/634609) | 341/277/22 | 1705/970/176 | 50 | 天下無双, 35 | 吠虎の猛勇 | all equal |
| 9 | 周瑜 小覇王盟友 | [630973](https://gamerch.com/sanpuzz/630973) | 320/291/76 | 1280/1019/243 | 50 | 孫呉の業火, 28→15 | 借刀殺人の計 | all equal |

Faction, troop type and star count also match for all five. Skill and leader
descriptions match in meaning: x50 蜀 attack, x30 defence-ignoring, x8 team 呉
attack, 蜀 HP x1.5, all ATK x3.5 at full HP, 鬼謀 ATK x2.2.

## Run 1's open items

| open item in run 1 | second source says | status |
|---|---|---|
| 關羽 `skillCdMin=20` had no source | Cooldown is listed as 30→20 | resolved, Java is right |
| 呂布 Lv1 stats: rakda3 read as HP 50 / ATK 341 / REC 277 | Lv1 is HP 341 / ATK 277 / REC 22 | resolved, Java is right. The rakda3 read was shifted by one column |
| 趙雲 leader name: rakda3 read No.156 as 神速の槍術 | No.156 and No.157 both list 神威に至る槍術 | still open. Java agrees with Gamerch, and the two sources disagree |
| 趙雲 leader effect value (run 1's fetch said 30%) | Text only: move time 大きく延びる, damage taken 大きく減少. No number | still unknown. Do not implement a number |

## Things this check found

- 關羽 card number. rakda3 has 【美髯公】関羽 at No.180. The Gamerch 蜀 list read
  it as No.197. Every stat, the skill and the leader skill match, so it is the
  same card. One of the two numbers is wrong, or my read of the list is. Do not
  store a card number for 關羽 until this is checked by hand.
- 呂布 cooldown. Gamerch lists 35 with no reduced value, which fits Java's 35/35.
- Not modelled in Java, on both sources: limit-break max stats (關羽
  3528/1430/311) and the awakened skill variants (諸葛亮's adds a one-turn
  damage halving).

## What this does and does not establish

- It establishes that these five generals' numbers match two fan databases. That
  meets "independent identity source plus an independent cross-check" for the
  **stat and skill values**.
- It does not touch art identity. None of the 16 `ch_*_l` assets is involved.
- Both sources are fan wikis for a game that closed on 2016-09-20. They may share
  an upstream, such as the in-game encyclopedia. No original game master was seen.
- I read the pages through a summarising fetch tool, not raw HTML. The one-column
  shift in run 1's 呂布 read shows that tool can misread a table. The numbers
  above were each read from a single card page and agree with Java digit for
  digit, which a misread is unlikely to produce. A manual spot check is still
  worth doing.
- 劉備, 張飛, 曹操, 孫權 and 貂蟬 were not checked here. Both audit runs class
  their stats and skills as CUSTOM test data, and I have no reason to disagree.

## Recommendation

1. Use run 1's CSV (`docs/GENERAL_PROVENANCE.csv` on `claude/issue-11-20260930-1357`)
   as the base, not run 2's.
2. In it, change 關羽 `active_cooldown` and 呂布 `lv1_stats` from UNRESOLVED to
   verified, citing both sources.
3. Keep 趙雲 `leader_name` UNRESOLVED (sources disagree) and `leader_effect`
   unknown. Keep Java's current behaviour of applying no reduction.
4. Add a `source_url_2` column so each verified field records both URLs.
5. Both audits warn that flipping `provenance` changes gameplay through the
   `verifiedOriginal` gates. With these five confirmed, there is no need to
   downgrade them, which avoids that regression for now.
