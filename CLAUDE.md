# Claude project instructions

This repository is a reconstruction/research project.

## Primary Claude role

Focus on independent evidence-based research and review:
- Asset ID ↔ card / character ID relationships
- Master-data structure and cross references
- source-backed gameplay values
- code review and regression-risk identification

## Evidence rule

Do not identify a character from artwork appearance alone.
Every proposed mapping must record concrete evidence and a confidence level
in `docs/ASSET_MAPPING.csv`.

Confidence definitions:
- HIGH: a serial/card/name relation from original game data (or another independent identity source), plus an independent cross-check.
- MEDIUM: one required identity link is indirect but still source-backed.
- LOW: pattern inference, naming convention, visual resemblance, or other non-direct evidence only.
- UNVERIFIED: insufficient evidence to identify the card.

Reconstructed/test master rows are prohibited as identity evidence. They may be
used only for parser/build regression unless their values are independently
proven to originate from the original live game data.

Production Java data should not be changed from a LOW/MEDIUM-confidence guess.
Only reviewed HIGH-confidence mappings may enter production Java data.
Prefer changing research/docs first and leave integration for a reviewed step.

## Safety / repository hygiene

Do not add API keys, OAuth tokens, local.properties, APKs, extracted
commercial-game PNGs, build outputs, or mechanical test artifacts.

The Android project must remain buildable without original artwork.
