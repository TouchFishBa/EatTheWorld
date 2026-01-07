# Implementation notes (guidelines)

## Recommended feature decomposition
1) Exhaustion tuning
- Goal: change how fast hunger drains due to sprint/jump/damage etc.
- Prefer: server-side `PlayerTickEvent` adjustments; keep logic centralized.

2) Natural regeneration overhaul
- Goal: gate/alter natural heal (e.g., require saturation, or slow it).
- Prefer: detect/regulate heal triggers; if hard to intercept cleanly, implement custom regen and suppress vanilla regen where possible.

3) Food consumption adjustments (per-item)
- Goal: rebalance foods without modifying item definitions.
- Prefer: `LivingEntityUseItemEvent.Finish` + config mapping keyed by `ResourceLocation`.

## Compatibility strategy
- Default: do not hardcode other mods' item classes.
- Provide:
  - global multipliers
  - per-item overrides by id
  - optional tag-based groups (if later implemented via tag checks)

## Safety rules
- Run logic on server side only (`!level.isClientSide`).
- Avoid heavy allocations in tick; cache config-derived maps/sets.
- Keep changes reversible via config toggles.
