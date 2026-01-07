package com.rz.eattheworld.mixins;

import com.rz.eattheworld.ModConfigs;
import com.rz.eattheworld.network.ClientFoodTracker;
import com.rz.eattheworld.util.FoodEffectUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Mixin to modify AppleSkin's FoodTooltip to apply food decrement effects
 * 
 * 这个Mixin拦截FoodTooltip的创建，修改其中的modifiedFood值以反映食物递减效果
 */
@Pseudo
@Mixin(targets = "squeek.appleskin.client.TooltipOverlayHandler$FoodTooltip", remap = false)
public class AppleSkinFoodTooltipMixin {
    
    @Shadow
    @Final
    @Mutable
    private Object modifiedFood;
    
    @Shadow
    @Final
    private ItemStack itemStack;
    
    /**
     * Inject at the TAIL of FoodTooltip constructor
     * 在FoodTooltip构造函数的最后注入，此时所有字段都已初始化
     */
    @Inject(
        method = "<init>(Lnet/minecraft/world/item/ItemStack;Lsqueek/appleskin/api/food/FoodValues;Lsqueek/appleskin/api/food/FoodValues;Lnet/minecraft/world/entity/player/Player;)V",
        at = @At("TAIL"),
        remap = false,
        require = 0  // 如果找不到方法，不要崩溃
    )
    private void modifyFoodValues(ItemStack itemStack, Object defaultFood, Object modifiedFood, Player player, CallbackInfo ci) {
        System.out.println("[AppleSkinFoodTooltipMixin] ===== Mixin被调用 =====");
        
        if (!ModConfigs.foodDecrementEnabled) {
            System.out.println("[AppleSkinFoodTooltipMixin] 食物递减未启用");
            return;
        }
        
        try {
            // 获取物品的ResourceLocation
            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            if (itemKey == null) {
                System.out.println("[AppleSkinFoodTooltipMixin] 无法获取itemKey");
                return;
            }
            
            System.out.println("[AppleSkinFoodTooltipMixin] 物品: " + itemKey);
            
            // 获取食物标记和效果倍率
            int foodMarker = ClientFoodTracker.getFoodMarker(itemKey.toString());
            double effectMultiplier = FoodEffectUtils.getEffectMultiplierForMarker(foodMarker);
            
            System.out.println("[AppleSkinFoodTooltipMixin] 标记: " + foodMarker + ", 倍率: " + effectMultiplier);
            
            // 获取当前modifiedFood的值
            Class<?> foodValuesClass = modifiedFood.getClass();
            Field hungerField = foodValuesClass.getDeclaredField("hunger");
            hungerField.setAccessible(true);
            
            Field satModField = foodValuesClass.getDeclaredField("saturationModifier");
            satModField.setAccessible(true);
            
            int currentHunger = hungerField.getInt(modifiedFood);
            float currentSatMod = satModField.getFloat(modifiedFood);
            
            System.out.println("[AppleSkinFoodTooltipMixin] 当前值: hunger=" + currentHunger + ", satMod=" + currentSatMod);
            
            // 计算新值
            int newHunger = (int) Math.round(currentHunger * effectMultiplier);
            float newSatMod = (float) (currentSatMod * effectMultiplier);
            
            System.out.println("[AppleSkinFoodTooltipMixin] 新值: hunger=" + newHunger + ", satMod=" + newSatMod);
            
            // 创建新的FoodValues对象
            Constructor<?> constructor = foodValuesClass.getDeclaredConstructor(int.class, float.class);
            constructor.setAccessible(true);
            Object newModifiedFood = constructor.newInstance(newHunger, newSatMod);
            
            // 替换this.modifiedFood
            Field thisModifiedFoodField = this.getClass().getDeclaredField("modifiedFood");
            thisModifiedFoodField.setAccessible(true);
            thisModifiedFoodField.set(this, newModifiedFood);
            
            System.out.println("[AppleSkinFoodTooltipMixin] 成功替换modifiedFood对象");
            
            // 验证
            Object verifyFood = thisModifiedFoodField.get(this);
            int verifyHunger = hungerField.getInt(verifyFood);
            float verifySatMod = satModField.getFloat(verifyFood);
            System.out.println("[AppleSkinFoodTooltipMixin] 验证: hunger=" + verifyHunger + ", satMod=" + verifySatMod);
            
        } catch (Exception e) {
            System.err.println("[AppleSkinFoodTooltipMixin] 错误: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("[AppleSkinFoodTooltipMixin] ===== 处理完成 =====");
    }
}
