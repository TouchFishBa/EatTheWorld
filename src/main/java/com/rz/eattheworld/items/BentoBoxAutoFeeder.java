package com.rz.eattheworld.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import com.rz.eattheworld.runtime.PlayerRuntimeState;
import com.rz.eattheworld.runtime.FoodConsumptionTracker;
import com.rz.eattheworld.events.CommonForgeEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraft.world.entity.LivingEntity;
import com.rz.eattheworld.ModConfigs;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.UseAnim;

public class BentoBoxAutoFeeder {
    
    public enum FeedMode {
        SEQUENTIAL,     // 顺序模式：按槽位顺序轮流进食
        SMART          // 智能模式：优先标记1的食物，标记相同则选饥饿度最高的
    }
    
    /**
     * 自动进食主方法 - 从饭盒中消耗食物并应用效果
     */
    public static boolean autoFeed(ServerPlayer player, ItemStack bentoBoxStack) {
        // 从NBT获取当前模式
        FeedMode mode = getFeedMode(bentoBoxStack);
        
        // 获取饭盒的物品槽
        LazyOptional<IItemHandler> handlerOpt = bentoBoxStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (!handlerOpt.isPresent()) {
            return false;
        }
        
        IItemHandler itemHandler = handlerOpt.resolve().get();
        
        // 根据模式选择食物
        int slotToUse = findFoodToUse(player, itemHandler, bentoBoxStack, mode);
        if (slotToUse == -1) {
            return false; // 没有合适的食物
        }
        
        ItemStack foodStack = itemHandler.getStackInSlot(slotToUse).copy();
        if (foodStack.isEmpty() || !isFood(foodStack)) {
            return false;
        }
        
        // 检查玩家是否能进食
        if (!player.canEat(false)) {
            return false;
        }
        
        // 获取食物属性
        FoodProperties foodProps = foodStack.getFoodProperties(player);
        if (foodProps == null) {
            return false;
        }
        
        // 从饭盒中取出食物
        ItemStack extractedFood = itemHandler.extractItem(slotToUse, 1, false);
        if (extractedFood.isEmpty()) {
            return false;
        }
        
        // 获取玩家状态
        PlayerRuntimeState state = CommonForgeEvents.state(player);
        if (state == null) {
            return false;
        }
        
        System.out.println("[EatTheWorld] ========== Bento Box Food Consumption ==========");
        System.out.println("[EatTheWorld] Food: " + extractedFood.getItem());
        System.out.println("[EatTheWorld] Food ID: " + BuiltInRegistries.ITEM.getKey(extractedFood.getItem()));
        System.out.println("[EatTheWorld] Diet compat enabled: " + com.rz.eattheworld.ModConfigs.foodDietCompatEnabled);
        System.out.println("[EatTheWorld] Diet loaded: " + com.rz.eattheworld.compat.DietCompat.isLoaded());
        
        // 🔥 新策略：让Diet完全处理食物消费，然后我们再调整
        // 1. 标记这是饭盒触发的事件
        state.isBentoBoxTriggeredEvent = true;
        
        // 2. 保存进食前的状态（用于我们的食物标记系统）
        int oldFoodLevel = player.getFoodData().getFoodLevel();
        float oldSaturation = player.getFoodData().getSaturationLevel();
        
        state.eatStartFoodLevel = oldFoodLevel;
        state.eatStartSaturation = oldSaturation;
        state.hasEatSnapshot = true;
        
        // 3. 保存进食前的标记
        if (ModConfigs.foodDecrementEnabled) {
            state.pendingFoodMarkerBeforeEat = state.foodTracker.getFoodMarker(extractedFood);
        }
        
        // 4. 设置待处理的食物项
        state.pendingFoodAdjust = true;
        state.pendingFoodItemId = String.valueOf(BuiltInRegistries.ITEM.getKey(extractedFood.getItem()));
        state.pendingFoodItem = extractedFood.copy();
        
        // 5. 更新食物标记
        if (ModConfigs.foodDecrementEnabled) {
            long gameTime = player.level().getGameTime();
            state.foodTracker.updateFoodMarker(extractedFood, gameTime);
        }
        
        // 6. 标记这是饭盒触发的，让handlePendingFoodAdjust知道这是饭盒模式
        state.isBentoBoxTriggeredEvent = true;
        
        try {
            // 🔥 关键：直接调用FoodData.eat()来触发Diet的Mixin（如果Diet兼容已启用）
            // Diet通过Mixin钩住FoodData.eat()方法来记录营养值
            if (com.rz.eattheworld.ModConfigs.foodDietCompatEnabled && com.rz.eattheworld.compat.DietCompat.isLoaded()) {
                player.getFoodData().eat(extractedFood.getItem(), extractedFood);
                System.out.println("[EatTheWorld] Called FoodData.eat() - Diet's mixin should have intercepted this");
            } else {
                // 如果Diet兼容未启用或Diet未加载，直接调用eat方法
                player.getFoodData().eat(extractedFood.getItem(), extractedFood);
                System.out.println("[EatTheWorld] Called FoodData.eat() without Diet compatibility");
            }
            
            // 🔥 重要：应用食物的药水效果（附魔金苹果等）
            applyFoodEffects(player, extractedFood, foodProps);
            
            // 🔥 重要：处理食物的返回物品（如蘑菇煲返回碗）
            if (com.rz.eattheworld.ModConfigs.foodContainerReturnEnabled) {
                handleFoodContainerReturn(player, extractedFood, bentoBoxStack);
            }
            
            // 同时也触发标准的 LivingEntityUseItemEvent.Finish 事件（给其他mod用，如SolCarrot）
            if (com.rz.eattheworld.ModConfigs.foodSolCarrotCompatEnabled) {
                net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish finishEvent = 
                    new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish(
                        player, extractedFood, extractedFood.getUseDuration(), extractedFood.copy());
                
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(finishEvent);
                System.out.println("[EatTheWorld] Posted LivingEntityUseItemEvent.Finish for SolCarrot compatibility");
            }
            
            System.out.println("[EatTheWorld] Our food marker system will apply in next tick");
            System.out.println("[EatTheWorld] =============================================");
            
        } catch (Exception e) {
            System.out.println("[EatTheWorld] Failed to call FoodData.eat(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 注意：不要在这里清除 isBentoBoxTriggeredEvent
            // 让 handlePendingFoodAdjust 知道这是饭盒触发的
        }
        
        // 11. 如果是顺序模式，更新索引
        if (mode == FeedMode.SEQUENTIAL) {
            int nextIndex = (slotToUse + 1) % itemHandler.getSlots();
            setSequentialSlotIndex(bentoBoxStack, nextIndex);
        }
        
        return true;
    }
    
    /**
     * 检查饭盒是否有食物
     */
    public static boolean hasFood(ItemStack bentoBoxStack) {
        LazyOptional<IItemHandler> handlerOpt = bentoBoxStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (!handlerOpt.isPresent()) {
            return false;
        }
        
        IItemHandler itemHandler = handlerOpt.resolve().get();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && isFood(stack)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取当前要进食的食物的使用时间（受标记影响）
     */
    public static int getUseDuration(ItemStack bentoBoxStack) {
        // 获取当前要进食的食物
        ItemStack foodStack = getNextFood(bentoBoxStack);
        if (foodStack.isEmpty()) {
            return 32; // 默认进食时间
        }
        
        // 获取食物的基础使用时间
        int baseDuration = foodStack.getUseAnimation() == UseAnim.EAT ? 32 : 32;
        
        // 应用速度惩罚（如果启用了食物递减）
        if (ModConfigs.foodDecrementEnabled) {
            // 需要从某处获取玩家状态来计算速度倍率
            // 由于这里没有玩家对象，我们返回基础时间
            // 实际的速度惩罚会在 onFoodStart 事件中应用
            return baseDuration;
        }
        
        return baseDuration;
    }
    
    /**
     * 获取下一个要进食的食物（不消耗）
     */
    private static ItemStack getNextFood(ItemStack bentoBoxStack) {
        FeedMode mode = getFeedMode(bentoBoxStack);
        
        LazyOptional<IItemHandler> handlerOpt = bentoBoxStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (!handlerOpt.isPresent()) {
            return ItemStack.EMPTY;
        }
        
        IItemHandler itemHandler = handlerOpt.resolve().get();
        
        if (mode == FeedMode.SEQUENTIAL) {
            // 顺序模式：从当前索引开始查找
            int startIndex = getSequentialSlotIndex(bentoBoxStack);
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                int checkSlot = (startIndex + i) % itemHandler.getSlots();
                ItemStack stack = itemHandler.getStackInSlot(checkSlot);
                if (!stack.isEmpty() && isFood(stack)) {
                    return stack;
                }
            }
        } else {
            // 智能模式：返回第一个找到的食物（简化版）
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty() && isFood(stack)) {
                    return stack;
                }
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * 根据模式查找要使用的食物槽位（带玩家参数用于智能模式）
     */
    private static int findFoodToUse(ServerPlayer player, IItemHandler handler, ItemStack bentoBoxStack, FeedMode mode) {
        if (mode == FeedMode.SEQUENTIAL) {
            // 顺序模式：从当前索引开始循环查找第一个有食物的槽位
            int startIndex = getSequentialSlotIndex(bentoBoxStack);
            
            for (int i = 0; i < handler.getSlots(); i++) {
                int checkSlot = (startIndex + i) % handler.getSlots();
                ItemStack stack = handler.getStackInSlot(checkSlot);
                if (!stack.isEmpty() && isFood(stack)) {
                    return checkSlot;
                }
            }
        } else if (mode == FeedMode.SMART) {
            // 智能模式：优先选择标记1的食物，如果标记相同则选择饥饿度最高的
            return findSmartFood(player, handler);
        }
        
        return -1; // 没有找到合适的食物
    }
    
    /**
     * 智能模式：优先未食用的食物，然后选择标记最小的，标记相同则选饥饿度最高的
     */
    private static int findSmartFood(ServerPlayer player, IItemHandler handler) {
        int bestSlot = -1;
        int bestMarker = Integer.MAX_VALUE;
        int bestHunger = 0;
        
        PlayerRuntimeState state = CommonForgeEvents.state(player);
        if (state == null) {
            // 如果无法获取状态，回退到简单的顺序模式
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty() && isFood(stack)) {
                    return i;
                }
            }
            return -1;
        }
        
        FoodConsumptionTracker tracker = state.foodTracker;
        
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty() || !isFood(stack)) {
                continue;
            }
            
            // 获取食物标记（注意：标记从0开始，0表示未食用过）
            int marker = tracker.getFoodMarker(stack);
            FoodProperties foodProps = stack.getItem().getFoodProperties(stack, player);
            if (foodProps == null) {
                continue;
            }
            
            int hunger = foodProps.getNutrition();
            
            // 应用食物递减效果和全局倍率
            if (ModConfigs.foodGlobalEnabled) {
                hunger = (int) Math.round(hunger * ModConfigs.foodGlobalNutritionMultiplier);
            }
            
            if (ModConfigs.foodDecrementEnabled) {
                double effectMultiplier = tracker.getEffectMultiplier(stack);
                hunger = (int) Math.round(hunger * effectMultiplier);
            }
            
            // 选择逻辑：
            // 1. 优先选择标记更小的（标记0 > 标记1 > ... > 标记5）
            // 2. 如果标记相同，选择饥饿度更高的
            if (bestSlot == -1 || marker < bestMarker || (marker == bestMarker && hunger > bestHunger)) {
                bestSlot = i;
                bestMarker = marker;
                bestHunger = hunger;
            }
        }
        
        return bestSlot;
    }
    
    private static boolean isFood(ItemStack stack) {
        return stack.getItem().getFoodProperties(stack, null) != null;
    }
    
    /**
     * 从NBT获取当前进食模式
     */
    public static FeedMode getFeedMode(ItemStack bentoBoxStack) {
        if (bentoBoxStack.hasTag() && bentoBoxStack.getTag().contains("FeedMode")) {
            String modeStr = bentoBoxStack.getTag().getString("FeedMode");
            if ("SEQUENTIAL".equals(modeStr)) {
                return FeedMode.SEQUENTIAL;
            }
        }
        return FeedMode.SMART; // 默认智能模式
    }
    
    /**
     * 设置进食模式到NBT
     */
    public static void setFeedMode(ItemStack bentoBoxStack, FeedMode mode) {
        bentoBoxStack.getOrCreateTag().putString("FeedMode", mode.name());
    }
    
    // 添加一个方法来获取顺序模式的槽位索引
    public static int getSequentialSlotIndex(ItemStack bentoBoxStack) {
        // 从NBT中获取当前顺序索引，如果不存在则返回0
        if (bentoBoxStack.hasTag() && bentoBoxStack.getTag().contains("SequentialSlotIndex")) {
            return bentoBoxStack.getTag().getInt("SequentialSlotIndex");
        }
        return 0; // 默认从第0个槽位开始
    }
    
    // 添加一个方法来更新顺序模式的槽位索引
    public static void setSequentialSlotIndex(ItemStack bentoBoxStack, int index) {
        bentoBoxStack.getOrCreateTag().putInt("SequentialSlotIndex", index);
    }
    
    /**
     * 应用食物的药水效果（如附魔金苹果的抗性提升等）
     */
    private static void applyFoodEffects(ServerPlayer player, ItemStack foodStack, FoodProperties foodProps) {
        // 应用食物的药水效果
        for (com.mojang.datafixers.util.Pair<net.minecraft.world.effect.MobEffectInstance, Float> effectPair : foodProps.getEffects()) {
            if (player.level().random.nextFloat() < effectPair.getSecond()) {
                net.minecraft.world.effect.MobEffectInstance effectInstance = effectPair.getFirst();
                if (effectInstance != null) {
                    // 应用药水效果到玩家
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effectInstance));
                    System.out.println("[EatTheWorld] Applied food effect: " + effectInstance.getEffect().getDescriptionId() + 
                                     " Duration: " + effectInstance.getDuration() + " Amplifier: " + effectInstance.getAmplifier());
                }
            }
        }
    }
    
    /**
     * 处理食物的容器返回（如蘑菇煲返回碗、牛奶桶返回桶等）
     */
    private static void handleFoodContainerReturn(ServerPlayer player, ItemStack foodStack, ItemStack bentoBoxStack) {
        // 方法1：尝试获取食物的返回物品（容器）
        ItemStack containerItem = foodStack.getCraftingRemainingItem();
        
        // 方法2：如果方法1失败，尝试通过物品的 finishUsingItem 方法获取
        if (containerItem.isEmpty()) {
            try {
                // 模拟完成使用物品，获取返回的物品
                ItemStack originalStack = foodStack.copy();
                ItemStack result = foodStack.getItem().finishUsingItem(originalStack, player.level(), player);
                
                // 检查返回的物品是否与原物品不同（说明有容器返回）
                if (!result.isEmpty() && !ItemStack.isSameItem(result, originalStack)) {
                    containerItem = result;
                } else if (!result.isEmpty() && result.getCount() != originalStack.getCount()) {
                    // 有些情况下返回的是修改了数量的同一物品，这种情况我们忽略
                    containerItem = ItemStack.EMPTY;
                }
            } catch (Exception e) {
                System.out.println("[EatTheWorld] Failed to get container via finishUsingItem: " + e.getMessage());
            }
        }
        
        if (containerItem.isEmpty()) {
            System.out.println("[EatTheWorld] No container return for food: " + foodStack.getItem());
            return; // 没有返回物品
        }
        
        System.out.println("[EatTheWorld] Food has container return: " + containerItem.getItem() + " (from " + foodStack.getItem() + ")");
        
        // 尝试将返回物品放回饭盒
        LazyOptional<IItemHandler> handlerOpt = bentoBoxStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (handlerOpt.isPresent()) {
            IItemHandler itemHandler = handlerOpt.resolve().get();
            
            // 尝试插入返回物品到饭盒的空槽位
            ItemStack remaining = containerItem.copy();
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                remaining = itemHandler.insertItem(i, remaining, false);
                if (remaining.isEmpty()) {
                    System.out.println("[EatTheWorld] Container item inserted into bento box slot " + i);
                    return; // 成功插入，完成
                }
            }
            
            // 如果饭盒满了，尝试放入玩家背包
            if (!remaining.isEmpty()) {
                if (player.getInventory().add(remaining)) {
                    System.out.println("[EatTheWorld] Container item added to player inventory");
                } else {
                    // 背包也满了，掉落到地上
                    player.drop(remaining, false);
                    System.out.println("[EatTheWorld] Container item dropped on ground");
                }
            }
        } else {
            // 无法访问饭盒，直接给玩家
            if (player.getInventory().add(containerItem)) {
                System.out.println("[EatTheWorld] Container item added to player inventory (fallback)");
            } else {
                player.drop(containerItem, false);
                System.out.println("[EatTheWorld] Container item dropped on ground (fallback)");
            }
        }
    }
}