# Search patterns (1.20.1 Forge)

Use these keywords first; prefer ripgrep/search over guessing.

## Vanilla/Mojang classes & methods
- `FoodData`
- `getFoodData`
- `addExhaustion`
- `exhaustion` (field names vary; use method-based search)
- `tick`
- `naturalRegeneration`
- `heal`

## Forge events (common entry points)
- `TickEvent.PlayerTickEvent`
- `LivingEntityUseItemEvent.Finish`
- `LivingHealEvent`
- `LivingHurtEvent`
- `PlayerEvent.Clone`

## Consumption / food-related
- `finishUsingItem`
- `use`
- `FoodProperties`

## Project-local
- `@SubscribeEvent`
- `MinecraftForge.EVENT_BUS`
- `ModConfigEvent`
