# Eat The World Mod 配置文件说明

## 如何修改配置

### 第一步：找到配置文件

1. 将 `eattheworld-1.0.0.jar` 放入游戏的 `mods` 文件夹
2. 启动游戏一次（配置文件会自动生成）
3. 关闭游戏
4. 找到配置文件位置：
   ```
   .minecraft/config/eattheworld-common.toml
   ```
   
   **常见路径示例：**
   - HMCL启动器：`.minecraft/versions/你的版本名称/config/eattheworld-common.toml`
   - PCL2启动器：`.minecraft/versions/你的版本名称/config/eattheworld-common.toml`
   - 官方启动器：`C:\Users\你的用户名\AppData\Roaming\.minecraft\config\eattheworld-common.toml`

### 第二步：编辑配置文件

1. 使用**记事本**或**任何文本编辑器**打开 `eattheworld-common.toml`
2. 找到你想修改的配置项
3. 修改等号后面的值
4. 保存文件

**示例：启用食物递减系统**
```toml
# 找到这一行
[food.decrement]
    enabled = false

# 改为
[food.decrement]
    enabled = true
```

### 第三步：重启游戏

修改配置后必须**重启游戏**才能生效！

## 配置文件示例

完整的配置文件看起来像这样：
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
    # ... 更多配置项
```

## 快速配置指南

### 我只想启用食物递减系统（推荐新手）

1. 打开 `eattheworld-common.toml`
2. 找到 `[food.decrement]` 部分
3. 将 `enabled = false` 改为 `enabled = true`
4. 保存并重启游戏

就这么简单！其他配置项保持默认值即可。

### 我想调整难度

**轻松模式（食物效果递减较慢）：**
```toml
[food.decrement]
    enabled = true
    marker1Effect = 1.0
    marker2Effect = 0.9
    marker3Effect = 0.8
    marker4Effect = 0.7
    marker5Effect = 0.6
    recoveryTicks = 12000  # 10分钟恢复
```

**困难模式（食物效果递减很快）：**
```toml
[food.decrement]
    enabled = true
    marker1Effect = 1.0
    marker2Effect = 0.7
    marker3Effect = 0.5
    marker4Effect = 0.3
    marker5Effect = 0.1
    recoveryTicks = 48000  # 40分钟恢复
    speedPenalty2 = 0.7
    speedPenalty3 = 0.5
    speedPenalty4 = 0.3
    speedPenalty5 = 0.1
```

## 配置项说明

### 1. 核心开关
- **core.enabled** (默认: true)
  - 总开关，控制整个mod的所有玩法逻辑是否启用

### 2. 调试选项
- **debug.enabled** (默认: false)
  - 调试开关，开启后会输出更多日志
- **debug.logFoodEvents** (默认: false)
  - 记录进食相关事件（仅服务端）
- **debug.logRegen** (默认: false)
  - 记录回血相关逻辑（仅服务端）

### 3. 行为消耗系统
- **hunger.actions.enabled** (默认: true)
  - 启用行为消耗系统（疾跑/跳跃/战斗/受击等会额外增加消耗）
- **hunger.actions.sprintExhaustionPerSecond** (默认: 0.0, 范围: 0.0-1000.0)
  - 疾跑时每秒追加的exhaustion，0表示不追加
- **hunger.actions.jumpExhaustion** (默认: 0.0, 范围: 0.0-1000.0)
  - 每次跳跃追加的exhaustion，0表示不追加
- **hunger.actions.attackExhaustion** (默认: 0.0, 范围: 0.0-1000.0)
  - 每次攻击实体时追加的exhaustion，0表示不追加
- **hunger.actions.hurtExhaustionMultiplier** (默认: 0.0, 范围: 0.0-1000.0)
  - 受击时追加exhaustion = 伤害值 × 倍率，0表示不追加
- **hunger.actions.hurtExhaustionMaxPerHit** (默认: 10.0, 范围: 0.0-1000.0)
  - 单次受击最多追加多少exhaustion（防止高伤害造成消耗暴涨）

### 4. 自然回血系统
- **regen.enabled** (默认: false)
  - 启用自定义自然回血系统
- **regen.disableVanillaNaturalRegen** (默认: true)
  - 是否禁用原版自然回血（避免叠加）
- **regen.requireNaturalRegenGamerule** (默认: false)
  - 是否要求gamerule naturalRegeneration=true才允许回血
- **regen.minFoodLevel** (默认: 18, 范围: 0-20)
  - 玩家饥饿值达到该阈值才允许回血
- **regen.requireSaturation** (默认: true)
  - 是否要求饱和度达到阈值才允许回血
- **regen.minSaturation** (默认: 0.0, 范围: 0.0-20.0)
  - 最低饱和度阈值
- **regen.intervalTicks** (默认: 80, 范围: 1-72000)
  - 回血间隔tick（20 tick = 1秒）
- **regen.healAmount** (默认: 1.0, 范围: 0.0-1000.0)
  - 每次回复的生命值（1.0=半颗心，2.0=一颗心）
- **regen.exhaustionCost** (默认: 6.0, 范围: 0.0-1000.0)
  - 每次回血追加的exhaustion

### 5. 全局食物收益倍率
- **food.global.enabled** (默认: false)
  - 启用全局食物收益倍率调整
- **food.global.nutritionMultiplier** (默认: 1.0, 范围: 0.0-1000.0)
  - 饥饿值倍率（1.0=原版）
- **food.global.saturationMultiplier** (默认: 1.0, 范围: 0.0-1000.0)
  - 饱和度倍率（1.0=原版）

### 6. AppleSkin兼容
- **food.appleskin.compat.enabled** (默认: true)
  - 确保AppleSkin显示的食物数值与本mod的倍率调整保持同步

### 6.5. 食物容器返回
- **food.containerReturn.enabled** (默认: false)
  - 启用后从饭盒进食会返回食物的容器物品（如蘑菇煲返回碗、牛奶桶返回桶等）
  - 默认关闭，需要手动启用

### 7. 按物品覆盖
- **food.override.enabled** (默认: false)
  - 启用按物品覆盖功能
- **food.override.entries** (默认: 空列表)
  - 格式: 'modid:item_name,nutrition,saturation_modifier,exhaustion(可选)'
  - 示例: 'minecraft:apple,8,0.6' 或 'minecraft:apple,8,0.6,0.5'

### 8. 食物效果递减系统 ⭐核心功能
- **food.decrement.enabled** (默认: false)
  - 启用食物效果递减系统（连续食用同种食物时效果会递减）

#### 效果倍率（影响饥饿值和饱和度恢复）
- **food.decrement.marker1Effect** (默认: 1.0, 范围: 0.0-1.0)
  - 首次食用效果倍率（100%）
- **food.decrement.marker2Effect** (默认: 0.8, 范围: 0.0-1.0)
  - 第二次食用效果倍率（80%）
- **food.decrement.marker3Effect** (默认: 0.6, 范围: 0.0-1.0)
  - 第三次食用效果倍率（60%）
- **food.decrement.marker4Effect** (默认: 0.4, 范围: 0.0-1.0)
  - 第四次食用效果倍率（40%）
- **food.decrement.marker5Effect** (默认: 0.2, 范围: 0.0-1.0)
  - 第五次及以后效果倍率（20%，最低限制）

#### 进食速度倍率（影响进食动画时长）
- **food.decrement.speedPenalty1** (默认: 1.0, 范围: 0.0-1.0)
  - 首次食用速度倍率（100%，正常速度）
- **food.decrement.speedPenalty2** (默认: 0.8, 范围: 0.0-1.0)
  - 第二次食用速度倍率（80%，稍慢）
- **food.decrement.speedPenalty3** (默认: 0.6, 范围: 0.0-1.0)
  - 第三次食用速度倍率（60%）
- **food.decrement.speedPenalty4** (默认: 0.4, 范围: 0.0-1.0)
  - 第四次食用速度倍率（40%）
- **food.decrement.speedPenalty5** (默认: 0.2, 范围: 0.0-1.0)
  - 第五次及以后速度倍率（20%，最慢）

#### 标记恢复机制
- **food.decrement.recoveryEnabled** (默认: true)
  - 启用食物标记自动恢复机制
- **food.decrement.recoveryTicks** (默认: 24000, 范围: 20-2400000)
  - 经过多少游戏刻后所有食物标记值-1
  - 参考: 20 ticks = 1秒，1200 ticks = 1分钟，24000 ticks = 1游戏日（20分钟）

## 游戏内命令

### 重置食物标记
**命令：** `/eattheworld reset`

**功能：**
- 清空所有食物的标记数据
- 重置食用次数记录
- 清除最后食用时间
- 立即同步到客户端

**使用场景：**
- 食物标记显示异常（如一直显示"标记即将恢复"）
- 想要重新开始食物多样性挑战
- 更新mod版本后数据不兼容

**注意：** 此命令只能由玩家在游戏中执行，不能在控制台使用。

## 饭盒功能

### 合成配方
```
铁锭 铁锭 铁锭
纸   纸   纸
铁锭 铁锭 铁锭
```

### 使用方法
- **普通右键**: 从饭盒中进食
- **潜行+右键**: 打开饭盒GUI，放入/取出食物

### 完整功能支持
- ✅ **药水效果**: 附魔金苹果、河豚等带buff的食物完全支持
- ✅ **容器返回**: 蘑菇煲返回碗、牛奶桶返回桶等
- ✅ **mod兼容**: 支持所有mod食物的特殊效果和容器返回

### 进食模式
1. **智能模式**（默认）
   - 优先选择未食用过的食物
   - 都吃过则选择次数最少的
   - 次数相同则选择饥饿度最高的

2. **顺序模式**
   - 按照槽位顺序轮流进食
   - GUI中蓝色高亮显示下一个进食位置

### 切换模式
在饭盒GUI中点击左上角的按钮切换模式

## 常见问题

### Q: 我修改了配置但没有生效？
A: 必须**完全关闭游戏后重启**才能生效，不能只是退出世界。

### Q: 配置文件在哪里？
A: 在 `.minecraft/config/eattheworld-common.toml`，如果找不到，先启动一次游戏让它自动生成。

### Q: 我改错了配置怎么办？
A: 删除 `eattheworld-common.toml` 文件，重启游戏会自动生成新的默认配置。

### Q: 可以用什么软件编辑配置文件？
A: 任何文本编辑器都可以，比如：
- Windows自带的记事本
- Notepad++
- VSCode
- Sublime Text

### Q: 食物递减系统默认是关闭的吗？
A: 是的，需要手动在配置文件中设置 `food.decrement.enabled = true` 才能启用。

### Q: 服务器和客户端都需要安装这个mod吗？
A: 是的，这是一个双端mod，服务器和客户端都需要安装。

### Q: 多人游戏中谁的配置生效？
A: 服务器端的配置生效，客户端配置只影响本地显示。

## 推荐配置

### 轻度难度（推荐新手）
```toml
[food.decrement]
enabled = true
marker1Effect = 1.0
marker2Effect = 0.9
marker3Effect = 0.8
marker4Effect = 0.7
marker5Effect = 0.6
recoveryTicks = 12000  # 10分钟恢复
```

### 中度难度（平衡）
```toml
[food.decrement]
enabled = true
marker1Effect = 1.0
marker2Effect = 0.8
marker3Effect = 0.6
marker4Effect = 0.4
marker5Effect = 0.2
recoveryTicks = 24000  # 20分钟恢复
```

### 高难度（挑战）
```toml
[food.decrement]
enabled = true
marker1Effect = 1.0
marker2Effect = 0.7
marker3Effect = 0.5
marker4Effect = 0.3
marker5Effect = 0.1
recoveryTicks = 48000  # 40分钟恢复
speedPenalty2 = 0.7
speedPenalty3 = 0.5
speedPenalty4 = 0.3
speedPenalty5 = 0.1
```

## 注意事项

1. 配置文件在首次启动游戏时自动生成
2. 修改配置后需要**完全重启游戏**才能生效（不能只是退出世界）
3. 如果配置文件损坏，删除后重启游戏会自动重新生成
4. 食物递减系统默认关闭，需要手动开启
5. 饭盒的进食速度会受到食物标记的影响（如果启用了食物递减系统）
6. 多人游戏中，服务器端的配置生效

## 完整配置文件示例

这是一个启用了食物递减系统的完整配置示例：

```toml
# Eat The World 配置文件

[core]
    # 总开关
    enabled = true

[debug]
    # 调试选项（一般不需要开启）
    enabled = false
    logFoodEvents = false
    logRegen = false

[hunger.actions]
    # 行为消耗系统（默认启用但消耗值为0）
    enabled = true
    sprintExhaustionPerSecond = 0.0
    jumpExhaustion = 0.0
    attackExhaustion = 0.0
    hurtExhaustionMultiplier = 0.0
    hurtExhaustionMaxPerHit = 10.0

[regen]
    # 自然回血系统（默认关闭）
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
    # 全局食物倍率（默认关闭）
    enabled = false
    nutritionMultiplier = 1.0
    saturationMultiplier = 1.0

[food.appleskin]
    # AppleSkin兼容
    compat.enabled = true

[food.override]
    # 按物品覆盖（默认关闭）
    enabled = false
    entries = []

[food.decrement]
    # 食物递减系统（核心功能，需要手动启用）
    enabled = true
    
    # 效果倍率（影响饥饿值和饱和度恢复）
    marker1Effect = 1.0   # 首次：100%
    marker2Effect = 0.8   # 第2次：80%
    marker3Effect = 0.6   # 第3次：60%
    marker4Effect = 0.4   # 第4次：40%
    marker5Effect = 0.2   # 第5次及以后：20%
    
    # 进食速度倍率（影响进食动画时长）
    speedPenalty1 = 1.0   # 首次：正常速度
    speedPenalty2 = 0.8   # 第2次：稍慢
    speedPenalty3 = 0.6   # 第3次：更慢
    speedPenalty4 = 0.4   # 第4次：很慢
    speedPenalty5 = 0.2   # 第5次及以后：最慢
    
    # 标记恢复机制
    recoveryEnabled = true
    recoveryTicks = 24000  # 24000 ticks = 20分钟 = 1游戏日
```

## 修改示例

### 示例1：只启用食物递减，保持默认难度
```toml
[food.decrement]
    enabled = true  # 只改这一行
```

### 示例2：启用食物递减 + 调整为简单难度
```toml
[food.decrement]
    enabled = true
    marker2Effect = 0.9  # 改为90%
    marker3Effect = 0.8  # 改为80%
    marker4Effect = 0.7  # 改为70%
    marker5Effect = 0.6  # 改为60%
    recoveryTicks = 12000  # 改为10分钟恢复
```

### 示例3：启用食物递减 + 启用自然回血系统
```toml
[food.decrement]
    enabled = true

[regen]
    enabled = true  # 启用自定义回血
    minFoodLevel = 18  # 饥饿值18以上才回血
```

---

**提示：** 如果你不确定要改什么，最简单的方法就是只把 `food.decrement.enabled` 改为 `true`，其他保持默认！
