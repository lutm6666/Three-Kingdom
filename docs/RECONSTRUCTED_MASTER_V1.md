# Reconstructed Master v1

This file is the canonical data source for the offline rebuild:

`app/src/main/assets/data/reconstructed_master_v1.json`

It is **not** claimed to be the lost original server Master.

## Why reconstruction is now the primary path

Reverse engineering of the original Taiwan 1.2.5.2 client confirmed this pipeline:

```text
master*.bin
  -> zlib/deflate
  -> JSON
  -> RFMasterDataManager
```

The original asset hosts are no longer resolvable. Wayback did not preserve the
Master payload, and the useful Common Crawl search did not yield the asset hosts.
No local or Library copy of `master.bin`, `masterN.bin`, or `list.bin` was found.

Development therefore proceeds with a provenance-first reconstructed Master
rather than inventing an unverifiable "original" payload.

## Confidence rules

- VERIFIED: directly supported by original APK/parser/native evidence or independently checked public game data.
- RECONSTRUCTED: required for a playable rebuild, but the exact original value/algorithm is unavailable.
- PARTIAL: identity or some fields are verified while important details remain unknown.
- UNKNOWN: insufficient evidence.

## v1 contents

- 6 character records with fully verified identity/stat endpoints
- 3 early Cao Cao route stages
- enemy ATK/TURN verification flags
- explicitly reconstructed HP/DEF, rewards, encounter weighting, and seed/drop defaults
- 0 verified original-art identity mappings

## Gate

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/validate_reconstructed_master.ps1
```

A Master change must pass this validator before merge.
