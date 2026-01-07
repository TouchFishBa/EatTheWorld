package com.rz.eattheworld.items.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FeedModePacket {
    private final int feedMode;

    public FeedModePacket(int feedMode) {
        this.feedMode = feedMode;
    }

    public FeedModePacket(FriendlyByteBuf buf) {
        this.feedMode = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.feedMode);
    }

    public boolean handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 在服务器端处理进食模式切换
            // 这里需要实现模式切换逻辑
            // 暂时只是占位符
        });
        return true;
    }
}