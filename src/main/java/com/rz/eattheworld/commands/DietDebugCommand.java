package com.rz.eattheworld.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.rz.eattheworld.compat.DietCompat;

public class DietDebugCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("dietdebug")
                .requires(source -> source.hasPermission(2))
                .executes(DietDebugCommand::execute)
        );
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        // 检查Diet是否加载
        boolean dietLoaded = DietCompat.isLoaded();
        source.sendSuccess(() -> Component.literal("Diet Loaded: " + dietLoaded), false);
        
        if (dietLoaded) {
            // 尝试通过反射获取Diet的数据
            try {
                // 尝试获取Diet的Capability
                Class<?> dietCapabilityClass = Class.forName("com.illusivesoulworks.diet.common.capability.DietCapability");
                
                source.sendSuccess(() -> Component.literal("Diet capability class found!"), false);
                
                // 尝试获取玩家的Diet数据
                // 这里需要根据Diet的实际API来实现
                // 暂时只是检测类是否存在
                
            } catch (ClassNotFoundException e) {
                source.sendFailure(Component.literal("Diet capability class not found: " + e.getMessage()));
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error accessing Diet data: " + e.getMessage()));
            }
        }
        
        return 1;
    }
}
