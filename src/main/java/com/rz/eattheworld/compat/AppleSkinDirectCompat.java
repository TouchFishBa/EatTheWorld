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
 * 专门针对AppleSkin的兼容性处理
 * 尝试通过反射或其他方式与AppleSkin进行交互
 */
@Mod.EventBusSubscriber(modid = EatTheWorldMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AppleSkinDirectCompat {
    
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean APPLESKIN_LOADED = ModList.get().isLoaded("appleskin");
    
    static {
        if (APPLESKIN_LOADED) {
            LOGGER.info("AppleSkin detected, enabling direct compatibility mode");
            try {
                // 尝试注册AppleSkin的事件
                registerAppleSkinEvents();
            } catch (Exception e) {
                LOGGER.warn("Failed to register AppleSkin events: " + e.getMessage());
            }
        }
    }
    
    private static void registerAppleSkinEvents() {
        // 这里可以尝试注册AppleSkin的特定事件
        // 但由于我们无法直接访问AppleSkin的API，我们使用其他方法
        LOGGER.info("AppleSkin compatibility initialized");
    }
    
    /**
     * 为AppleSkin提供食物属性查询接口
     * 这个方法可能会被AppleSkin通过反射调用
     */
    public static FoodProperties getBentoBoxFoodProperties(ItemStack stack, Player player) {
        if (stack.getItem() instanceof BentoBoxItem bentoBox) {
            return bentoBox.getFoodProperties(stack, player);
        }
        return null;
    }
    
    /**
     * 检查物品是否是饭盒
     */
    public static boolean isBentoBox(ItemStack stack) {
        return stack.getItem() instanceof BentoBoxItem;
    }
    
    /**
     * 获取饭盒中下一个要进食的食物
     */
    public static ItemStack getNextFoodFromBentoBox(ItemStack bentoBoxStack, Player player) {
        if (bentoBoxStack.getItem() instanceof BentoBoxItem bentoBox) {
            // 通过反射调用私有方法
            try {
                var method = BentoBoxItem.class.getDeclaredMethod("getNextFoodToEat", Player.class, ItemStack.class);
                method.setAccessible(true);
                return (ItemStack) method.invoke(bentoBox, player, bentoBoxStack);
            } catch (Exception e) {
                LOGGER.warn("Failed to get next food from bento box: " + e.getMessage());
            }
        }
        return ItemStack.EMPTY;
    }
}