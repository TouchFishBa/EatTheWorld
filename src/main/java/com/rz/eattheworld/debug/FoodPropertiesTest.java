package com.rz.eattheworld.debug;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 食物属性测试类
 * 用于测试和调试食物属性
 */
public class FoodPropertiesTest {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static void testFoodProperties(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        
        FoodProperties foodProperties = itemStack.getItem().getFoodProperties();
        if (foodProperties != null) {
            LOGGER.debug("Food nutrition: {}", foodProperties.getNutrition());
            LOGGER.debug("Food saturation: {}", foodProperties.getSaturationModifier());
        }
    }
}
