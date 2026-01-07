package com.rz.eattheworld.compat;

import com.rz.eattheworld.ModConfigs;
import com.rz.eattheworld.food.FoodOverrideData;
import com.rz.eattheworld.network.ClientFoodTracker;
import com.rz.eattheworld.items.BentoBoxItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class AppleSkinCompat {
    private static final String EVENT_CLASS = "squeek.appleskin.api.event.FoodValuesEvent";
    private static final String FOODVALUES_CLASS = "squeek.appleskin.api.food.FoodValues";

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void init() {
        try {
            Class<?> eventClass = Class.forName(EVENT_CLASS);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (Class) eventClass, (Consumer) AppleSkinCompat::onFoodValuesRaw);
        } catch (Throwable ignored) {
            // AppleSkin 不存在或 API 变动时直接跳过
        }
    }

    private static void onFoodValuesRaw(Object event) {
        if (!ModConfigs.coreEnabled || (!ModConfigs.foodGlobalEnabled && !ModConfigs.foodOverrideEnabled && !ModConfigs.foodDecrementEnabled) || !ModConfigs.foodAppleSkinCompatEnabled) {
            return;
        }
        
        // 动态检查AppleSkin是否存在
        try {
            Class.forName(EVENT_CLASS);
        } catch (ClassNotFoundException e) {
            // AppleSkin不存在，直接返回
            return;
        }

        // 获取事件中的ItemStack
        ItemStack itemStack = tryGetItemStack(event);
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }
        
        System.out.println("[AppleSkinCompat] ===== 开始处理 FoodValuesEvent =====");
        
        // 特殊处理：如果是饭盒，显示下一个食物的属性
        if (itemStack.getItem() instanceof BentoBoxItem) {
            handleBentoBoxFoodValues(event, itemStack);
            return;
        }

        // 获取事件中的modifiedFoodValues字段并直接修改它
        Object modifiedFoodValues = tryGetModifiedFoodValues(event);
        if (modifiedFoodValues == null) {
            System.out.println("[AppleSkinCompat] 无法获取 modifiedFoodValues");
            return;
        }
        
        // 直接从ItemStack获取物品信息
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemKey == null) {
            System.out.println("[AppleSkinCompat] 无法获取 itemKey");
            return;
        }
        
        System.out.println("[AppleSkinCompat] 物品: " + itemKey);
        
        // 获取当前的hunger和saturationModifier值（作为基础值）
        Integer currentHunger = tryGetInt(modifiedFoodValues, "hunger");
        Float currentSatMod = tryGetFloat(modifiedFoodValues, "saturationModifier");
        
        if (currentHunger == null || currentSatMod == null) {
            System.out.println("[AppleSkinCompat] 无法获取当前值");
            return;
        }
        
        System.out.println("[AppleSkinCompat] 初始值: hunger=" + currentHunger + ", satMod=" + currentSatMod);
        
        int newHunger = currentHunger;
        float newSatMod = currentSatMod;
        boolean hasOverride = false;
        
        // 1. 首先检查是否有覆盖配置
        if (ModConfigs.foodOverrideEnabled) {
            FoodOverrideData.Parser.parseEntries(ModConfigs.foodOverrideEntries);
            var overrideOpt = FoodOverrideData.Parser.getOverride(itemKey);
            
            if (overrideOpt.isPresent()) {
                FoodOverrideData override = overrideOpt.get();
                if (override.enabled) {
                    hasOverride = true;
                    newHunger = override.nutrition;
                    newSatMod = override.saturationModifier;
                    System.out.println("[AppleSkinCompat] 应用覆盖: hunger=" + newHunger + ", satMod=" + newSatMod);
                }
            }
        }
        
        // 2. 如果没有覆盖，应用全局倍率
        if (!hasOverride && ModConfigs.foodGlobalEnabled) {
            double nutritionMul = ModConfigs.foodGlobalNutritionMultiplier;
            double saturationMul = ModConfigs.foodGlobalSaturationMultiplier;
            
            System.out.println("[AppleSkinCompat] 全局倍率: nutrition=" + nutritionMul + ", saturation=" + saturationMul);
            
            if (nutritionMul != 1.0 || saturationMul != 1.0) {
                newHunger = (int) Math.round(currentHunger * nutritionMul);
                newHunger = Math.max(0, newHunger);
                
                float currentSatIncrement = currentHunger * currentSatMod * 2.0f;
                float newSatIncrement = (float) (currentSatIncrement * saturationMul);
                
                if (newHunger > 0) {
                    newSatMod = newSatIncrement / (newHunger * 2.0f);
                } else {
                    newSatMod = 0.0f;
                }
                System.out.println("[AppleSkinCompat] 应用全局倍率后: hunger=" + newHunger + ", satMod=" + newSatMod);
            }
        }
        
        // 3. 最后应用食物递减效果（这个应该在最后，基于前面计算的值）
        if (ModConfigs.foodDecrementEnabled) {
            System.out.println("[AppleSkinCompat] 食物递减已启用");
            net.minecraft.client.player.LocalPlayer clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer != null) {
                int foodMarker = ClientFoodTracker.getFoodMarker(itemKey.toString());
                double effectMultiplier = getEffectMultiplierForMarker(foodMarker);
                
                System.out.println("[AppleSkinCompat] 标记: " + foodMarker + ", 倍率: " + effectMultiplier);
                System.out.println("[AppleSkinCompat] 应用递减前: hunger=" + newHunger + ", satMod=" + newSatMod);
                
                // 应用递减倍率（移除条件判断，总是应用）
                newHunger = (int) Math.round(newHunger * effectMultiplier);
                newSatMod = (float) (newSatMod * effectMultiplier);
                
                System.out.println("[AppleSkinCompat] 应用递减后: hunger=" + newHunger + ", satMod=" + newSatMod);
            } else {
                System.out.println("[AppleSkinCompat] 无法获取玩家");
            }
        }
        
        // 通过反射设置新的值
        System.out.println("[AppleSkinCompat] 最终设置: hunger=" + newHunger + ", satMod=" + newSatMod);
        trySetFoodValuesFields(modifiedFoodValues, newHunger, newSatMod);
        System.out.println("[AppleSkinCompat] ===== 处理完成 =====");
    }
    
    private static void handleBentoBoxFoodValues(Object event, ItemStack bentoBoxStack) {
        // 获取饭盒中下一个要吃的食物
        ItemStack nextFood = getNextFoodFromBentoBox(bentoBoxStack);
        if (nextFood.isEmpty()) {
            return;
        }
        
        // 获取下一个食物的原始属性
        FoodProperties foodProps = nextFood.getItem().getFoodProperties(nextFood, Minecraft.getInstance().player);
        if (foodProps == null) {
            return;
        }
        
        int nutrition = foodProps.getNutrition();
        float saturationMod = foodProps.getSaturationModifier();
        
        // 应用EatTheWorld的效果倍率
        if (ModConfigs.foodDecrementEnabled) {
            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(nextFood.getItem());
            if (itemKey != null) {
                int foodMarker = ClientFoodTracker.getFoodMarker(itemKey.toString());
                double effectMultiplier = getEffectMultiplierForMarker(foodMarker);
                
                nutrition = (int) Math.round(nutrition * effectMultiplier);
                saturationMod = (float) (saturationMod * effectMultiplier);
            }
        }
        
        // 设置到AppleSkin事件中
        Object modifiedFoodValues = tryGetModifiedFoodValues(event);
        if (modifiedFoodValues != null) {
            trySetFoodValuesFields(modifiedFoodValues, nutrition, saturationMod);
        }
    }
    
    private static ItemStack getNextFoodFromBentoBox(ItemStack bentoBoxStack) {
        // 直接从 NBT 读取物品数据
        CompoundTag stackTag = bentoBoxStack.getTag();
        if (stackTag == null || !stackTag.contains("inventory")) {
            return ItemStack.EMPTY;
        }
        
        CompoundTag inventoryTag = stackTag.getCompound("inventory");
        
        // 创建一个临时的 ItemStackHandler
        net.minecraftforge.items.ItemStackHandler tempHandler = new net.minecraftforge.items.ItemStackHandler(27);
        tempHandler.deserializeNBT(inventoryTag);
        
        // 获取当前模式
        String mode = "SMART"; // 默认智能模式
        if (stackTag.contains("FeedMode")) {
            mode = stackTag.getString("FeedMode");
        }
        
        if ("SMART".equals(mode)) {
            // 智能模式：优先未食用，都吃过则选择次数最少的，次数相同则选择饥饿度最高的
            // 第一步：查找未食用过的食物
            for (int i = 0; i < tempHandler.getSlots(); i++) {
                ItemStack stack = tempHandler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem().getFoodProperties(stack, null) != null) {
                    ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemKey != null) {
                        int marker = ClientFoodTracker.getFoodMarker(itemKey.toString());
                        if (marker <= 1) { // 未食用过
                            return stack;
                        }
                    }
                }
            }
            
            // 第二步：如果所有食物都吃过，找到次数最少的食物
            // 如果次数相同，选择饥饿度最高的
            ItemStack bestFood = ItemStack.EMPTY;
            int lowestMarker = Integer.MAX_VALUE;
            int highestNutrition = 0;
            
            for (int i = 0; i < tempHandler.getSlots(); i++) {
                ItemStack stack = tempHandler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem().getFoodProperties(stack, null) != null) {
                    ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemKey != null) {
                        int marker = ClientFoodTracker.getFoodMarker(itemKey.toString());
                        
                        // 获取食物的饥饿度
                        net.minecraft.world.food.FoodProperties foodProps = stack.getItem().getFoodProperties(stack, null);
                        int nutrition = foodProps != null ? foodProps.getNutrition() : 0;
                        
                        // 应用效果倍率后的饥饿度
                        double effectMultiplier = getEffectMultiplierForMarker(marker);
                        int effectiveNutrition = (int) Math.round(nutrition * effectMultiplier);
                        
                        // 选择标记最小的，如果标记相同则选择饥饿度最高的
                        if (marker < lowestMarker || 
                            (marker == lowestMarker && effectiveNutrition > highestNutrition)) {
                            lowestMarker = marker;
                            highestNutrition = effectiveNutrition;
                            bestFood = stack;
                        }
                    }
                }
            }
            
            return bestFood;
        } else {
            // 顺序模式：获取当前顺序槽位索引
            int startSlot = com.rz.eattheworld.items.BentoBoxAutoFeeder.getSequentialSlotIndex(bentoBoxStack);
            
            // 从当前槽位开始查找，找到有食物的槽位
            for (int i = 0; i < tempHandler.getSlots(); i++) {
                int currentSlot = (startSlot + i) % tempHandler.getSlots();
                ItemStack stack = tempHandler.getStackInSlot(currentSlot);
                if (!stack.isEmpty() && stack.getItem().getFoodProperties(stack, null) != null) {
                    return stack;
                }
            }
        }
        
        return ItemStack.EMPTY;
    }

    private static ItemStack tryGetItemStack(Object event) {
        try {
            Method m = event.getClass().getMethod("getItemStack");
            Object result = m.invoke(event);
            if (result instanceof ItemStack) {
                return (ItemStack) result;
            }
        } catch (Throwable ignored) {
        }
        
        try {
            Field f = event.getClass().getDeclaredField("itemStack");
            f.setAccessible(true);
            Object result = f.get(event);
            if (result instanceof ItemStack) {
                return (ItemStack) result;
            }
        } catch (Throwable ignored) {
        }
        
        return null;
    }

    private static Object tryGetModifiedFoodValues(Object event) {
        // 尝试通过getter方法获取（推荐方式）
        try {
            Method m = event.getClass().getMethod("getModifiedFoodValues");
            return m.invoke(event);
        } catch (Throwable ignored) {
        }
        
        // 备选方案：直接访问字段
        try {
            Field f = event.getClass().getDeclaredField("modifiedFoodValues");
            f.setAccessible(true);
            return f.get(event);
        } catch (Throwable ignored) {
        }
        
        return null;
    }
    
    private static ResourceLocation tryGetItemKey(Object event) {
        // 尝试通过getter方法获取（推荐方式）
        try {
            Method m = event.getClass().getMethod("getItemStack");
            Object itemStack = m.invoke(event);
            
            if (itemStack != null) {
                Method getItemMethod = itemStack.getClass().getMethod("getItem");
                Object item = getItemMethod.invoke(itemStack);
                
                if (item instanceof net.minecraft.world.item.Item) {
                    return BuiltInRegistries.ITEM.getKey((net.minecraft.world.item.Item) item);
                }
            }
        } catch (Throwable ignored) {
        }
        
        // 备选方案：直接访问字段
        try {
            Field f = event.getClass().getDeclaredField("itemStack");
            f.setAccessible(true);
            Object itemStack = f.get(event);
            
            if (itemStack != null) {
                Method getItemMethod = itemStack.getClass().getMethod("getItem");
                Object item = getItemMethod.invoke(itemStack);
                
                if (item instanceof net.minecraft.world.item.Item) {
                    return BuiltInRegistries.ITEM.getKey((net.minecraft.world.item.Item) item);
                }
            }
        } catch (Throwable ignored) {
        }
        
        return null;
    }
    

    
    private static void trySetFoodValuesFields(Object foodValues, int hunger, float satMod) {
        try {
            Field hungerField = foodValues.getClass().getDeclaredField("hunger");
            hungerField.setAccessible(true);
            hungerField.set(foodValues, hunger);
            
            Field satModField = foodValues.getClass().getDeclaredField("saturationModifier");
            satModField.setAccessible(true);
            satModField.set(foodValues, satMod);
            
            // 验证设置是否成功
            int verifyHunger = hungerField.getInt(foodValues);
            float verifySatMod = satModField.getFloat(foodValues);
            System.out.println("[AppleSkinCompat] 验证设置: hunger=" + verifyHunger + ", satMod=" + verifySatMod);
            
        } catch (Throwable e) {
            System.out.println("[AppleSkinCompat] 设置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Integer tryGetInt(Object obj, String... names) {
        for (String name : names) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object r = m.invoke(obj);
                if (r instanceof Integer i) {
                    return i;
                }
            } catch (Throwable ignored) {
            }

            try {
                Field f = obj.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object r = f.get(obj);
                if (r instanceof Integer i) {
                    return i;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Float tryGetFloat(Object obj, String... names) {
        for (String name : names) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object r = m.invoke(obj);
                if (r instanceof Float f) {
                    return f;
                }
            } catch (Throwable ignored) {
            }

            try {
                Field f = obj.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object r = f.get(obj);
                if (r instanceof Float fl) {
                    return fl;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
    
    /**
     * 内联的效果倍率计算方法，避免类加载问题
     */
    private static double getEffectMultiplierForMarker(int marker) {
        switch (marker) {
            case 1: return 1.0;  // 第一次吃 - 100%效果
            case 2: return ModConfigs.foodDecrementMarker2Effect;  // 第二次吃
            case 3: return ModConfigs.foodDecrementMarker3Effect;  // 第三次吃
            case 4: return ModConfigs.foodDecrementMarker4Effect;  // 第四次吃
            case 5: default: return ModConfigs.foodDecrementMarker5Effect;  // 第五次及以上
        }
    }

    private AppleSkinCompat() {
    }
}
