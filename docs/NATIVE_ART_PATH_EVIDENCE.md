# Native character-art path evidence

Source binary on the local research machine:

`lib/armeabi/libgame.so`

SHA-256:

`a01a5b2d38aa2220779ebad50087b344b0040036f7e5761e4f87b577129a63fd`

Printable strings extracted from the original unmodified game binary include:

```text
character/S/ch_%s_s.png
character/M/ch_%s_m.png
character/L/ch_%s_l.png
character/W/ch_%s_w.png
character/LL/ch_%s_ll.png
```

The same native binary exposes C++ symbols including:

```text
CharacterParameter::getSerialId()
CharacterParameter::setSerialId(...)
RFMasterDataManager::getCharacterData(...)
CharacterParameter::getCharacterParameterFromSerialId(...)
```

This is valid structural evidence that character art is addressed through a
character serial/master identifier represented by `%s`.

## What this does NOT prove

It does not identify any of the current 16 art assets by character name or public
card number.

The three mappings previously used as control samples have been retracted because
their `character_data` rows came from a reconstructed test master whose keys were
deliberately borrowed from existing sprite IDs. See `EVIDENCE_CORRECTION.md`.

## Original master-download evidence

The same original binary contains:

```text
tool/resource
master.bin
master%d.bin
data/master.json
masterdataversion
http://pzgame02.app-master.com.tw/server/dl/
http://pzpatch.app-master.com.tw/server/dl/
pzgame02.app-master.com.tw/server/
```

Thumb disassembly of `CCBSceneLoading::downloadMasterFile(float)` shows URL
construction using the master base URL plus `master.bin`, with divided-master
downloads using `master%d.bin`.

The old hosts no longer resolve in DNS, and an Internet Archive CDX query for
their `server/*` paths returned no saved HTTP-200 entries.

The current research goal is therefore to recover an independent original
serial-to-card identity source rather than infer names from the artwork.
