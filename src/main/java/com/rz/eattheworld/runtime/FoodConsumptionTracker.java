package com.rz.eattheworld.runtime;

import com.rz.eattheworld.ModConfigs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class FoodConsumptionTracker {
    // 记录每种食物的标记值（食用次数，用于效果计算，上限5）
    private final Map<String, Integer> foodMarkers = new HashMap<>();
    
    // 记录每种食物实际吃过的总次数（不受上限限制，用于显示）
    private final Map<String, Integer> foodActualCount = new HashMap<>();
    
    // 记录每种食物最后食用时间（游戏刻）
    private final Map<String, Long> foodLastConsumedTime = new HashMap<>();
    
    // 上次恢复标记的游戏时间（用于追踪恢复间隔）
    private long lastRecoveryTime = 0;
    
    /**
     * 获取食物的标记值（食用次数）
     */
    public int getFoodMarker(ItemStack foodItem) {
        String itemId = getItemId(foodItem);
        // 默认值为1，表示未食用过（第一次吃应该100%效果）
        return foodMarkers.getOrDefault(itemId, 1);
    }
    
    /**
     * 获取食物实际吃过的总次数（不受上限限制）
     */
    public int getFoodActualCount(ItemStack foodItem) {
        String itemId = getItemId(foodItem);
        return foodActualCount.getOrDefault(itemId, 0);
    }
    
    /**
     * 更新食物标记值（在食用食物时调用）
     */
    public void updateFoodMarker(ItemStack foodItem, long gameTime) {
        String itemId = getItemId(foodItem);
        
        // 检查是否需要恢复标记值
        restoreFoodMarkers(gameTime);
        
        // 增加食物标记值，最大为5
        // 默认值为1（第一次吃），吃完后变成2（第二次吃）
        int currentMarker = foodMarkers.getOrDefault(itemId, 1);
        int newMarker = Math.min(currentMarker + 1, 5);
        foodMarkers.put(itemId, newMarker);
        
        // 增加实际吃过的总次数（不受上限限制）
        int currentCount = foodActualCount.getOrDefault(itemId, 0);
        foodActualCount.put(itemId, currentCount + 1);
        
        // 记录最后食用时间
        foodLastConsumedTime.put(itemId, gameTime);
    }
    
    /**
     * 恢复食物标记值（基于游戏时间）
     */
    public void restoreFoodMarkers(long gameTime) {
        // 使用配置文件中定义的恢复时间（以游戏刻度为单位）
        long recoveryInterval = ModConfigs.foodDecrementRecoveryTicks;
        
        // 遍历所有食物，检查是否需要恢复
        for (String itemId : foodMarkers.keySet()) {
            long lastConsumed = foodLastConsumedTime.getOrDefault(itemId, 0L);
            
            // 如果距离上次食用/恢复已经过了指定时间，则减少标记值
            if (gameTime - lastConsumed >= recoveryInterval) {
                int currentMarker = foodMarkers.get(itemId);
                if (currentMarker > 1) {
                    int newMarker = Math.max(currentMarker - 1, 1);
                    foodMarkers.put(itemId, newMarker);
                    // 🔥 重要：恢复后更新 lastConsumedTime，重置倒计时
                    foodLastConsumedTime.put(itemId, gameTime);
                }
            }
        }
    }
    
    /**
     * 获取食物的效果倍率（基于标记值）
     */
    public double getEffectMultiplier(ItemStack foodItem) {
        int marker = getFoodMarker(foodItem);
        switch (marker) {
            case 1: return 1.0; // 第一次吃 - 100%效果
            case 2: return ModConfigs.foodDecrementMarker2Effect; // 第二次吃
            case 3: return ModConfigs.foodDecrementMarker3Effect; // 第三次吃
            case 4: return ModConfigs.foodDecrementMarker4Effect; // 第四次吃
            case 5: default: return ModConfigs.foodDecrementMarker5Effect; // 第五次及以上
        }
    }
    
    /**
     * 获取食物的进食速度倍率（基于标记值）
     */
    public double getSpeedMultiplier(ItemStack foodItem) {
        int marker = getFoodMarker(foodItem);
        switch (marker) {
            case 1: return 1.0; // 第一次吃 - 正常速度
            case 2: return ModConfigs.foodDecrementSpeedPenalty2; // 第二次吃
            case 3: return ModConfigs.foodDecrementSpeedPenalty3; // 第三次吃
            case 4: return ModConfigs.foodDecrementSpeedPenalty4; // 第四次吃
            case 5: default: return ModConfigs.foodDecrementSpeedPenalty5; // 第五次及以上
        }
    }
    
    /**
     * 获取物品ID字符串
     */
    private String getItemId(ItemStack itemStack) {
        Item item = itemStack.getItem();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        return itemId != null ? itemId.toString() : "unknown";
    }
    
    /**
     * 重置所有食物标记（用于调试或特殊场景）
     */
    public void resetAllMarkers() {
        foodMarkers.clear();
        foodActualCount.clear();
        foodLastConsumedTime.clear();
    }
    
    /**
     * 获取所有食物标记的副本（用于调试）
     */
    public Map<String, Integer> getFoodMarkersCopy() {
        return new HashMap<>(foodMarkers);
    }
    
    /**
     * 获取所有食物实际次数的副本（用于网络同步）
     */
    public Map<String, Integer> getFoodActualCountCopy() {
        return new HashMap<>(foodActualCount);
    }
    
    /**
     * 获取所有食物最后食用时间的副本（用于网络同步）
     */
    public Map<String, Long> getFoodLastConsumedTimeCopy() {
        return new HashMap<>(foodLastConsumedTime);
    }
    
    /**
     * 获取上次恢复时间
     */
    public long getLastRecoveryTime() {
        return lastRecoveryTime;
    }
    
    /**
     * 序列化到NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        
        // 保存食物标记
        CompoundTag markersTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : foodMarkers.entrySet()) {
            markersTag.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("foodMarkers", markersTag);
        
        // 保存实际次数
        CompoundTag countsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : foodActualCount.entrySet()) {
            countsTag.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("foodActualCount", countsTag);
        
        // 保存最后食用时间
        CompoundTag timesTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : foodLastConsumedTime.entrySet()) {
            timesTag.putLong(entry.getKey(), entry.getValue());
        }
        nbt.put("foodLastConsumedTime", timesTag);
        
        // 保存上次恢复时间
        nbt.putLong("lastRecoveryTime", lastRecoveryTime);
        
        return nbt;
    }
    
    /**
     * 从NBT反序列化
     */
    public void deserializeNBT(CompoundTag nbt) {
        // 清空现有数据
        foodMarkers.clear();
        foodActualCount.clear();
        foodLastConsumedTime.clear();
        
        // 加载食物标记
        if (nbt.contains("foodMarkers")) {
            CompoundTag markersTag = nbt.getCompound("foodMarkers");
            for (String key : markersTag.getAllKeys()) {
                foodMarkers.put(key, markersTag.getInt(key));
            }
        }
        
        // 加载实际次数
        if (nbt.contains("foodActualCount")) {
            CompoundTag countsTag = nbt.getCompound("foodActualCount");
            for (String key : countsTag.getAllKeys()) {
                foodActualCount.put(key, countsTag.getInt(key));
            }
        }
        
        // 加载最后食用时间
        if (nbt.contains("foodLastConsumedTime")) {
            CompoundTag timesTag = nbt.getCompound("foodLastConsumedTime");
            for (String key : timesTag.getAllKeys()) {
                foodLastConsumedTime.put(key, timesTag.getLong(key));
            }
        }
        
        // 加载上次恢复时间
        if (nbt.contains("lastRecoveryTime")) {
            lastRecoveryTime = nbt.getLong("lastRecoveryTime");
        }
    }

}