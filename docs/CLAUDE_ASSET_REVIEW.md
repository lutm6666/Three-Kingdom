# Claude independent asset review (issue #3) — RETRACTED

## Retraction

The identity conclusions in the original issue #3 / PR #4 review are no longer
accepted as evidence.

The review correctly matched public card pages to the numeric/stat rows supplied
in `MASTER_DIRECT_MATCHES.csv`, but the provenance of those rows was later
rechecked: they came from a **reconstructed test master**, not an original
downloaded game master.

Those test rows had intentionally borrowed existing APK sprite IDs to avoid
missing-art failures. The character identities/stats were then inserted under
those borrowed keys. Matching the inserted stats back to public pages therefore
recovered the test cards by construction and did not identify the artwork.

Withdrawn conclusions:

- `ch_15200025_l -> No.102 -> 〖大斧〗徐晃`
- `ch_35300032_l -> No.260 -> 于禁`
- `ch_56400035_l -> No.350 -> 成公英`

All three are UNVERIFIED again.

The public card pages themselves remain useful references for those card numbers,
but they do not contain the APK serial IDs needed to identify these art assets.

See `EVIDENCE_CORRECTION.md` for the corrected evidence rule.
