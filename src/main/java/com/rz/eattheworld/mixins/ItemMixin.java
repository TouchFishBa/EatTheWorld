package com.rz.eattheworld.mixins;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.rz.eattheworld.ModConfigs;
import com.rz.eattheworld.network.ClientFoodTracker;
import com.rz.eattheworld.util.FoodEffectUtils;

/**
 * Mixin to intercept getFoodProperties calls and apply food marker effects
 * This ensures AppleSkin and other food display mods see the modified values
 */
@Mixin(value = Item.class, priority = 1500)
public class ItemMixin {
    
    // 拦截getFoodProperties(ItemStack, LivingEntity)方法
    @Inject(method = "getFoodProperties", at = @At("RETURN"), cancellable = true)
    private void modifyFoodProperties(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<FoodProperties> cir) {
        // 只在客户端修改（用于显示）
        if (entity == null || !entity.level().isClientSide) {
            return;
        }
        
        // 只有在食物递减系统启用时才修改
        if (!ModConfigs.foodDecrementEnabled) {
            return;
        }
        
        FoodProperties originalProps = cir.getReturnValue();
        if (originalProps == null) {
            return;
        }
        
        // 获取当前物品
        Item thisItem = (Item)(Object)this;
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(thisItem);
        if (itemKey == null) {
            return;
        }
        
        try {
            // 获取食物标记并应用效果倍率
            int foodMarker = ClientFoodTracker.getFoodMarker(itemKey.toString());
            
            // 获取效果倍率
            double effectMultiplier = FoodEffectUtils.getEffectMultiplierForMarker(foodMarker);
            
            // 如果倍率是1.0（第一次吃），不需要修改
            if (Math.abs(effectMultiplier - 1.0) < 0.001) {
                return;
            }
            
            int modifiedNutrition = (int) Math.round(originalProps.getNutrition() * effectMultiplier);
            float modifiedSaturation = (float) (originalProps.getSaturationModifier() * effectMultiplier);
            
            // 确保nutrition至少为1，否则Minecraft会认为这不是食物
            if (modifiedNutrition < 1) {
                modifiedNutrition = 1;
            }
            
            // 如果数值有变化，创建新的FoodProperties
            if (modifiedNutrition != originalProps.getNutrition() || 
                Math.abs(modifiedSaturation - originalProps.getSaturationModifier()) > 0.001f) {
                
                FoodProperties.Builder builder = new FoodProperties.Builder()
                    .nutrition(modifiedNutrition)
                    .saturationMod(modifiedSaturation);
                
                // 复制其他属性
                if (originalProps.canAlwaysEat()) {
                    builder.alwaysEat();
                }
                if (originalProps.isFastFood()) {
                    builder.fast();
                }
                if (originalProps.isMeat()) {
                    builder.meat();
                }
                
                // 复制状态效果
                originalProps.getEffects().forEach(pair -> {
                    builder.effect(pair.getFirst(), pair.getSecond());
                });
                
                FoodProperties newProps = builder.build();
                cir.setReturnValue(newProps);
                
                // 调试输出
                if (ModConfigs.debugEnabled) {
                    System.out.println("ItemMixin: 修改食物属性 " + itemKey);
                    System.out.println("  标记: " + foodMarker + ", 倍率: " + effectMultiplier);
                    System.out.println("  原始: 饥饿度=" + originalProps.getNutrition() + ", 饱和度=" + originalProps.getSaturationModifier());
                    System.out.println("  修改: 饥饿度=" + modifiedNutrition + ", 饱和度=" + modifiedSaturation);
                }
            }
        } catch (Exception e) {
            // 如果出错，返回原始属性，避免崩溃
            if (ModConfigs.debugEnabled) {
                System.err.println("ItemMixin错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
