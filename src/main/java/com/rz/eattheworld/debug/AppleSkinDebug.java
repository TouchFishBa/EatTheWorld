package com.rz.eattheworld.debug;

import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AppleSkin 调试类
 * 用于调试 AppleSkin 兼容性问题
 */
public class AppleSkinDebug {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static void debugFoodTooltip(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        
        // 调试代码：输出食物信息
        LOGGER.debug("Debugging food tooltip for: {}", itemStack.getItem().getName(itemStack));
    }
}
