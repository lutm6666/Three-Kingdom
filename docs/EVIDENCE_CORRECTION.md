# Evidence-chain correction

## Status

The three mappings previously promoted to HIGH in issue #3 / PR #4 are
**withdrawn**:

- `ch_15200025_l -> No.102 -> 〖大斧〗徐晃`
- `ch_35300032_l -> No.260 -> 于禁`
- `ch_56400035_l -> No.350 -> 成公英`

They are now UNVERIFIED again.

## Why the earlier proof was circular

The file used as `assets/data/master.json` was not an original downloaded game
master. It was a parser-shaped offline test master created during the earlier
reconstruction work.

The original reconstruction note explicitly states that its three
`character_data` keys were borrowed from sprite IDs that happened to exist in
the APK only to prevent missing-art failures, and that those keys did **not**
establish that the sprites depicted the Wiki characters whose test stats were
inserted.

Therefore:

1. a sprite ID was selected first;
2. a chosen Wiki card's stats/name were inserted under that borrowed key;
3. later matching those same stats back to the public Wiki/card database merely
   recovered the card that had been inserted.

That is circular evidence and cannot identify the artwork.

## Evidence that remains valid

The original unmodified `libgame.so` does contain the resource templates:

```text
character/S/ch_%s_s.png
character/M/ch_%s_m.png
character/L/ch_%s_l.png
character/W/ch_%s_w.png
character/LL/ch_%s_ll.png
```

and native symbols for character serial-ID access. This is useful structural
evidence about how art resources are addressed, but it does not by itself map a
serial ID to a public card number or character name.

The original binary also exposes the old master-download path and names including
`tool/resource`, `master.bin`, `master%d.bin`,
`pzgame02.app-master.com.tw/server/dl/`, and
`pzpatch.app-master.com.tw/server/dl/`.

## New identity gate

A character-art mapping may be promoted to HIGH only when the serial/card/name
relationship comes from **original game data or another independent source that
was not produced by this reconstruction**, followed by an independent
cross-check.

Reconstructed/test master rows are prohibited as identity evidence.

Artwork appearance alone remains insufficient.
