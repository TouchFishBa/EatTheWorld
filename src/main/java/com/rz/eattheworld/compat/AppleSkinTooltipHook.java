package com.rz.eattheworld.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.rz.eattheworld.EatTheWorldMod;
import com.rz.eattheworld.ModConfigs;
import com.rz.eattheworld.network.ClientFoodTracker;
import com.rz.eattheworld.util.FoodEffectUtils;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * 专门针对AppleSkin的tooltip hook
 * 已禁用 - 使用FoodDisplayCompat代替
 */
//@Mod.EventBusSubscriber(modid = EatTheWorldMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AppleSkinTooltipHook {
    
    //@SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltipLast(ItemTooltipEvent event) {
        // 已禁用 - 这个方法的匹配太宽泛，会错误修改其他mod的显示
    }
}