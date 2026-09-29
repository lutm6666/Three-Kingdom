# Reconstructed test-master rows — not identity evidence

Source file on the local research machine:

`assets/data/master.json`

SHA-256:

`d7a4eb4de6b1b00d0a7320622f041e9a159573ba38d08738ead793865ab83eaf`

## Provenance correction

This file is a **reconstructed parser-shaped test master**, not an original
downloaded SanPazu master.

The three rows in `MASTER_DIRECT_MATCHES.csv` were deliberately created during
the offline reconstruction. Their record keys were borrowed from sprite IDs
already present in the APK so that the test cards would not reference missing
art.

The earlier reconstruction record explicitly warned that borrowing those keys
did **not** mean the sprites depicted the Wiki characters whose data was placed
under them.

Consequently the rows are useful for parser/build regression only. They must not
be used to prove:

- sprite identity,
- serial ID -> public card No.,
- character name,
- evolution/variant identity.

See `EVIDENCE_CORRECTION.md`.

The original unmodified game master is still missing.
