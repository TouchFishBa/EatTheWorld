package com.rz.eattheworld.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import com.rz.eattheworld.EatTheWorldMod;
import com.rz.eattheworld.items.BentoBoxItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 尝试通过反射注册AppleSkin的事件
 */
@Mod.EventBusSubscriber(modid = EatTheWorldMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AppleSkinEventHandler {
    
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean APPLESKIN_LOADED = ModList.get().isLoaded("appleskin");
    
    static {
        if (APPLESKIN_LOADED) {
            try {
                // 尝试注册AppleSkin的事件
                registerAppleSkinEvents();
            } catch (Exception e) {
                LOGGER.warn("Failed to register AppleSkin events: " + e.getMessage());
            }
        }
    }
    
    private static void registerAppleSkinEvents() {
        try {
            // 尝试找到AppleSkin的事件类
            Class<?> foodTooltipEventClass = Class.forName("squeek.appleskin.api.event.FoodTooltipEvent");
            LOGGER.info("Found AppleSkin FoodTooltipEvent class");
            
            // 这里可以尝试注册事件处理器
            // 但由于我们无法直接访问AppleSkin的API，这可能不会工作
            
        } catch (ClassNotFoundException e) {
            LOGGER.info("AppleSkin API not found, using alternative approach");
        }
    }
    
    /**
     * 通用的食物属性获取方法，可能被AppleSkin通过反射调用
     */
    public static FoodProperties getModifiedFoodProperties(ItemStack stack, Player player) {
        if (stack.getItem() instanceof BentoBoxItem bentoBox) {
            FoodProperties props = bentoBox.getFoodProperties(stack, player);
            LOGGER.debug("Providing modified food properties: nutrition={}, saturation={}", 
                props != null ? props.getNutrition() : 0, 
                props != null ? props.getSaturationModifier() : 0);
            return props;
        }
        return null;
    }
}