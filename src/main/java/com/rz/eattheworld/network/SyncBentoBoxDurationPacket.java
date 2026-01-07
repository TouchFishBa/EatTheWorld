package com.rz.eattheworld.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步饭盒进食时间的网络包
 * 服务器端计算正确的进食时间后发送给客户端
 */
public class SyncBentoBoxDurationPacket {
    private final int duration;
    
    public SyncBentoBoxDurationPacket(int duration) {
        this.duration = duration;
    }
    
    public static void encode(SyncBentoBoxDurationPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.duration);
    }
    
    public static SyncBentoBoxDurationPacket decode(FriendlyByteBuf buf) {
        return new SyncBentoBoxDurationPacket(buf.readInt());
    }
    
    public static void handle(SyncBentoBoxDurationPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端更新缓存的进食时间
            BentoBoxDurationCache.setDuration(packet.duration);
        });
        ctx.get().setPacketHandled(true);
    }
}
