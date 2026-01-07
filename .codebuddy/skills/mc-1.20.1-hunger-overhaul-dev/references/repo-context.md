# Repo context (auto-derived)

- Loader: Forge (ForgeGradle 6.x)
- Minecraft: 1.20.1
- Forge: 47.2.0
- Java: 17

Current template code (not yet HungerOverhaul):
- Main mod class: `src/main/java/com/example/examplemod/ExampleMod.java`
  - `@Mod(ExampleMod.MODID)`
  - Uses `MinecraftForge.EVENT_BUS.register(this)` and MOD bus listeners.
  - Registers COMMON config via `ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC)`
- Config class: `src/main/java/com/example/examplemod/Config.java`
  - Uses `ForgeConfigSpec` and `ModConfigEvent` to cache config values.
- `mods.toml` exists at `src/main/resources/META-INF/mods.toml` and uses Gradle property expansion.

Notes:
- `gradle.properties` still contains placeholder `mod_id=examplemod` etc. Rename/package cleanup can be done later; it does not block implementing hunger logic.
