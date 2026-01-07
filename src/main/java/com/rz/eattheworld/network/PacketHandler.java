package com.rz.eattheworld.network;

import com.rz.eattheworld.EatTheWorldMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1.0";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(EatTheWorldMod.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel();
    
    private static int packetId = 0;
    
    public static void register() {
        CHANNEL.messageBuilder(SwitchBentoBoxModePacket.class, packetId++)
            .encoder(SwitchBentoBoxModePacket::encode)
            .decoder(SwitchBentoBoxModePacket::decode)
            .consumerMainThread(SwitchBentoBoxModePacket::handle)
            .add();
        
        CHANNEL.messageBuilder(SyncBentoBoxDurationPacket.class, packetId++)
            .encoder(SyncBentoBoxDurationPacket::encode)
            .decoder(SyncBentoBoxDurationPacket::decode)
            .consumerMainThread(SyncBentoBoxDurationPacket::handle)
            .add();
    }
    
    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }
    
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
