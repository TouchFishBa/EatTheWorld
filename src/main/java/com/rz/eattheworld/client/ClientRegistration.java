package com.rz.eattheworld.client;

import com.rz.eattheworld.EatTheWorldMod;
import com.rz.eattheworld.items.container.BentoBoxMenu;
import com.rz.eattheworld.items.screen.BentoBoxScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = EatTheWorldMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientRegistration {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // 注册容器屏幕
        event.enqueueWork(() -> {
            MenuScreens.register(EatTheWorldMod.BENTO_BOX_MENU.get(), BentoBoxScreen::new);
        });
    }
}