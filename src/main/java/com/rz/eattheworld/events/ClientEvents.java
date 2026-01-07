package com.rz.eattheworld.events;

import com.rz.eattheworld.ModConfigs;
import com.rz.eattheworld.network.ClientFoodTracker;
import com.rz.eattheworld.items.BentoBoxItem;
import com.rz.eattheworld.items.BentoBoxAutoFeeder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;

@Mod.EventBusSubscriber(modid = com.rz.eattheworld.EatTheWorldMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            tickCounter++;
            
            // 每秒更新一次（20刻 = 1秒）
            if (tickCounter >= 20) {
                tickCounter = 0;
                
                // 强制刷新tooltip（通过触发鼠标移动事件）
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.screen == null && mc.player != null) {
                    // 只在游戏界面（非GUI界面）时刷新
                    // Tooltip会在下次鼠标移动时自动更新
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!ModConfigs.coreEnabled || !ModConfigs.foodDecrementEnabled) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        Player player = event.getEntity();
        if (player == null) {
            return;
        }

        // 饭盒由BentoBoxItem自己处理tooltip，这里跳过
        if (stack.getItem() instanceof BentoBoxItem) {
            return;
        }

        // 普通食物处理
        if (stack.getFoodProperties(null) == null) {
            return; // 只处理食物
        }

        // 获取食物标记信息
        int marker = getFoodMarker(player, stack);
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int actualCount = ClientFoodTracker.getFoodActualCount(itemId);
        
        // 添加食物标记信息到工具提示
        net.minecraft.network.chat.MutableComponent markerText;
        ChatFormatting textColor;
        if (actualCount == 0) {
            markerText = Component.translatable("gui.eattheworld.food_never_eaten");
            textColor = ChatFormatting.BLUE; // 蓝色字体
        } else {
            markerText = Component.translatable("gui.eattheworld.food_eaten_count", actualCount);
            textColor = ChatFormatting.GREEN; // 绿色字体
        }
        
        // 添加食物标记信息到工具提示
        event.getToolTip().add(markerText.withStyle(textColor));
        
        // 添加标记等级信息（所有食物都显示）
        event.getToolTip().add(Component.translatable("gui.eattheworld.marker_level", marker, getEffectMultiplier(marker) * 100).withStyle(ChatFormatting.YELLOW));
        
        // 计算恢复倒计时（只有标记大于1时才显示）
        if (marker > 1) {
            long lastConsumedTime = ClientFoodTracker.getFoodLastConsumedTime(itemId);
            long recoveryInterval = ModConfigs.foodDecrementRecoveryTicks;
            
            if (lastConsumedTime > 0) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null) {
                    long currentGameTime = mc.level.getGameTime();
                    long timeSinceLastConsumed = currentGameTime - lastConsumedTime;
                    
                    // 简单逻辑：只计算距离上次食用/恢复的时间
                    if (timeSinceLastConsumed < recoveryInterval) {
                        // 还没到恢复时间，显示倒计时
                        long timeUntilRecovery = recoveryInterval - timeSinceLastConsumed;
                        displayRecoveryCountdown(event, timeUntilRecovery);
                    } else {
                        // 已经到了恢复时间，等待服务端处理
                        event.getToolTip().add(Component.translatable("gui.eattheworld.marker_recovering").withStyle(ChatFormatting.AQUA));
                    }
                }
            }
        }
    }
    
    private static void displayRecoveryCountdown(ItemTooltipEvent event, long timeUntilRecovery) {
        // 转换为秒（20刻 = 1秒）
        long secondsUntilRecovery = timeUntilRecovery / 20;
        
        // 转换为天、小时、分钟、秒
        long days = secondsUntilRecovery / 86400;
        long hours = (secondsUntilRecovery % 86400) / 3600;
        long minutes = (secondsUntilRecovery % 3600) / 60;
        long seconds = secondsUntilRecovery % 60;
        
        net.minecraft.network.chat.MutableComponent recoveryText;
        if (days > 0) {
            recoveryText = Component.translatable("gui.eattheworld.marker_recovery_time", days * 86400 + hours * 3600);
        } else if (hours > 0) {
            recoveryText = Component.translatable("gui.eattheworld.marker_recovery_time", hours * 3600 + minutes * 60);
        } else if (minutes > 0) {
            recoveryText = Component.translatable("gui.eattheworld.marker_recovery_time", minutes * 60 + seconds);
        } else {
            recoveryText = Component.translatable("gui.eattheworld.marker_recovery_time", seconds);
        }
        
        event.getToolTip().add(recoveryText.withStyle(ChatFormatting.GRAY));
    }
    
    private static int getFoodMarker(Player player, ItemStack stack) {
        // 从网络同步的数据中获取食物标记
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return ClientFoodTracker.getFoodMarker(itemId);
    }
    
    private static double getEffectMultiplier(int marker) {
        switch (marker) {
            case 1: return 1.0; // 第一次吃 - 100%效果
            case 2: return ModConfigs.foodDecrementMarker2Effect; // 第二次吃
            case 3: return ModConfigs.foodDecrementMarker3Effect; // 第三次吃
            case 4: return ModConfigs.foodDecrementMarker4Effect; // 第四次吃
            case 5: default: return ModConfigs.foodDecrementMarker5Effect; // 第五次及以上
        }
    }
}