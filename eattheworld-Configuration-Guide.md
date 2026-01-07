# Eat The World Mod Configuration Guide

## How to Modify Configuration

### Step 1: Locate the Configuration File

1. Place `eattheworld-1.0.0.jar` into your game's `mods` folder
2. Launch the game once (the configuration file will be automatically generated)
3. Close the game
4. Find the configuration file at:
   ```
   .minecraft/config/eattheworld-common.toml
   ```
   
   **Common Path Examples:**
   - HMCL Launcher: `.minecraft/versions/your_version_name/config/eattheworld-common.toml`
   - PCL2 Launcher: `.minecraft/versions/your_version_name/config/eattheworld-common.toml`
   - Official Launcher: `C:\Users\your_username\AppData\Roaming\.minecraft\config\eattheworld-common.toml`

### Step 2: Edit the Configuration File

1. Open `eattheworld-common.toml` with **Notepad** or **any text editor**
2. Find the configuration option you want to modify
3. Change the value after the equals sign
4. Save the file

**Example: Enable Food Decrement System**
```toml
# Find this line
[food.decrement]
    enabled = false

# Change to
[food.decrement]
    enabled = true
```

### Step 3: Restart the Game

You must **restart the game completely** for changes to take effect! Simply exiting the world is not enough.

## Configuration File Example

A complete configuration file looks like this:
```toml
[core]
    enabled = true

[debug]
    enabled = false
    logFoodEvents = false
    logRegen = false

[food.decrement]
    enabled = false
    marker1Effect = 1.0
    marker2Effect = 0.8
    # ... more options
```

## Quick Configuration Guide

### I Only Want to Enable Food Decrement System (Recommended for Beginners)

1. Open `eattheworld-common.toml`
2. Find the `[food.decrement]` section
3. Change `enabled = false` to `enabled = true`
4. Save and restart the game

That's it! Keep all other options at their default values.

### I Want to Adjust Difficulty

**Easy Mode (Food Effect Decreases Slowly):**
```toml
[food.decrement]
    enabled = true
    marker1Effect = 1.0
    marker2Effect = 0.9
    marker3Effect = 0.8
    marker4Effect = 0.7
    marker5Effect = 0.6
    recoveryTicks = 12000  # 10 minutes to recover
```

**Hard Mode (Food Effect Decreases Quickly):**
```toml
[food.decrement]
    enabled = true
    marker1Effect = 1.0
    marker2Effect = 0.7
    marker3Effect = 0.5
    marker4Effect = 0.3
    marker5Effect = 0.1
    recoveryTicks = 48000  # 40 minutes to recover
    speedPenalty2 = 0.7
    speedPenalty3 = 0.5
    speedPenalty4 = 0.3
    speedPenalty5 = 0.1
```

## Configuration Options Explained

### 1. Core Switch
- **core.enabled** (Default: true)
  - Master switch that controls whether all mod gameplay logic is enabled

### 2. Debug Options
- **debug.enabled** (Default: false)
  - Debug switch; when enabled, outputs more logs
- **debug.logFoodEvents** (Default: false)
  - Log food consumption events (server-side only)
- **debug.logRegen** (Default: false)
  - Log health regeneration logic (server-side only)

### 3. Action Exhaustion System
- **hunger.actions.enabled** (Default: true)
  - Enable action exhaustion system (sprinting/jumping/combat/damage increase exhaustion)
- **hunger.actions.sprintExhaustionPerSecond** (Default: 0.0, Range: 0.0-1000.0)
  - Additional exhaustion per second while sprinting; 0 means no additional exhaustion
- **hunger.actions.jumpExhaustion** (Default: 0.0, Range: 0.0-1000.0)
  - Additional exhaustion per jump; 0 means no additional exhaustion
- **hunger.actions.attackExhaustion** (Default: 0.0, Range: 0.0-1000.0)
  - Additional exhaustion per entity attack; 0 means no additional exhaustion
- **hunger.actions.hurtExhaustionMultiplier** (Default: 0.0, Range: 0.0-1000.0)
  - Additional exhaustion when taking damage = damage × multiplier; 0 means no additional exhaustion
- **hunger.actions.hurtExhaustionMaxPerHit** (Default: 10.0, Range: 0.0-1000.0)
  - Maximum additional exhaustion per hit (prevents high damage from causing excessive exhaustion)

### 4. Natural Regeneration System
- **regen.enabled** (Default: false)
  - Enable custom natural regeneration system
- **regen.disableVanillaNaturalRegen** (Default: true)
  - Whether to disable vanilla natural regeneration (to avoid stacking)
- **regen.requireNaturalRegenGamerule** (Default: false)
  - Whether to require gamerule naturalRegeneration=true to allow regeneration
- **regen.minFoodLevel** (Default: 18, Range: 0-20)
  - Player hunger level must reach this threshold to allow regeneration
- **regen.requireSaturation** (Default: true)
  - Whether saturation must reach threshold to allow regeneration
- **regen.minSaturation** (Default: 0.0, Range: 0.0-20.0)
  - Minimum saturation threshold
- **regen.intervalTicks** (Default: 80, Range: 1-72000)
  - Regeneration interval in ticks (20 ticks = 1 second)
- **regen.healAmount** (Default: 1.0, Range: 0.0-1000.0)
  - Health restored per regeneration (1.0 = half heart, 2.0 = full heart)
- **regen.exhaustionCost** (Default: 6.0, Range: 0.0-1000.0)
  - Additional exhaustion per regeneration

### 5. Global Food Benefit Multiplier
- **food.global.enabled** (Default: false)
  - Enable global food benefit multiplier adjustment
- **food.global.nutritionMultiplier** (Default: 1.0, Range: 0.0-1000.0)
  - Hunger value multiplier (1.0 = vanilla)
- **food.global.saturationMultiplier** (Default: 1.0, Range: 0.0-1000.0)
  - Saturation multiplier (1.0 = vanilla)

### 6. AppleSkin Compatibility
- **food.appleskin.compat.enabled** (Default: true)
  - Ensure AppleSkin displays food values synchronized with this mod's multiplier adjustments

### 6.5. Food Container Return
- **food.containerReturn.enabled** (Default: false)
  - When enabled, eating from bento box returns the food's container item (e.g., mushroom stew returns bowl, milk bucket returns bucket)
  - Disabled by default; enable manually if needed

### 7. Per-Item Override
- **food.override.enabled** (Default: false)
  - Enable per-item override functionality
- **food.override.entries** (Default: empty list)
  - Format: 'modid:item_name,nutrition,saturation_modifier,exhaustion(optional)'
  - Example: 'minecraft:apple,8,0.6' or 'minecraft:apple,8,0.6,0.5'

### 8. Food Effect Decrement System ⭐Core Feature
- **food.decrement.enabled** (Default: false)
  - Enable food effect decrement system (repeated consumption of same food reduces effectiveness)

#### Effect Multipliers (Affects hunger and saturation recovery)
- **food.decrement.marker1Effect** (Default: 1.0, Range: 0.0-1.0)
  - First consumption effect multiplier (100%)
- **food.decrement.marker2Effect** (Default: 0.8, Range: 0.0-1.0)
  - Second consumption effect multiplier (80%)
- **food.decrement.marker3Effect** (Default: 0.6, Range: 0.0-1.0)
  - Third consumption effect multiplier (60%)
- **food.decrement.marker4Effect** (Default: 0.4, Range: 0.0-1.0)
  - Fourth consumption effect multiplier (40%)
- **food.decrement.marker5Effect** (Default: 0.2, Range: 0.0-1.0)
  - Fifth and subsequent consumption effect multiplier (20%, minimum limit)

#### Eating Speed Multipliers (Affects eating animation duration)
- **food.decrement.speedPenalty1** (Default: 1.0, Range: 0.0-1.0)
  - First consumption speed multiplier (100%, normal speed)
- **food.decrement.speedPenalty2** (Default: 0.8, Range: 0.0-1.0)
  - Second consumption speed multiplier (80%, slightly slower)
- **food.decrement.speedPenalty3** (Default: 0.6, Range: 0.0-1.0)
  - Third consumption speed multiplier (60%)
- **food.decrement.speedPenalty4** (Default: 0.4, Range: 0.0-1.0)
  - Fourth consumption speed multiplier (40%)
- **food.decrement.speedPenalty5** (Default: 0.2, Range: 0.0-1.0)
  - Fifth and subsequent speed multiplier (20%, slowest)

#### Marker Recovery Mechanism
- **food.decrement.recoveryEnabled** (Default: true)
  - Enable automatic food marker recovery mechanism
- **food.decrement.recoveryTicks** (Default: 24000, Range: 20-2400000)
  - After how many game ticks all food markers decrease by 1
  - Reference: 20 ticks = 1 second, 1200 ticks = 1 minute, 24000 ticks = 1 game day (20 minutes)

## In-Game Commands

### Reset Food Markers
**Command:** `/eattheworld reset`

**Functions:**
- Clear all food marker data
- Reset consumption counts
- Clear last consumption times
- Immediately sync to client

**Use Cases:**
- Food markers display abnormally (e.g., always showing "marker recovering soon")
- Want to restart the food diversity challenge
- Mod version update causes data incompatibility

**Note:** This command can only be executed by players in-game, not in console.

## Bento Box Features

### Crafting Recipe
```
Iron Ingot  Iron Ingot  Iron Ingot
Paper       Paper       Paper
Iron Ingot  Iron Ingot  Iron Ingot
```

### Usage
- **Normal Right-click**: Eat from the bento box
- **Sneak + Right-click**: Open bento box GUI to add/remove food

### Full Feature Support
- ✅ **Potion Effects**: Enchanted golden apples, pufferfish, and other buff foods fully supported
- ✅ **Container Return**: Mushroom stew returns bowl, milk bucket returns bucket, etc.
- ✅ **Mod Compatibility**: Supports special effects and container returns for all mod foods

### Eating Modes
1. **Smart Mode** (Default)
   - Prioritizes foods never eaten before
   - If all eaten, selects the one eaten least
   - If equal, selects the one with highest hunger value

2. **Sequential Mode**
   - Eats foods in slot order
   - GUI shows blue highlight for next eating position

### Switch Modes
Click the button in the top-left corner of the bento box GUI to switch modes

## Frequently Asked Questions

### Q: I modified the config but it didn't take effect?
A: You must **completely close and restart the game**. Simply exiting the world is not enough.

### Q: Where is the configuration file?
A: It's at `.minecraft/config/eattheworld-common.toml`. If you can't find it, launch the game once to auto-generate it.

### Q: I made a mistake in the config, what should I do?
A: Delete the `eattheworld-common.toml` file and restart the game. It will auto-generate a new one with default settings.

### Q: What software can I use to edit the config file?
A: Any text editor works, such as:
- Windows Notepad
- Notepad++
- VSCode
- Sublime Text

### Q: Is the food decrement system disabled by default?
A: Yes, you need to manually set `food.decrement.enabled = true` in the config file to enable it.

### Q: Do both server and client need this mod installed?
A: Yes, this is a dual-sided mod. Both server and client need to have it installed.

### Q: Whose config takes effect in multiplayer?
A: The server-side config takes effect. Client-side config only affects local display.

## Recommended Configurations

### Light Difficulty (Recommended for Beginners)
```toml
[food.decrement]
enabled = true
marker1Effect = 1.0
marker2Effect = 0.9
marker3Effect = 0.8
marker4Effect = 0.7
marker5Effect = 0.6
recoveryTicks = 12000  # 10 minutes to recover
```

### Medium Difficulty (Balanced)
```toml
[food.decrement]
enabled = true
marker1Effect = 1.0
marker2Effect = 0.8
marker3Effect = 0.6
marker4Effect = 0.4
marker5Effect = 0.2
recoveryTicks = 24000  # 20 minutes to recover
```

### Hard Difficulty (Challenge)
```toml
[food.decrement]
enabled = true
marker1Effect = 1.0
marker2Effect = 0.7
marker3Effect = 0.5
marker4Effect = 0.3
marker5Effect = 0.1
recoveryTicks = 48000  # 40 minutes to recover
speedPenalty2 = 0.7
speedPenalty3 = 0.5
speedPenalty4 = 0.3
speedPenalty5 = 0.1
```

## Important Notes

1. Configuration file is auto-generated on first game launch
2. Changes require **complete game restart** to take effect (simply exiting the world is not enough)
3. If the config file is corrupted, delete it and restart the game to auto-regenerate
4. Food decrement system is disabled by default; enable manually
5. Bento box eating speed is affected by food markers (if food decrement system is enabled)
6. In multiplayer, server-side config takes effect

## Complete Configuration File Example

This is a complete configuration example with food decrement system enabled:

```toml
# Eat The World Configuration File

[core]
    # Master switch
    enabled = true

[debug]
    # Debug options (usually not needed)
    enabled = false
    logFoodEvents = false
    logRegen = false

[hunger.actions]
    # Action exhaustion system (enabled by default but exhaustion values are 0)
    enabled = true
    sprintExhaustionPerSecond = 0.0
    jumpExhaustion = 0.0
    attackExhaustion = 0.0
    hurtExhaustionMultiplier = 0.0
    hurtExhaustionMaxPerHit = 10.0

[regen]
    # Natural regeneration system (disabled by default)
    enabled = false
    disableVanillaNaturalRegen = true
    requireNaturalRegenGamerule = false
    minFoodLevel = 18
    requireSaturation = true
    minSaturation = 0.0
    intervalTicks = 80
    healAmount = 1.0
    exhaustionCost = 6.0

[food.global]
    # Global food multiplier (disabled by default)
    enabled = false
    nutritionMultiplier = 1.0
    saturationMultiplier = 1.0

[food.appleskin]
    # AppleSkin compatibility
    compat.enabled = true

[food.override]
    # Per-item override (disabled by default)
    enabled = false
    entries = []

[food.decrement]
    # Food decrement system (core feature, needs manual enabling)
    enabled = true
    
    # Effect multipliers (affects hunger and saturation recovery)
    marker1Effect = 1.0   # 1st: 100%
    marker2Effect = 0.8   # 2nd: 80%
    marker3Effect = 0.6   # 3rd: 60%
    marker4Effect = 0.4   # 4th: 40%
    marker5Effect = 0.2   # 5th+: 20%
    
    # Eating speed multipliers (affects eating animation duration)
    speedPenalty1 = 1.0   # 1st: normal speed
    speedPenalty2 = 0.8   # 2nd: slightly slower
    speedPenalty3 = 0.6   # 3rd: slower
    speedPenalty4 = 0.4   # 4th: much slower
    speedPenalty5 = 0.2   # 5th+: slowest
    
    # Marker recovery mechanism
    recoveryEnabled = true
    recoveryTicks = 24000  # 24000 ticks = 20 minutes = 1 game day
```

## Modification Examples

### Example 1: Only Enable Food Decrement with Default Difficulty
```toml
[food.decrement]
    enabled = true  # Only change this line
```

### Example 2: Enable Food Decrement + Adjust to Easy Difficulty
```toml
[food.decrement]
    enabled = true
    marker2Effect = 0.9  # Change to 90%
    marker3Effect = 0.8  # Change to 80%
    marker4Effect = 0.7  # Change to 70%
    marker5Effect = 0.6  # Change to 60%
    recoveryTicks = 12000  # Change to 10 minutes recovery
```

### Example 3: Enable Food Decrement + Enable Natural Regeneration System
```toml
[food.decrement]
    enabled = true

[regen]
    enabled = true  # Enable custom regeneration
    minFoodLevel = 18  # Only regenerate when hunger is 18+
```

---

**Tip:** If you're unsure what to change, the simplest approach is to just change `food.decrement.enabled` to `true` and keep everything else at default!
