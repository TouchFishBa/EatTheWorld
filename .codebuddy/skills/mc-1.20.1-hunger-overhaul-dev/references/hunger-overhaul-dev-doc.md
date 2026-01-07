# HungerOverhaul（MC 1.20.1 Forge）开发文档（功能全量版）

> 目标：在 1.20.1 Forge(47.x) 上实现与经典 HungerOverhaul 思路一致的“生存饥饿系统重做/增强”模组。
>
> 设计原则：**配置化**（所有数值可调）、**服务端权威**（多人一致）、**最小侵入**（优先事件驱动）、**兼容优先**（不硬依赖其他食物 mod）。

---

## 0. 文档使用方式（分步结构）

本模组按“模块化 + 配置分区”组织。后续调整时按以下步骤定位：

1. 先在“1. 模块总览”找到要改的模块。
2. 阅读该模块的“参数”小节，确认可调项与默认值。
3. 阅读该模块“实现逻辑”小节，确认挂载点（事件/注入点）与算法。
4. 若涉及联动，查看“依赖关系”小节与“跨模块依赖图”。
5. 修改后按“12. 回归测试清单”验证。

---

## 1. 模块总览（完整列表）

### 1.1 Core（核心/基础设施）
- **CORE-1 配置系统**（Config）
- **CORE-2 运行期缓存与热重载**（Config Cache / Reload）
- **CORE-3 日志与调试开关**（Logging/Debug）
- **CORE-4 网络与同步（可选）**（Client Sync, 若需要 UI/提示与服务端参数一致）

### 1.2 Hunger（饥饿与消耗机制）
- **HUN-1 Exhaustion 乘区/衰减策略**
- **HUN-2 饥饿/饱和下限保护与钳制**（Clamp/Guard）
- **HUN-3 行为消耗调整**（疾跑/跳跃/攻击/受击/挖掘等）
- **HUN-4 饥饿惩罚策略**（低饥饿时减速/挖掘疲劳/视效等，可选）

### 1.3 Regeneration（自然回血/恢复系统）
- **REG-1 自然回血开关与条件重写**
- **REG-2 自然回血频率/消耗公式**
- **REG-3 低饥饿掉血（Hardcore-ish，可选）**

### 1.4 Food Rebalance（食物收益与效果修改）
- **FOOD-1 全局食物收益倍率**（nutrition/saturation 全局缩放）
- **FOOD-2 按物品覆盖**（per-item override：营养、饱和、额外 Exhaustion、效果）
- **FOOD-3 按标签/分组覆盖**（tag/group override，可选）
- **FOOD-4 进食后副作用/增益**（概率性中毒、虚弱、缓慢等）
- **FOOD-5 冷却/连续进食惩罚**（可选）

### 1.5 Eating Experience（进食体验：动画/时长/音效/提示）
- **EAT-1 进食时长调整**（use duration）
- **EAT-2 进食动画变化（客户端）**（第一人称/第三人称手臂动画，可选）
- **EAT-3 进食打断规则**（移动/受击/切换物品时是否中断，可选）
- **EAT-4 反馈与提示**（吃“劣质/变质/过量”提示，可选）

### 1.6 Nutrition System（营养系统：多维营养/均衡/惩罚）
- **NUT-1 营养维度定义**（例如：蛋白/碳水/脂肪/蔬果/乳制等）
- **NUT-2 食物→营养映射**（按物品/标签映射）
- **NUT-3 营养衰减与历史窗口**（时间衰减/滑动窗口）
- **NUT-4 均衡奖励**（生命上限/抗性/移速等增益，可选）
- **NUT-5 失衡惩罚**（挖掘疲劳/虚弱等，可选）

### 1.7 Compatibility（兼容与集成）
- **COMP-1 与其他食物 mod 兼容**（只用 id/tag，不依赖类）
- **COMP-2 与自然回血规则/游戏规则兼容**（`gamerule naturalRegeneration`）
- **COMP-3 与药水/状态效果兼容**（再生、饥饿、中毒等）

### 1.8 Commands & Debug（命令与调试，可选）
- **CMD-1 打印当前玩家饥饿/饱和/消耗/营养状态**
- **CMD-2 重载配置/营养映射（若支持）**

---

## 2. 配置分区与命名规范（建议）

> Forge 侧建议使用 `COMMON` 配置；客户端动画/提示可用 `CLIENT`。

- `common.toml`
  - `core.*`
  - `hunger.*`
  - `regen.*`
  - `food.*`
  - `nutrition.*`
  - `compat.*`
  - `debug.*`
- `client.toml`（可选）
  - `eatclient.*`（动画/提示）

通用规则：
- 所有倍率以 `1.0` 为“不改”，小于 0 一律非法。
- 所有概率为 `[0,1]`。
- 所有 tick 频率以整数 tick 表达（20 tick = 1 秒）。

---

## 3. CORE-1 配置系统（Config）

### 描述
提供所有功能模块的可调参数，并在运行期缓存为静态字段供逻辑快速读取。

### 参数（建议）
- `core.enabled`：bool，默认 `true`（总开关）
- `core.serverAuthority`：bool，默认 `true`（服务端权威执行）
- `core.failSafeMode`：bool，默认 `true`（异常时不影响原版逻辑）

### 实现逻辑（建议）
- 使用 `ForgeConfigSpec` 定义参数。
- 在 `ModConfigEvent` 中将 spec 值同步到静态缓存字段。
- 所有 tick/事件处理入口先判断 `core.enabled`。

### 依赖关系
- 被所有模块依赖。

---

## 4. CORE-2 运行期缓存与热重载（Config Cache / Reload）

### 描述
将复杂配置（如 per-item 覆盖表、营养映射）解析为高效结构（Map/Set），避免每 tick 解析字符串。

### 参数（建议）
- `core.cache.itemOverrideMaxEntries`：int，默认 `4096`
- `core.cache.logInvalidEntries`：bool，默认 `true`

### 实现逻辑（建议）
- `Map<ResourceLocation, FoodOverride>`：按物品 id 映射。
- `Map<TagKey<Item>, FoodOverride>`（可选）：按 tag 映射。
- 重载发生在 `ModConfigEvent.Reloading` 或自定义命令触发时。

### 依赖关系
- 被 FOOD-2、FOOD-3、NUT-2 依赖。

---

## 5. HUN-1 Exhaustion 乘区/衰减策略

### 描述
调整玩家 Exhaustion（消耗）增长速度，从而改变饥饿下降的整体节奏。

### 参数（建议）
- `hunger.exhaustion.enabled`：bool，默认 `true`
- `hunger.exhaustion.multiplier`：double，默认 `1.0`（对新增 exhaustion 的倍率）
- `hunger.exhaustion.flatAddPerSecond`：double，默认 `0.0`（每秒额外增加 exhaustion，可用于持续消耗）
- `hunger.exhaustion.maxClampPerTick`：double，默认 `4.0`（每 tick 最大增加量，防止异常爆表）

### 实现逻辑（建议）
- 事件入口（服务端）：`TickEvent.PlayerTickEvent`（END phase）。
- 方案 A（推荐，最小侵入）：
  - 记录玩家上一次 exhaustion 值，计算本 tick 增量 
  - 将增量乘以 `multiplier`，再写回差值（需谨慎避免递归/重复）。
- 方案 B（备选）：
  - 在可拦截的行为事件里直接按配置添加/减少 exhaustion（HUN-3）。

### 示例
- 想让饥饿掉得更快：`hunger.exhaustion.multiplier=1.5`

### 依赖关系
- 依赖 CORE（配置/缓存）。
- 与 HUN-3 互补：HUN-3 更精细，HUN-1 更全局。

---

## 6. HUN-3 行为消耗调整（疾跑/跳跃/攻击/受击/挖掘等）

### 描述
对特定行为追加 exhaustion（或修改原有消耗），使“行动成本”更符合生存难度目标。

### 参数（建议）
- `hunger.actions.enabled`：bool，默认 `true`
- `hunger.actions.sprintExhaustionPerSecond`：double，默认 `0.0`
- `hunger.actions.jumpExhaustion`：double，默认 `0.0`
- `hunger.actions.attackExhaustion`：double，默认 `0.0`
- `hunger.actions.hurtExhaustionMultiplier`：double，默认 `0.0`（按受伤伤害额外乘区）

### 实现逻辑（建议）
- 疾跑：每 tick 判断 `player.isSprinting()` 累加 `perSecond/20`。
- 跳跃：检测 `player.onGround()`→离地边沿（需要记录上 tick 状态）。
- 攻击：监听 `LivingHurtEvent`（source 为 player 造成）或 `AttackEntityEvent`（取决于版本/可用事件）。
- 受击：`LivingHurtEvent`（entity 为 player）按伤害值追加。

### 依赖关系
- 依赖 CORE。
- 与 HUN-1 同时开启时，注意“重复加成”：HUN-1 改全局增量，HUN-3 额外添加行为增量。

---

## 7. REG-1 自然回血开关与条件重写

### 描述
重写/限制原版自然回血触发条件（例如：必须饱和>0、必须饥饿>=18、或改为更慢）。

### 参数（建议）
- `regen.enabled`：bool，默认 `true`
- `regen.requireNaturalRegenGamerule`：bool，默认 `true`
- `regen.minFoodLevel`：int，默认 `18`（触发最低饥饿值）
- `regen.requireSaturation`：bool，默认 `true`
- `regen.minSaturation`：double，默认 `0.0`

### 实现逻辑（建议）
- 服务端 tick 中判断：
  - `gamerule naturalRegeneration`（若 require）
  - 玩家当前饥饿/饱和值
- 触发逻辑选择：
  - 方案 A：实现“自定义回血”，并尽量抑制原版自然回血（需要找到可抑制入口，否则会叠加）。
  - 方案 B：不改回血，只通过额外 exhaustion 让回血变得更“昂贵”（侵入小但控制弱）。

### 依赖关系
- 依赖 CORE。
- 与 FOOD/HUN 的数值共同决定“回血成本”。

---

## 8. REG-2 自然回血频率/消耗公式

### 描述
控制回血速度、每次回血量、以及对应的饥饿/饱和/消耗代价。

### 参数（建议）
- `regen.intervalTicks`：int，默认 `80`（每 4 秒一次）
- `regen.healAmount`：double，默认 `1.0`（半颗心=1.0）
- `regen.exhaustionCost`：double，默认 `3.0`
- `regen.useSaturationFirst`：bool，默认 `true`（优先消耗饱和）

### 实现逻辑（建议）
- 每玩家维护一个计时器（capability/attachment 或简单 map，取决于你是否需要持久化）。
- 到达 interval 后：
  - 满足 REG-1 条件则 `player.heal(healAmount)`
  - 然后 `player.getFoodData().addExhaustion(exhaustionCost)`

### 依赖关系
- 依赖 REG-1 与 CORE。

---

## 9. FOOD-1 全局食物收益倍率

### 描述
对所有“进食获得的饥饿/饱和”做统一缩放，快速改变整体食物价值。

### 参数（建议）
- `food.global.enabled`：bool，默认 `true`
- `food.global.nutritionMultiplier`：double，默认 `1.0`
- `food.global.saturationMultiplier`：double，默认 `1.0`

### 实现逻辑（建议）
- 在“进食完成”事件中读取本次食物的 `FoodProperties`，计算原收益，再按倍率得出目标收益。
- 通过“追加 exhaustion / 追加饱和/饥饿”方式做差值修正。
  - 注意：直接改 `FoodProperties` 往往不可行（很多物品是静态定义）。

### 依赖关系
- 依赖 CORE、CORE-2（如果同时支持 per-item）。

---

## 10. FOOD-2 按物品覆盖（per-item override）

### 描述
对指定物品（通过 `namespace:path`）单独设定进食收益与副作用。

### 参数（建议）
- `food.override.enabled`：bool，默认 `true`
- `food.override.entries`：list<string>，默认空
  - 建议格式（示例）：
    - `minecraft:bread nutrition=3 saturation=0.4 exhaustion=0.0`
    - `minecraft:rotten_flesh nutrition=2 saturation=0.1 effect=minecraft:hunger@200,1 chance=0.8`

### 实现逻辑（建议）
- 配置加载时解析为结构：
  - `nutrition`（int）
  - `saturation`（double）
  - `exhaustion`（double，额外）
  - `effects[]`（id、持续、等级、概率）
- 进食完成时：
  - 若命中覆盖：按覆盖值修正最终收益（可选择“覆盖模式”或“增量模式”）。

### 依赖关系
- 依赖 CORE-2 解析与缓存。
- 与 FOOD-1 同时启用时，明确优先级：建议 `per-item > tag > global`。

---

## 11. EAT-2 进食动画变化（客户端，可选）

### 描述
改变进食时的第一人称/第三人称动画表现（例如更慢的抬手、更明显的咀嚼动作），增强反馈。

### 参数（建议，client.toml）
- `eatclient.animation.enabled`：bool，默认 `false`
- `eatclient.animation.profile`：enum，默认 `VANILLA_PLUS`（预设曲线：`VANILLA`/`VANILLA_PLUS`/`HEAVY`）
- `eatclient.animation.amplitude`：double，默认 `1.0`

### 实现逻辑（建议）
- 客户端侧监听渲染相关事件（Forge 客户端事件）对手臂/物品位移做插值曲线调整。
- 若实现成本过高，可先只做 `EAT-1`（时长）与提示反馈。

### 依赖关系
- 依赖 CORE（总开关）与客户端配置。
- 与服务端逻辑无强依赖（纯表现）。

---

## 12. NUT-1 ~ NUT-5 营养系统（完整子功能）

> 说明：营养系统是“跨多次进食的长期状态”，建议实现为玩家附加数据（capability/attachment），并提供衰减与奖励/惩罚。

### 12.1 NUT-1 营养维度定义
**描述**：定义营养维度集合与每维的取值范围。

**参数（建议）**
- `nutrition.enabled`：bool，默认 `false`
- `nutrition.dimensions`：list<string>，默认 `[]`（如 `protein,carb,fat,vegetable,fruit`）
- `nutrition.maxPerDimension`：double，默认 `100.0`
- `nutrition.decayPerDay`：double，默认 `10.0`（每 Minecraft 日衰减量）

**实现逻辑**
- 每个维度维护一个浮点值 
- 每 tick/每秒/每天进行衰减（推荐按“世界时间”折算）

### 12.2 NUT-2 食物→营养映射
**描述**：指定物品/标签对应的营养增量。

**参数（建议）**
- `nutrition.map.itemEntries`：list<string>
  - 示例：`minecraft:beef protein=8 fat=4`
- `nutrition.map.tagEntries`：list<string>
  - 示例：`#forge:vegetables vegetable=6`

**实现逻辑**
- 进食完成事件中：根据物品 id 或 tag 命中，累加营养值并 clamp 到上限。

### 12.3 NUT-3 营养衰减与历史窗口
**描述**：支持“短期窗口均衡”（最近 N 天/最近 N 次进食）或纯连续衰减。

**参数（建议）**
- `nutrition.window.enabled`：bool，默认 `true`
- `nutrition.window.days`：int，默认 `3`

**实现逻辑**
- 记录最近若干天的营养摄入累计（按天桶），计算均衡度。

### 12.4 NUT-4 均衡奖励
**描述**：当各维度均衡时提供增益。

**参数（建议）**
- `nutrition.bonus.enabled`：bool，默认 `true`
- `nutrition.bonus.threshold`：double，默认 `0.7`（均衡度阈值）
- `nutrition.bonus.effects`：list<string>
  - 示例：`minecraft:movement_speed@200,0`

**实现逻辑**
- 周期性检查均衡度，给玩家短 buff（可刷新）

### 12.5 NUT-5 失衡惩罚
**描述**：长期单一饮食触发 debuff。

**参数（建议）**
- `nutrition.penalty.enabled`：bool，默认 `true`
- `nutrition.penalty.threshold`：double，默认 `0.3`
- `nutrition.penalty.effects`：list<string>

**实现逻辑**
- 均衡度过低 → 施加 debuff（短 buff 循环刷新）

### 依赖关系
- 依赖 CORE、CORE-2（映射解析）。
- 与 FOOD-2 同入口（进食完成事件）但逻辑独立。

---

## 13. 功能依赖关系（跨模块）

### 13.1 依赖图（文本版）
- `CORE-1/2/3` → 所有模块
- `HUN-*` ↔ `REG-*`（共同影响“回血成本/生存压力”）
- `FOOD-1/2/3/4/5` → 间接影响 `REG-*`（食物恢复能力决定回血频率）
- `NUT-*` 依赖 `FOOD` 的“进食完成入口”，但不依赖其数值（可独立开关）
- `EAT-*`：
  - `EAT-1` 影响玩法节奏（更慢吃东西）→ 间接影响 `HUN/REG`
  - `EAT-2/4` 仅表现 → 不影响服务端平衡（除非提示依赖服务端同步）

### 13.2 冲突/叠加注意
- 同时启用 `FOOD-1` 与 `FOOD-2/3`：必须规定优先级（建议 `per-item > tag > global`）。
- 同时启用 `HUN-1` 与 `HUN-3`：需要避免“既全局乘又额外加导致过强”。
- `REG` 若采用“自定义回血”，需要确认不会与原版自然回血叠加。

---

## 14. 示例配置片段（建议格式）

> 说明：以下是“文档示例”，最终键名以实际 `ForgeConfigSpec` 为准。

- 更快掉饥饿：
  - `hunger.exhaustion.multiplier=1.5`
- 限制自然回血：
  - `regen.minFoodLevel=18`
  - `regen.requireSaturation=true`
  - `regen.intervalTicks=120`
- 单独削弱面包：
  - `food.override.entries=["minecraft:bread nutrition=2 saturation=0.2"]`
- 开启营养系统：
  - `nutrition.enabled=true`
  - `nutrition.dimensions=["protein","carb","fat","vegetable","fruit"]`

---

## 15. 回归测试清单（必须）

- 单人：
  - 吃多种食物，验证 FOOD 覆盖/倍率生效
  - 疾跑/跳跃/战斗，验证消耗变化
  - 自然回血在不同饥饿/饱和下是否符合预期
- 多人（专服/局域网）：
  - 同步一致性：客户端显示与服务端实际一致
- 边界：
  - 死亡重生、切维度、退出重进
  - `gamerule naturalRegeneration` 开/关
  - 与 `Regeneration`、`Hunger`、`Poison` 等状态效果叠加

---

## 16. 实现落地顺序（建议里程碑）

1. CORE（配置/缓存/日志）
2. HUN（先 HUN-3 行为消耗，再视需要加 HUN-1 全局乘）
3. REG（先实现“自定义回血 + 成本”，再处理抑制原版叠加问题）
4. FOOD（先 global，再 per-item）
5. NUT（先维度+映射+衰减，再做奖惩）
6. EAT 动画（纯客户端，可最后做）

