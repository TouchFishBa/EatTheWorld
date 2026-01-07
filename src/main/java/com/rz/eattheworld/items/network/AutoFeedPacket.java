package com.rz.eattheworld.items.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class AutoFeedPacket {
    private final int feedMode;

    public AutoFeedPacket(int feedMode) {
        this.feedMode = feedMode;
    }

    public AutoFeedPacket(FriendlyByteBuf buf) {
        this.feedMode = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.feedMode);
    }

    public boolean handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                // 在服务器端执行自动进食逻辑
                // 找到玩家手持的饭盒
                ItemStack bentoBoxStack = player.getMainHandItem();
                if (bentoBoxStack.getItem() instanceof com.rz.eattheworld.items.BentoBoxItem) {
                    // 这里需要实现自动进食逻辑
                    // 暂时只是占位符
                }
            }
        });
        return true;
    }
}