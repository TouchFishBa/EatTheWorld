package com.rz.eattheworld.runtime;

import net.minecraft.world.item.ItemStack;
import com.rz.eattheworld.items.BentoBoxAutoFeeder;

public class PlayerRuntimeState {
    public boolean wasOnGround;
    public int serverTickCounter;
    public int lastDebugLogTick;

    public int regenTimerTicks;

    public int eatStartFoodLevel;
    public float eatStartSaturation;
    public boolean hasEatSnapshot;

    public boolean pendingFoodAdjust;
    public String pendingFoodItemId;
    public ItemStack pendingFoodItem;
    public int pendingFoodMarkerBeforeEat; // 吃之前的食物标记（用于计算效果）
    
    // 食物消耗追踪器
    public FoodConsumptionTracker foodTracker;
    
    // 饭盒相关字段
    public ItemStack bentoBoxStack = ItemStack.EMPTY;
    public BentoBoxAutoFeeder.FeedMode bentoBoxMode;
    public int bentoBoxSlot = -1;
    public boolean isEatingFromBentoBox = false;
    
    // 标记是否是饭盒触发的事件（避免重复处理）
    public boolean isBentoBoxTriggeredEvent = false;
    
    public PlayerRuntimeState() {
        this.foodTracker = new FoodConsumptionTracker();
        this.pendingFoodItem = ItemStack.EMPTY;
    }
}