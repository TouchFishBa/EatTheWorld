---
name: mc-1.20.1-hunger-overhaul-dev
description: >-
  用于在 Minecraft 1.20.1 Forge(47.x) mod 工程中实现/迁移 HungerOverhaul 类机制：调整饥饿/饱和/消耗/自然回血/食物收益；要求配置化、服务端权威、兼容其他食物mod；并提供定位入口与回归测试工作流。
---

## 目的
- 在 1.20.1 Forge 工程中实现“饥饿机制重做/增强”类功能（而不是仅加物品）。
- 将所有数值与开关做成可配置（COMMON config），并保证多人联机一致性。

## 何时触发本 skill
- 用户提出：饥饿/饱和/Exhaustion/自然回血/食物营养值/食物分级/负面效果/耐力消耗/疾跑跳跃消耗 等需求。
- 用户提出：复刻某个旧版 HungerOverhaul 思路到 1.20.1。
- 用户提出：与其他食物/生存类 mod 的兼容策略。

## 工作流（按顺序执行）
### 1. 先判定工程与约束
- 确认加载器：Forge 1.20.1（通过 `build.gradle` 中 `net.minecraftforge.gradle` 与 `minecraft_version/forge_version`）。
- 确认当前 mod 基础骨架：入口 `@Mod` 主类、是否已存在 `Config`、是否已有事件订阅。
- 明确目标改动属于以下哪一类（决定实现路径）：
  1) 仅“倍率/阈值/开关”改消耗与回血（优先用事件/Tick，侵入最小）。
  2) 需要“按物品覆盖食物收益”（通过消费完成事件 + 配置映射实现；避免硬改 `FoodProperties`）。
  3) 必须重写 `FoodData`/自然回血核心逻辑（最后手段：AT/核心注入；先评估维护成本）。

### 2. 入口定位（先搜索再改）
- 在映射源码/依赖中用关键词定位：
  - `FoodData`、`addExhaustion`、`getFoodData`、`tick`、`naturalRegeneration`、`heal`。
- 在 Forge 事件侧搜索/使用：
  - `TickEvent.PlayerTickEvent`（服务端每tick统一调整）
  - `LivingEntityUseItemEvent.Finish`（食物吃完后追加/修正收益）
  - 需要时再考虑：`LivingHurtEvent`、`LivingHealEvent`、`PlayerEvent.Clone`（死亡重生数据处理）

### 3. 配置化（强制要求）
- 所有数值走 COMMON config：倍率、阈值、开关、每物品覆盖表。
- 为“每物品覆盖”设计稳定格式：`minecraft:bread=nutrition:?,saturation:?` 或分字段列表；加载时校验 `ResourceLocation` 是否存在。
- 运行时读取用缓存字段（在 `ModConfigEvent` 中同步到静态变量），避免每tick频繁访问 spec。

### 4. 服务端权威与同步策略
- 逻辑在服务端执行；客户端只做 UI 表现。
- 若改动涉及玩家属性/能力，确保不会被客户端预测导致回弹。

### 5. 最小侵入实现建议（默认路径）
- 默认优先：
  - 用 `PlayerTickEvent` 在服务端按配置对 Exhaustion/自然回血条件做“加成/抑制”。
  - 用 `LivingEntityUseItemEvent.Finish` 对“吃完食物”追加效果：例如额外 Exhaustion、削弱饱和、冷却等。
- 只有当事件无法实现目标时，才进入“核心逻辑重写/注入”路径，并在 PR/变更说明中记录原因。

### 6. 回归测试清单（每次改完必须跑）
- 单人/局域网/专服：生存模式下疾跑/跳跃/受击/回血/吃食物。
- `gamerule naturalRegeneration` 开关两种情况下的表现。
- 进出存档、死亡重生、切维度后机制是否保持一致。

## 输出要求（对实现代码的约束）
- 不要硬编码散落数值；必须集中到 `Config`。
- 不要重写大量 vanilla 逻辑除非必要；优先事件驱动。
- 对外兼容：优先用 item id/标签映射而不是写死具体 mod 物品类。

## 参考资料
- `references/repo-context.md`
- `references/search-patterns.md`
- `references/implementation-notes.md`
