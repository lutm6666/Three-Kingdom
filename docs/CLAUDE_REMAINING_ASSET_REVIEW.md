# Claude research pass on the remaining 13 asset IDs (issue #6)

Result: **0 of 13 resolved. All 13 stay UNVERIFIED.** No identity was taken from artwork.

## Why nothing could be resolved
1. **No structural link.** `MASTER_DIRECT_MATCHES.md` says the 13 stems are not
   `character_data` keys in the master snapshot. `assets/data/master.json` is not in the
   repo, so I could not search it for related rows (evolution or variant links).
   `libgame.so` shows `ch_%s_l.png` is filled from a serial ID, but the 13 IDs are
   not known to be valid master keys.
2. **No public source keyed by serial ID.** The one public source
   (https://p.rakda3.net/sanpuz/b102, also b260 and b350) lists card No., image
   number (`0102.png`), skill type ID (866) and stats. It has no `15200025`-style
   serial. For the controls, the stat match to master rows is what tied serial to
   card No. Without master rows for the 13 IDs there are no stats to compare.
3. **The serial has no derivable card No.** Controls: 15200025→102,
   35300032→260, 56400035→350. No arithmetic relation is visible. Any digit
   pattern (for example `x4200004` repeated across four prefixes) is LOW-confidence
   inference only, so I did not use it.
4. A web search for the serial numbers returned nothing relevant.

## Per-row status
All 13 rows (`ch_14200004_l`, `ch_23100019_l`, `ch_24200004_l`, `ch_25600027_l`,
`ch_34200004_l`, `ch_35300025_l`, `ch_35400027_l`, `ch_44200004_l`,
`ch_45500027_l`, `ch_45500028_l`, `ch_53600019_l`, `ch_54100023_l`,
`ch_55600025_l`): UNVERIFIED. Only the evidence note in `ASSET_MAPPING.csv` changed.
The three HIGH controls are untouched.

## What would unblock this
- Master rows for these IDs from wherever they are defined. Check other master tables
  (card/evolution/variant tables, or a download-data table) and the local
  `master.json`. Look for a table that maps serial ID to card No.
- Or the `%s` values the native code passes for each card, which would expose the
  mapping. Then compare stats with the public pages.
- Once a row has a card No. and matching stats on b<No>, it is HIGH by the same method
  as issue #3.
