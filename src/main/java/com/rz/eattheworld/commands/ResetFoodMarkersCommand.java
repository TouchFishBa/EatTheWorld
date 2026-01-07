package com.rz.eattheworld.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.rz.eattheworld.events.CommonForgeEvents;
import com.rz.eattheworld.runtime.PlayerRuntimeState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ResetFoodMarkersCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("eattheworld")
                .then(Commands.literal("reset")
                    .executes(ResetFoodMarkersCommand::resetFoodMarkers)
                )
        );
    }
    
    private static int resetFoodMarkers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayer player) {
            // 获取玩家的运行时状态
            PlayerRuntimeState state = CommonForgeEvents.state(player);
            
            if (state != null) {
                // 重置所有食物标记数据
                state.foodTracker.resetAllMarkers();
                
                // 保存到玩家NBT
                net.minecraft.nbt.CompoundTag playerData = player.getPersistentData();
                net.minecraft.nbt.CompoundTag trackerData = state.foodTracker.serializeNBT();
                playerData.put("EatTheWorld_FoodTracker", trackerData);
                
                // 同步到客户端
                com.rz.eattheworld.network.SyncFoodMarkersPacket.sendToPlayer(
                    player,
                    state.foodTracker.getFoodMarkersCopy(),
                    state.foodTracker.getFoodActualCountCopy(),
                    state.foodTracker.getFoodLastConsumedTimeCopy(),
                    player.level().getGameTime(),
                    state.foodTracker.getLastRecoveryTime()
                );
                
                // 发送成功消息
                player.sendSystemMessage(Component.translatable("command.eattheworld.reset_success"));
                return 1;
            } else {
                player.sendSystemMessage(Component.translatable("command.eattheworld.reset_failed"));
                return 0;
            }
        } else {
            source.sendFailure(Component.translatable("command.eattheworld.player_only"));
            return 0;
        }
    }
}
