package com.rz.eattheworld.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.network.NetworkEvent;
import com.rz.eattheworld.items.BentoBoxItem;

import java.util.function.Supplier;

public class SwitchBentoBoxModePacket {
    
    public SwitchBentoBoxModePacket() {
    }
    
    public static void encode(SwitchBentoBoxModePacket packet, FriendlyByteBuf buf) {
        // 不需要发送任何数据
    }
    
    public static SwitchBentoBoxModePacket decode(FriendlyByteBuf buf) {
        return new SwitchBentoBoxModePacket();
    }
    
    public static void handle(SwitchBentoBoxModePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            
            // 查找玩家手中或打开的饭盒
            ItemStack bentoBoxStack = findBentoBox(player);
            if (bentoBoxStack.isEmpty()) {
                return;
            }
            
            // 切换模式
            CompoundTag tag = bentoBoxStack.getOrCreateTag();
            String currentMode = tag.getString("FeedMode");
            
            if ("SMART".equals(currentMode)) {
                tag.putString("FeedMode", "SEQUENTIAL");
            } else {
                tag.putString("FeedMode", "SMART");
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static ItemStack findBentoBox(ServerPlayer player) {
        // 检查主手
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof BentoBoxItem) {
            return mainHand;
        }
        
        // 检查副手
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof BentoBoxItem) {
            return offHand;
        }
        
        // 检查背包中的饭盒（如果玩家打开了GUI）
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof BentoBoxItem) {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }
}
