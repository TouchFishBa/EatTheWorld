package com.rz.eattheworld;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = EatTheWorldMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfigs {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue CORE_ENABLED = BUILDER
            .comment("总开关：控制 EatTheWorld 的所有玩法逻辑是否启用。")
            .define("core.enabled", true);

    private static final ForgeConfigSpec.BooleanValue DEBUG_ENABLED = BUILDER
            .comment("调试开关：开启后会输出更多日志。")
            .define("debug.enabled", false);

    private static final ForgeConfigSpec.BooleanValue DEBUG_LOG_FOOD_EVENTS = BUILDER
            .comment("调试：记录进食相关事件（仅服务端）。")
            .define("debug.logFoodEvents", false);

    private static final ForgeConfigSpec.BooleanValue DEBUG_LOG_REGEN = BUILDER
            .comment("调试：记录回血相关逻辑（仅服务端）。")
            .define("debug.logRegen", false);

    private static final ForgeConfigSpec.BooleanValue HUNGER_ACTIONS_ENABLED = BUILDER
            .comment("启用“行为消耗”系统（疾跑/跳跃/战斗/受击等会额外增加消耗）。")
            .define("hunger.actions.enabled", true);

    private static final ForgeConfigSpec.DoubleValue HUNGER_SPRINT_EXHAUSTION_PER_SECOND = BUILDER
            .comment("疾跑额外消耗：疾跑时每秒追加的 exhaustion。0 = 不追加。")
            .defineInRange("hunger.actions.sprintExhaustionPerSecond", 1.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.DoubleValue HUNGER_JUMP_EXHAUSTION = BUILDER
            .comment("跳跃额外消耗：每次跳跃追加的 exhaustion。0 = 不追加。")
            .defineInRange("hunger.actions.jumpExhaustion", 1.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.DoubleValue HUNGER_ATTACK_EXHAUSTION = BUILDER
            .comment("攻击额外消耗：每次攻击实体时追加的 exhaustion。0 = 不追加。")
            .defineInRange("hunger.actions.attackExhaustion", 1.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.DoubleValue HUNGER_HURT_EXHAUSTION_MULTIPLIER = BUILDER
            .comment("受击额外消耗：玩家受伤时追加 exhaustion = 伤害值 * 倍率。0 = 不追加。")
            .defineInRange("hunger.actions.hurtExhaustionMultiplier", 0.5, 0.0, 1000.0);

    private static final ForgeConfigSpec.DoubleValue HUNGER_HURT_EXHAUSTION_MAX_PER_HIT = BUILDER
            .comment("受击额外消耗上限：单次受击最多追加多少 exhaustion（防止高伤害造成消耗暴涨）。")
            .defineInRange("hunger.actions.hurtExhaustionMaxPerHit", 5.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.BooleanValue REGEN_ENABLED = BUILDER
            .comment("自然回血系统：启用后将接管自然回血规则（Step 6 会禁用原版自然回血以避免叠加）。")
            .define("regen.enabled", true);

    private static final ForgeConfigSpec.BooleanValue REGEN_DISABLE_VANILLA = BUILDER
            .comment("是否禁用原版 gamerule 自然回血（RULE_NATURAL_REGENERATION）。启用后会在服务器启动时将其设为 false。")
            .define("regen.disableVanillaNaturalRegen", true);

    private static final ForgeConfigSpec.BooleanValue REGEN_REQUIRE_GAMERULE = BUILDER
            .comment("是否要求 gamerule naturalRegeneration=true 才允许本模组回血逻辑运行（注意：如果同时禁用原版自然回血，该条件通常应关闭）。")
            .define("regen.requireNaturalRegenGamerule", false);

    private static final ForgeConfigSpec.IntValue REGEN_MIN_FOOD_LEVEL = BUILDER
            .comment("自然回血条件：玩家饥饿值达到该阈值（0~20）才允许回血。")
            .defineInRange("regen.minFoodLevel", 18, 0, 20);

    private static final ForgeConfigSpec.BooleanValue REGEN_REQUIRE_SATURATION = BUILDER
            .comment("自然回血条件：是否要求饱和度达到阈值才允许回血。")
            .define("regen.requireSaturation", true);

    private static final ForgeConfigSpec.DoubleValue REGEN_MIN_SATURATION = BUILDER
            .comment("自然回血条件：最低饱和度阈值。")
            .defineInRange("regen.minSaturation", 0.0, 0.0, 20.0);

    private static final ForgeConfigSpec.IntValue REGEN_INTERVAL_TICKS = BUILDER
            .comment("自定义回血：间隔 tick（20 tick = 1 秒）。")
            .defineInRange("regen.intervalTicks", 40, 1, 72000);

    private static final ForgeConfigSpec.DoubleValue REGEN_HEAL_AMOUNT = BUILDER
            .comment("自定义回血：每次回复的生命值。1.0=半颗心，2.0=一颗心。")
            .defineInRange("regen.healAmount", 1.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.DoubleValue REGEN_EXHAUSTION_COST = BUILDER
            .comment("自定义回血：每次回血追加的 exhaustion。原版约为 6.0。")
            .defineInRange("regen.exhaustionCost", 10.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.BooleanValue FOOD_GLOBAL_ENABLED = BUILDER
            .comment("全局食物收益倍率：启用后会统一缩放所有食物的饥饿值与饱和度收益（Step 8）。")
            .define("food.global.enabled", false);

    private static final ForgeConfigSpec.DoubleValue FOOD_GLOBAL_NUTRITION_MULTIPLIER = BUILDER
            .comment("全局食物收益倍率：饥饿值（nutrition）倍率。1.0=原版。")
            .defineInRange("food.global.nutritionMultiplier", 1.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_GLOBAL_SATURATION_MULTIPLIER = BUILDER
            .comment("全局食物收益倍率：饱和度（saturation）倍率。1.0=原版。")
            .defineInRange("food.global.saturationMultiplier", 1.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.BooleanValue FOOD_APPLE_SKIN_COMPAT_ENABLED = BUILDER
            .comment("AppleSkin兼容：启用后将确保AppleSkin显示的食物数值与本模组的倍率调整保持同步。")
            .define("food.appleskin.compat.enabled", true);
    
    private static final ForgeConfigSpec.BooleanValue FOOD_CONTAINER_RETURN_ENABLED = BUILDER
            .comment("食物容器返回：启用后从饭盒进食会返回食物的容器物品（如蘑菇煲返回碗、牛奶桶返回桶等）。")
            .define("food.containerReturn.enabled", false);
    
    private static final ForgeConfigSpec.BooleanValue FOOD_DIET_COMPAT_ENABLED = BUILDER
            .comment("Diet兼容：启用后将确保从饭盒进食时Diet的营养系统正常工作。")
            .define("food.diet.compat.enabled", true);
    
    private static final ForgeConfigSpec.BooleanValue FOOD_SOLCARROT_COMPAT_ENABLED = BUILDER
            .comment("SolCarrot兼容：启用后将确保从饭盒进食时SolCarrot的食物多样性系统正常工作。")
            .define("food.solcarrot.compat.enabled", true);

    private static final ForgeConfigSpec.BooleanValue FOOD_OVERRIDE_ENABLED = BUILDER
            .comment("按物品覆盖：启用后允许为特定物品设置独立的饥饿值和饱和度收益（Step 9）。")
            .define("food.override.enabled", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> FOOD_OVERRIDE_ENTRIES = BUILDER
            .comment("按物品覆盖：格式为 'modid:item_name,nutrition,saturation_modifier,exhaustion(optional)'，例如：'minecraft:apple,8,0.6' 或 'minecraft:apple,8,0.6,0.5'")
            .defineList("food.override.entries", Collections.emptyList(), o -> o instanceof String);

    // 食物递减机制相关配置
    private static final ForgeConfigSpec.BooleanValue FOOD_DECREMENT_ENABLED = BUILDER
            .comment("食物效果递减：启用后连续食用同种食物时效果会递减（Step 10）。")
            .define("food.decrement.enabled", true);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_MARKER_1_EFFECT = BUILDER
            .comment("食物标记1效果倍率：首次食用同种食物的效果倍率。")
            .defineInRange("food.decrement.marker1Effect", 1.0, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_MARKER_2_EFFECT = BUILDER
            .comment("食物标记2效果倍率：第二次食用同种食物的效果倍率。")
            .defineInRange("food.decrement.marker2Effect", 0.8, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_MARKER_3_EFFECT = BUILDER
            .comment("食物标记3效果倍率：第三次食用同种食物的效果倍率。")
            .defineInRange("food.decrement.marker3Effect", 0.6, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_MARKER_4_EFFECT = BUILDER
            .comment("食物标记4效果倍率：第四次食用同种食物的效果倍率。")
            .defineInRange("food.decrement.marker4Effect", 0.4, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_MARKER_5_EFFECT = BUILDER
            .comment("食物标记5效果倍率：第五次及以后食用同种食物的效果倍率（最低限制）。")
            .defineInRange("food.decrement.marker5Effect", 0.2, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_SPEED_PENALTY_1 = BUILDER
            .comment("食物标记1进食速度倍率：首次食用同种食物的进食速度倍率。")
            .defineInRange("food.decrement.speedPenalty1", 1.0, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_SPEED_PENALTY_2 = BUILDER
            .comment("食物标记2进食速度倍率：第二次食用同种食物的进食速度倍率。")
            .defineInRange("food.decrement.speedPenalty2", 0.8, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_SPEED_PENALTY_3 = BUILDER
            .comment("食物标记3进食速度倍率：第三次食用同种食物的进食速度倍率。")
            .defineInRange("food.decrement.speedPenalty3", 0.6, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_SPEED_PENALTY_4 = BUILDER
            .comment("食物标记4进食速度倍率：第四次食用同种食物的进食速度倍率。")
            .defineInRange("food.decrement.speedPenalty4", 0.4, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue FOOD_DECREMENT_SPEED_PENALTY_5 = BUILDER
            .comment("食物标记5进食速度倍率：第五次及以后食用同种食物的进食速度倍率（最低限制）。")
            .defineInRange("food.decrement.speedPenalty5", 0.2, 0.0, 1.0);

    private static final ForgeConfigSpec.IntValue FOOD_DECREMENT_RECOVERY_TICKS = BUILDER
            .comment("食物标记恢复时间：经过多少游戏刻（ticks）后所有食物标记值-1。",
                     "参考：20 ticks = 1秒，1200 ticks = 1分钟，24000 ticks = 1游戏日（20分钟）")
            .defineInRange("food.decrement.recoveryTicks", 12000, 20, 2400000);

    private static final ForgeConfigSpec.BooleanValue FOOD_DECREMENT_RECOVERY_ENABLED = BUILDER
            .comment("启用食物标记恢复：是否启用食物标记的自动恢复机制。")
            .define("food.decrement.recoveryEnabled", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();


    public static boolean coreEnabled;
    public static boolean debugEnabled;
    public static boolean debugLogFoodEvents;
    public static boolean debugLogRegen;

    public static boolean hungerActionsEnabled;
    public static double sprintExhaustionPerSecond;
    public static double jumpExhaustion;
    public static double attackExhaustion;
    public static double hurtExhaustionMultiplier;
    public static double hurtExhaustionMaxPerHit;

    public static boolean regenEnabled;
    public static boolean regenDisableVanillaNaturalRegen;
    public static boolean regenRequireNaturalRegenGamerule;
    public static int regenMinFoodLevel;
    public static boolean regenRequireSaturation;
    public static double regenMinSaturation;
    public static int regenIntervalTicks;
    public static double regenHealAmount;
    public static double regenExhaustionCost;

    public static boolean foodGlobalEnabled;
    public static double foodGlobalNutritionMultiplier;
    public static double foodGlobalSaturationMultiplier;
    public static boolean foodAppleSkinCompatEnabled;
    public static boolean foodContainerReturnEnabled;
    public static boolean foodDietCompatEnabled;
    public static boolean foodSolCarrotCompatEnabled;
    public static boolean foodOverrideEnabled;
    public static List<? extends String> foodOverrideEntries;
    
    // 食物递减机制相关变量
    public static boolean foodDecrementEnabled;
    public static double foodDecrementMarker1Effect;
    public static double foodDecrementMarker2Effect;
    public static double foodDecrementMarker3Effect;
    public static double foodDecrementMarker4Effect;
    public static double foodDecrementMarker5Effect;
    public static double foodDecrementSpeedPenalty1;
    public static double foodDecrementSpeedPenalty2;
    public static double foodDecrementSpeedPenalty3;
    public static double foodDecrementSpeedPenalty4;
    public static double foodDecrementSpeedPenalty5;
    public static int foodDecrementRecoveryTicks;
    public static boolean foodDecrementRecoveryEnabled;


    @SubscribeEvent
    static void onConfigLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        coreEnabled = CORE_ENABLED.get();
        debugEnabled = DEBUG_ENABLED.get();
        debugLogFoodEvents = DEBUG_LOG_FOOD_EVENTS.get();
        debugLogRegen = DEBUG_LOG_REGEN.get();

        hungerActionsEnabled = HUNGER_ACTIONS_ENABLED.get();
        sprintExhaustionPerSecond = HUNGER_SPRINT_EXHAUSTION_PER_SECOND.get();
        jumpExhaustion = HUNGER_JUMP_EXHAUSTION.get();
        attackExhaustion = HUNGER_ATTACK_EXHAUSTION.get();
        hurtExhaustionMultiplier = HUNGER_HURT_EXHAUSTION_MULTIPLIER.get();
        hurtExhaustionMaxPerHit = HUNGER_HURT_EXHAUSTION_MAX_PER_HIT.get();

        regenEnabled = REGEN_ENABLED.get();
        regenDisableVanillaNaturalRegen = REGEN_DISABLE_VANILLA.get();
        regenRequireNaturalRegenGamerule = REGEN_REQUIRE_GAMERULE.get();
        regenMinFoodLevel = REGEN_MIN_FOOD_LEVEL.get();
        regenRequireSaturation = REGEN_REQUIRE_SATURATION.get();
        regenMinSaturation = REGEN_MIN_SATURATION.get();
        regenIntervalTicks = REGEN_INTERVAL_TICKS.get();
        regenHealAmount = REGEN_HEAL_AMOUNT.get();
        regenExhaustionCost = REGEN_EXHAUSTION_COST.get();

        foodGlobalEnabled = FOOD_GLOBAL_ENABLED.get();
        foodGlobalNutritionMultiplier = FOOD_GLOBAL_NUTRITION_MULTIPLIER.get();
        foodGlobalSaturationMultiplier = FOOD_GLOBAL_SATURATION_MULTIPLIER.get();
        foodAppleSkinCompatEnabled = FOOD_APPLE_SKIN_COMPAT_ENABLED.get();
        foodContainerReturnEnabled = FOOD_CONTAINER_RETURN_ENABLED.get();
        foodDietCompatEnabled = FOOD_DIET_COMPAT_ENABLED.get();
        foodSolCarrotCompatEnabled = FOOD_SOLCARROT_COMPAT_ENABLED.get();
        foodOverrideEnabled = FOOD_OVERRIDE_ENABLED.get();
        foodOverrideEntries = FOOD_OVERRIDE_ENTRIES.get();
        
        // 食物递减机制配置
        foodDecrementEnabled = FOOD_DECREMENT_ENABLED.get();
        foodDecrementMarker1Effect = FOOD_DECREMENT_MARKER_1_EFFECT.get();
        foodDecrementMarker2Effect = FOOD_DECREMENT_MARKER_2_EFFECT.get();
        foodDecrementMarker3Effect = FOOD_DECREMENT_MARKER_3_EFFECT.get();
        foodDecrementMarker4Effect = FOOD_DECREMENT_MARKER_4_EFFECT.get();
        foodDecrementMarker5Effect = FOOD_DECREMENT_MARKER_5_EFFECT.get();
        foodDecrementSpeedPenalty1 = FOOD_DECREMENT_SPEED_PENALTY_1.get();
        foodDecrementSpeedPenalty2 = FOOD_DECREMENT_SPEED_PENALTY_2.get();
        foodDecrementSpeedPenalty3 = FOOD_DECREMENT_SPEED_PENALTY_3.get();
        foodDecrementSpeedPenalty4 = FOOD_DECREMENT_SPEED_PENALTY_4.get();
        foodDecrementSpeedPenalty5 = FOOD_DECREMENT_SPEED_PENALTY_5.get();
        foodDecrementRecoveryTicks = FOOD_DECREMENT_RECOVERY_TICKS.get();
        foodDecrementRecoveryEnabled = FOOD_DECREMENT_RECOVERY_ENABLED.get();

    }
}

