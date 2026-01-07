package com.rz.eattheworld.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.rz.eattheworld.network.ClientFoodTracker;
import com.rz.eattheworld.util.FoodEffectUtils;
import java.util.HashMap;
import java.util.Map;

public class TestMixinCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testmixin")
            .requires(source -> source.hasPermission(2))
            .executes(TestMixinCommand::execute));
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // 测试苹果的属性
            Item apple = Items.APPLE;
            ResourceLocation appleKey = BuiltInRegistries.ITEM.getKey(apple);
            
            if (appleKey != null) {
                source.sendSuccess(() -> Component.literal("=== Mixin测试开始 ==="), false);
                
                // 测试不同标记值
                for (int marker = 1; marker <= 5; marker++) {
                    final int finalMarker = marker; // 创建final变量供lambda使用
                    
                    Map<String, Integer> testMarkers = new HashMap<>();
                    testMarkers.put(appleKey.toString(), finalMarker);
                    ClientFoodTracker.updateFoodMarkers(testMarkers);
                    
                    FoodProperties props = apple.getFoodProperties();
                    double multiplier = FoodEffectUtils.getEffectMultiplierForMarker(finalMarker);
                    final double finalMultiplier = multiplier; // 创建final变量供lambda使用
                    
                    if (props != null) {
                        final int nutrition = props.getNutrition();
                        final float saturation = props.getSaturationModifier();
                        
                        source.sendSuccess(() -> Component.literal(
                            String.format("标记%d: 饥饿度=%d, 饱和度=%.2f, 倍率=%.2f", 
                                finalMarker, nutrition, saturation, finalMultiplier)
                        ), false);
                    }
                }
                
                source.sendSuccess(() -> Component.literal("=== Mixin测试完成 ==="), false);
            } else {
                source.sendFailure(Component.literal("无法获取苹果的ResourceLocation"));
            }
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("测试失败: " + e.getMessage()));
            e.printStackTrace();
        }
        
        return 1;
    }
}