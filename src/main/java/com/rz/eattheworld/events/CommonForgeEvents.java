package com.rz.eattheworld.events;

import com.mojang.logging.LogUtils;
import com.rz.eattheworld.EatTheWorldMod;
import com.rz.eattheworld.ModConfigs;
import com.rz.eattheworld.food.FoodOverrideData;
import com.rz.eattheworld.runtime.PlayerRuntimeState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.InteractionHand;

import com.rz.eattheworld.network.SyncFoodMarkersPacket;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EatTheWorldMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonForgeEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<UUID, PlayerRuntimeState> RUNTIME = new ConcurrentHashMap<>();

    // 将state方法设为public，以便外部访问
    public static PlayerRuntimeState state(ServerPlayer player) {
        return RUNTIME.computeIfAbsent(player.getUUID(), (id) -> {
            PlayerRuntimeState st = new PlayerRuntimeState();
            st.wasOnGround = player.onGround();
            return st;
        });
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        if (!ModConfigs.coreEnabled || !ModConfigs.regenEnabled || !ModConfigs.regenDisableVanillaNaturalRegen) {
            return;
        }

        MinecraftServer server = event.getServer();
        GameRules.BooleanValue rule = server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION);
        if (rule.get()) {
            rule.set(false, server);
            if (ModConfigs.debugEnabled && ModConfigs.debugLogRegen) {
                LOGGER.info("[EatTheWorld][debug] 已禁用原版自然回血 gamerule (naturalRegeneration=false)");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!ModConfigs.coreEnabled) {
            return;
        }

        PlayerRuntimeState st = state(player);
        st.serverTickCounter++;

        if (ModConfigs.hungerActionsEnabled) {
            double perSecond = ModConfigs.sprintExhaustionPerSecond;
            if (perSecond > 0.0 && player.isSprinting()) {
                float add = (float) (perSecond / 20.0);
                player.getFoodData().addExhaustion(add);

                if (ModConfigs.debugEnabled && st.serverTickCounter - st.lastDebugLogTick >= 100) {
                    LOGGER.info("[EatTheWorld][debug] sprint_exhaustion +{} (perSecond={}) player={}",
                            add, perSecond, player.getGameProfile().getName());
                }
            }
        }

        boolean onGround = player.onGround();
        st.wasOnGround = onGround;


        handleCustomRegen(player, st);
        handlePendingFoodAdjust(player, st);
        
        // 处理食物标记恢复
        if (ModConfigs.foodDecrementEnabled && ModConfigs.foodDecrementRecoveryEnabled) {
            long gameTime = player.level().getGameTime();
            // 每经过指定的游戏刻数（配置文件中设置），恢复食物标记
            if (ModConfigs.foodDecrementRecoveryTicks > 0) {
                st.foodTracker.restoreFoodMarkers(gameTime);
            }
        }
        
        // 每隔一定时间同步食物标记到客户端
        if (ModConfigs.foodDecrementEnabled && st.serverTickCounter % 20 == 0) { // 每秒同步一次
            long gameTime = player.level().getGameTime();
            SyncFoodMarkersPacket.sendToPlayer(
                player, 
                st.foodTracker.getFoodMarkersCopy(),
                st.foodTracker.getFoodActualCountCopy(),
                st.foodTracker.getFoodLastConsumedTimeCopy(),
                gameTime,
                st.foodTracker.getLastRecoveryTime()
            );
        }

        if (ModConfigs.debugEnabled && st.serverTickCounter - st.lastDebugLogTick >= 100) {
            st.lastDebugLogTick = st.serverTickCounter;
            LOGGER.info("[EatTheWorld][debug] tick player={} food={} sat={} onGround={}",
                    player.getGameProfile().getName(),
                    player.getFoodData().getFoodLevel(),
                    player.getFoodData().getSaturationLevel(),
                    onGround);
        }
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ModConfigs.coreEnabled || !ModConfigs.hungerActionsEnabled) {
            return;
        }

        double cost = ModConfigs.jumpExhaustion;
        if (cost <= 0.0) {
            return;
        }

        PlayerRuntimeState st = state(player);
        st.serverTickCounter++;

        float add = (float) cost;
        player.getFoodData().addExhaustion(add);

        if (ModConfigs.debugEnabled) {
            LOGGER.info("[EatTheWorld][debug] jump_exhaustion +{} player={}", add, player.getGameProfile().getName());
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ModConfigs.coreEnabled || !ModConfigs.hungerActionsEnabled) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        double cost = ModConfigs.attackExhaustion;
        if (cost <= 0.0) {
            return;
        }

        float add = (float) cost;
        player.getFoodData().addExhaustion(add);

        if (ModConfigs.debugEnabled) {
            LOGGER.info("[EatTheWorld][debug] attack_exhaustion +{} player={} target={}",
                    add,
                    player.getGameProfile().getName(),
                    event.getTarget() == null ? "null" : event.getTarget().getType().toString());
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ModConfigs.coreEnabled || !ModConfigs.hungerActionsEnabled) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        double mul = ModConfigs.hurtExhaustionMultiplier;
        if (mul <= 0.0) {
            return;
        }

        float damage = event.getAmount();
        double raw = damage * mul;
        double clamped = Math.min(raw, ModConfigs.hurtExhaustionMaxPerHit);
        if (clamped <= 0.0) {
            return;
        }

        float add = (float) clamped;
        player.getFoodData().addExhaustion(add);

        if (ModConfigs.debugEnabled) {
            LOGGER.info("[EatTheWorld][debug] hurt_exhaustion +{} (damage={}, mul={}) player={} source={}",
                    add,
                    damage,
                    mul,
                    player.getGameProfile().getName(),
                    event.getSource().getMsgId());
        }
    }

    @SubscribeEvent
    public static void onFoodStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ModConfigs.coreEnabled) {
            return;
        }

        ItemStack stack = event.getItem();
        
        // 检查是否是饭盒
        if (stack.getItem() instanceof com.rz.eattheworld.items.BentoBoxItem) {
            // 饭盒进食：获取饭盒内的食物并应用速度惩罚
            ItemStack nextFood = getNextFoodFromBentoBox(stack);
            if (!nextFood.isEmpty() && ModConfigs.foodDecrementEnabled) {
                PlayerRuntimeState st = state(player);
                double speedMultiplier = st.foodTracker.getSpeedMultiplier(nextFood);
                if (speedMultiplier < 1.0) {
                    int originalUseDuration = event.getDuration();
                    int newUseDuration = (int) Math.round(originalUseDuration / speedMultiplier);
                    event.setDuration(newUseDuration);
                    
                    if (ModConfigs.debugEnabled && ModConfigs.debugLogFoodEvents) {
                        int marker = st.foodTracker.getFoodMarker(nextFood);
                        LOGGER.info("[EatTheWorld][debug] bento_box_speed_penalty applied: player={} food={} marker={} speedMultiplier={} duration: {}->{}", 
                                player.getGameProfile().getName(),
                                String.valueOf(BuiltInRegistries.ITEM.getKey(nextFood.getItem())),
                                marker,
                                speedMultiplier,
                                originalUseDuration,
                                newUseDuration);
                    }
                }
            }
            
            // 记录进食开始状态
            PlayerRuntimeState st = state(player);
            st.eatStartFoodLevel = player.getFoodData().getFoodLevel();
            st.eatStartSaturation = player.getFoodData().getSaturationLevel();
            st.hasEatSnapshot = true;
            return;
        }
        
        // 普通食物进食
        FoodProperties food = stack.getFoodProperties(player);
        if (food == null) {
            return;
        }

        PlayerRuntimeState st = state(player);
        
        // 处理进食速度惩罚
        if (ModConfigs.foodDecrementEnabled) {
            double speedMultiplier = st.foodTracker.getSpeedMultiplier(stack);
            if (speedMultiplier < 1.0) {
                // 通过增加使用时间来模拟速度减慢（使用时间越长，吃起来越慢）
                int originalUseDuration = event.getDuration();
                int newUseDuration = (int) Math.round(originalUseDuration / speedMultiplier);
                event.setDuration(newUseDuration);
                
                if (ModConfigs.debugEnabled && ModConfigs.debugLogFoodEvents) {
                    int marker = st.foodTracker.getFoodMarker(stack);
                    LOGGER.info("[EatTheWorld][debug] food_speed_penalty applied: player={} item={} marker={} speedMultiplier={} duration: {}->{}", 
                            player.getGameProfile().getName(),
                            String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())),
                            marker,
                            speedMultiplier,
                            originalUseDuration,
                            newUseDuration);
                }
            }
        } else {
            // 即使未启用食物递减，也要确保进食动画正常工作
            if (ModConfigs.debugEnabled && ModConfigs.debugLogFoodEvents) {
                LOGGER.info("[EatTheWorld][debug] food_start: player={} item={} duration={}",
                        player.getGameProfile().getName(),
                        String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())),
                        event.getDuration());
            }
        }
        
        st.eatStartFoodLevel = player.getFoodData().getFoodLevel();
        st.eatStartSaturation = player.getFoodData().getSaturationLevel();
        st.hasEatSnapshot = true;
    }
    
    /**
     * 从饭盒中获取下一个要进食的食物（不消耗）
     */
    private static ItemStack getNextFoodFromBentoBox(ItemStack bentoBoxStack) {
        LazyOptional<IItemHandler> handlerOpt = bentoBoxStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (!handlerOpt.isPresent()) {
            return ItemStack.EMPTY;
        }
        
        IItemHandler itemHandler = handlerOpt.resolve().get();
        com.rz.eattheworld.items.BentoBoxAutoFeeder.FeedMode mode = 
            com.rz.eattheworld.items.BentoBoxAutoFeeder.getFeedMode(bentoBoxStack);
        
        if (mode == com.rz.eattheworld.items.BentoBoxAutoFeeder.FeedMode.SEQUENTIAL) {
            // 顺序模式：从当前索引开始查找
            int startIndex = com.rz.eattheworld.items.BentoBoxAutoFeeder.getSequentialSlotIndex(bentoBoxStack);
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                int checkSlot = (startIndex + i) % itemHandler.getSlots();
                ItemStack stack = itemHandler.getStackInSlot(checkSlot);
                if (!stack.isEmpty() && stack.getItem().getFoodProperties(stack, null) != null) {
                    return stack;
                }
            }
        } else {
            // 智能模式：返回第一个找到的食物（简化版）
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem().getFoodProperties(stack, null) != null) {
                    return stack;
                }
            }
        }
        
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onFoodFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ModConfigs.coreEnabled) {
            return;
        }

        ItemStack stack = event.getItem();
        
        // 检查是否是饭盒
        if (stack.getItem() instanceof com.rz.eattheworld.items.BentoBoxItem) {
            // 饭盒进食完成：不处理，因为autoFeed已经处理了所有逻辑
            System.out.println("[EatTheWorld] Bento box eating finished - skipping event handler");
            return;
        }
        
        PlayerRuntimeState st = state(player);
        
        // 检查是否是饭盒触发的事件（避免重复处理）
        if (st.isBentoBoxTriggeredEvent) {
            System.out.println("[EatTheWorld] Bento box triggered event detected - skipping our logic, letting SolCarrot handle it");
            return;
        }
        
        // 普通食物进食
        FoodProperties food = stack.getFoodProperties(player);
        if (food == null) {
            return;
        }
        
        // 处理食物递减机制
        if (ModConfigs.foodDecrementEnabled) {
            // 先保存当前标记（用于计算效果）
            st.pendingFoodMarkerBeforeEat = st.foodTracker.getFoodMarker(stack);
            
            // 然后更新食物标记（为下次进食做准备）
            long gameTime = player.level().getGameTime();
            st.foodTracker.updateFoodMarker(stack, gameTime);
            
            if (ModConfigs.debugEnabled && ModConfigs.debugLogFoodEvents) {
                int markerAfter = st.foodTracker.getFoodMarker(stack);
                LOGGER.info("[EatTheWorld][debug] food_decrement: player={} item={} markerBefore={} markerAfter={}",
                        player.getGameProfile().getName(),
                        st.pendingFoodItemId,
                        st.pendingFoodMarkerBeforeEat,
                        markerAfter);
            }
        }
        
        // 如果是从饭盒进食，恢复饭盒并更新索引
        if (st.isEatingFromBentoBox) {
            handleBentoBoxFinish(player, st);
        }
        
        st.pendingFoodAdjust = true;
        st.pendingFoodItemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        st.pendingFoodItem = stack.copy();

        // 通知SolCarrot玩家吃了食物（如果SolCarrot已加载且配置启用）
        // 这里是正确的调用时机，因为食物已经被实际消费
        System.out.println("[EatTheWorld] Normal food eating finished: " + stack.getItem());
        if (ModConfigs.foodSolCarrotCompatEnabled) {
            if (!com.rz.eattheworld.compat.SolCarrotCompat.isLoaded()) {
                com.rz.eattheworld.compat.SolCarrotCompat.init();
            }
            com.rz.eattheworld.compat.SolCarrotCompat.notifyFoodEaten(player, stack);
        }

        if (ModConfigs.debugEnabled && ModConfigs.debugLogFoodEvents) {
            LOGGER.info("[EatTheWorld][debug] food_finish player={} item={} nutrition={} satMod={}",
                    player.getGameProfile().getName(),
                    st.pendingFoodItemId,
                    food.getNutrition(),
                    food.getSaturationModifier());
        }
    }
    
    /**
     * 处理从饭盒进食完成后的逻辑
     */
    private static void handleBentoBoxFinish(ServerPlayer player, PlayerRuntimeState st) {
        // 恢复饭盒到主手
        player.setItemInHand(InteractionHand.MAIN_HAND, st.bentoBoxStack);
        
        // 如果是顺序模式，更新索引
        if (st.bentoBoxMode == com.rz.eattheworld.items.BentoBoxAutoFeeder.FeedMode.SEQUENTIAL) {
            LazyOptional<IItemHandler> handlerOpt = st.bentoBoxStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if (handlerOpt.isPresent()) {
                IItemHandler itemHandler = handlerOpt.resolve().get();
                int nextIndex = (st.bentoBoxSlot + 1) % itemHandler.getSlots();
                com.rz.eattheworld.items.BentoBoxAutoFeeder.setSequentialSlotIndex(st.bentoBoxStack, nextIndex);
            }
        }
        
        // 清除饭盒状态
        st.isEatingFromBentoBox = false;
        st.bentoBoxStack = ItemStack.EMPTY;
        st.bentoBoxMode = null;
        st.bentoBoxSlot = -1;
    }




    private static void handlePendingFoodAdjust(ServerPlayer player, PlayerRuntimeState st) {
        if (!st.pendingFoodAdjust) {
            return;
        }
        st.pendingFoodAdjust = false;

        if (!ModConfigs.coreEnabled) {
            return;
        }
        
        // 如果没有启用任何调整功能（全局倍率、覆盖、递减），则直接返回
        if (!ModConfigs.foodGlobalEnabled && !ModConfigs.foodOverrideEnabled && !ModConfigs.foodDecrementEnabled) {
            return;
        }

        if (!st.hasEatSnapshot) {
            return;
        }
        st.hasEatSnapshot = false;

        // 获取食物项并检查是否有物品覆盖
        // 使用在onFoodFinish事件中存储的食物项，以确保正确的食物标记应用
        ItemStack foodItem = st.pendingFoodItem;
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(foodItem.getItem());
        
        // 确保食物项是有效的
        if (foodItem.isEmpty() || foodItem.getFoodProperties(player) == null) {
            return;
        }
        
        boolean hasOverride = false;
        int targetFoodGain = 0;
        float targetSatGain = 0.0f;
        
        if (ModConfigs.foodOverrideEnabled) {
            // 检查是否对此物品有覆盖
            FoodOverrideData.Parser.parseEntries(ModConfigs.foodOverrideEntries); // 确保覆盖数据已解析
            var overrideOpt = FoodOverrideData.Parser.getOverride(itemKey);
            
            if (overrideOpt.isPresent()) {
                FoodOverrideData override = overrideOpt.get();
                if (override.enabled) {
                    hasOverride = true;
                    targetFoodGain = override.nutrition;
                    targetSatGain = override.saturationModifier * override.nutrition * 2.0f; // 计算饱和增量
                }
            }
        }
        
        if (!hasOverride && ModConfigs.foodGlobalEnabled) {
            // 使用全局倍率 - 从原始食物属性计算，而不是从差异计算
            FoodProperties originalFood = foodItem.getFoodProperties(player);
            if (originalFood != null) {
                int originalNutrition = originalFood.getNutrition();
                float originalSaturationMod = originalFood.getSaturationModifier();
                
                double nutritionMul = ModConfigs.foodGlobalNutritionMultiplier;
                double saturationMul = ModConfigs.foodGlobalSaturationMultiplier;
                
                targetFoodGain = (int) Math.round(originalNutrition * nutritionMul);
                
                // 计算饱和度增益：原版公式为 saturationGain = nutrition * saturationMod * 2.0f
                float originalSaturationGain = originalNutrition * originalSaturationMod * 2.0f;
                float newSaturationGain = (float) (originalSaturationGain * saturationMul);
                targetSatGain = newSaturationGain;
            }
        }
        
        // 如果没有启用全局倍率或覆盖，使用原始食物属性
        if (!hasOverride && !ModConfigs.foodGlobalEnabled) {
            FoodProperties originalFood = foodItem.getFoodProperties(player);
            if (originalFood != null) {
                targetFoodGain = originalFood.getNutrition();
                // 原版饱和度增益计算：saturationGain = nutrition * saturationModifier * 2.0f
                targetSatGain = originalFood.getNutrition() * originalFood.getSaturationModifier() * 2.0f;
            }
        }
        
        if (targetFoodGain == 0 && targetSatGain == 0.0f) {
            return;
        }

        // 首先恢复到吃食物前的状态
        int originalFoodLevel = st.eatStartFoodLevel;
        float originalSaturation = st.eatStartSaturation;
        
        // 🔥 如果是饭盒触发的，不要恢复饥饿值
        // 因为Diet已经通过FoodData.eat()处理过了，我们基于Diet的结果调整
        if (!st.isBentoBoxTriggeredEvent) {
            player.getFoodData().setFoodLevel(originalFoodLevel);
            player.getFoodData().setSaturation(originalSaturation);
        } else {
            // 饭盒模式：Diet已经通过Mixin处理了FoodData.eat()
            // 我们不需要恢复，只需要应用我们的食物标记递减效果
            // 清除标记
            st.isBentoBoxTriggeredEvent = false;
        }
        
        // 应用食物递减机制
        if (ModConfigs.foodDecrementEnabled && foodItem.getFoodProperties(player) != null) {
            // 使用进食前保存的标记值来计算效果
            double effectMultiplier = getEffectMultiplierForMarker(st.pendingFoodMarkerBeforeEat);
            targetFoodGain = (int) Math.round(targetFoodGain * effectMultiplier);
            targetSatGain = (float) (targetSatGain * effectMultiplier);
            
            if (ModConfigs.debugEnabled && ModConfigs.debugLogFoodEvents) {
                LOGGER.info("[EatTheWorld][debug] food_decrement applied: player={} item={} markerBeforeEat={} effectMultiplier={} adjustedFoodGain={} adjustedSatGain={}",
                        player.getGameProfile().getName(),
                        st.pendingFoodItemId,
                        st.pendingFoodMarkerBeforeEat,
                        effectMultiplier,
                        targetFoodGain,
                        targetSatGain);
            }
        }
        
        // 然后应用调整后的食物效果
        int newFoodLevel = clampInt(originalFoodLevel + targetFoodGain, 0, 20);
        float newSaturation = originalSaturation + targetSatGain;
        
        // 饱和度不能超过饥饿值
        newSaturation = Math.min(newSaturation, (float) newFoodLevel);
        // 确保饱和度不低于0
        newSaturation = Math.max(newSaturation, 0.0f);

        player.getFoodData().setFoodLevel(newFoodLevel);
        player.getFoodData().setSaturation(newSaturation);

        if (ModConfigs.debugEnabled && ModConfigs.debugLogFoodEvents) {
            if (hasOverride) {
                LOGGER.info("[EatTheWorld][debug] food_override player={} item={} -> food={} sat={} (nutrition={}, saturation={})",
                        player.getGameProfile().getName(),
                        st.pendingFoodItemId,
                        newFoodLevel,
                        newSaturation,
                        targetFoodGain,
                        targetSatGain);
            } else {
                LOGGER.info("[EatTheWorld][debug] food_global_mul player={} item={} -> food={} sat={} (nutrition={}, saturation={})",
                        player.getGameProfile().getName(),
                        st.pendingFoodItemId,
                        newFoodLevel,
                        newSaturation,
                        targetFoodGain,
                        targetSatGain);
            }
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }




    private static void handleCustomRegen(ServerPlayer player, PlayerRuntimeState st) {
        if (!ModConfigs.coreEnabled || !ModConfigs.regenEnabled) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        if (ModConfigs.regenRequireNaturalRegenGamerule) {
            if (!player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
                return;
            }
        }

        int interval = ModConfigs.regenIntervalTicks;
        if (interval <= 0) {
            interval = 1;
        }

        st.regenTimerTicks++;
        if (st.regenTimerTicks < interval) {
            return;
        }
        st.regenTimerTicks = 0;

        if (player.getHealth() >= player.getMaxHealth()) {
            return;
        }

        if (!regenConditionsMet(player)) {
            return;
        }

        float heal = (float) ModConfigs.regenHealAmount;
        if (heal <= 0.0f) {
            return;
        }

        float before = player.getHealth();
        player.heal(heal);
        float after = player.getHealth();

        double cost = ModConfigs.regenExhaustionCost;
        if (cost > 0.0) {
            player.getFoodData().addExhaustion((float) cost);
        }

        if (ModConfigs.debugEnabled && ModConfigs.debugLogRegen) {
            LOGGER.info("[EatTheWorld][debug] custom_regen heal={} ({}->{}), exhaustion+{} player={} food={} sat={} intervalTicks={}",
                    heal,
                    before,
                    after,
                    cost,
                    player.getGameProfile().getName(),
                    player.getFoodData().getFoodLevel(),
                    player.getFoodData().getSaturationLevel(),
                    interval);
        }
    }

    private static boolean regenConditionsMet(ServerPlayer player) {
        if (player.getFoodData().getFoodLevel() < ModConfigs.regenMinFoodLevel) {
            return false;
        }

        if (ModConfigs.regenRequireSaturation) {
            return player.getFoodData().getSaturationLevel() >= (float) ModConfigs.regenMinSaturation;
        }

        return true;
    }
    
    /**
     * 根据标记值获取效果倍率
     */
    private static double getEffectMultiplierForMarker(int marker) {
        switch (marker) {
            case 1: return 1.0; // 第一次吃 - 100%效果
            case 2: return ModConfigs.foodDecrementMarker2Effect; // 第二次吃
            case 3: return ModConfigs.foodDecrementMarker3Effect; // 第三次吃
            case 4: return ModConfigs.foodDecrementMarker4Effect; // 第四次吃
            case 5: default: return ModConfigs.foodDecrementMarker5Effect; // 第五次及以上
        }
    }


    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRuntimeState st = state(player);
            
            // 从玩家NBT数据中加载食物标记数据
            net.minecraft.nbt.CompoundTag playerData = player.getPersistentData();
            if (playerData.contains("EatTheWorld_FoodTracker")) {
                net.minecraft.nbt.CompoundTag trackerData = playerData.getCompound("EatTheWorld_FoodTracker");
                st.foodTracker.deserializeNBT(trackerData);
                
                if (ModConfigs.debugEnabled) {
                    LOGGER.info("[EatTheWorld][debug] Loaded food tracker data for player: {}", player.getGameProfile().getName());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRuntimeState st = RUNTIME.get(player.getUUID());
            
            if (st != null) {
                // 保存食物标记数据到玩家NBT
                net.minecraft.nbt.CompoundTag playerData = player.getPersistentData();
                net.minecraft.nbt.CompoundTag trackerData = st.foodTracker.serializeNBT();
                playerData.put("EatTheWorld_FoodTracker", trackerData);
                
                if (ModConfigs.debugEnabled) {
                    LOGGER.info("[EatTheWorld][debug] Saved food tracker data for player: {}", player.getGameProfile().getName());
                }
            }
            
            RUNTIME.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer && event.getOriginal() instanceof ServerPlayer oldPlayer) {
            // 在玩家死亡重生或从末地返回时，保留食物标记数据
            PlayerRuntimeState oldState = RUNTIME.get(oldPlayer.getUUID());
            
            if (oldState != null) {
                // 序列化旧玩家的食物标记数据
                net.minecraft.nbt.CompoundTag trackerData = oldState.foodTracker.serializeNBT();
                
                // 保存到新玩家的NBT
                net.minecraft.nbt.CompoundTag newPlayerData = newPlayer.getPersistentData();
                newPlayerData.put("EatTheWorld_FoodTracker", trackerData);
                
                if (ModConfigs.debugEnabled) {
                    LOGGER.info("[EatTheWorld][debug] Cloned food tracker data for player: {}", newPlayer.getGameProfile().getName());
                }
            }
            
            // 移除旧玩家的运行时数据
            RUNTIME.remove(oldPlayer.getUUID());
        }
    }

    private CommonForgeEvents() {
    }
}