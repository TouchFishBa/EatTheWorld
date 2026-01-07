package com.rz.eattheworld.events;

import com.rz.eattheworld.EatTheWorldMod;
import com.rz.eattheworld.commands.TestMixinCommand;
import com.rz.eattheworld.commands.DietDebugCommand;
import com.rz.eattheworld.commands.ResetFoodMarkersCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EatTheWorldMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandEvents {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TestMixinCommand.register(event.getDispatcher());
        DietDebugCommand.register(event.getDispatcher());
        ResetFoodMarkersCommand.register(event.getDispatcher());
    }
}