package com.rz.eattheworld.items.container;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.network.FriendlyByteBuf;

public class BentoBoxMenu extends BentoBoxContainer {
    public BentoBoxMenu(MenuType<?> type, int windowId, Inventory playerInventory, ItemStack bentoBoxStack, BlockPos pos, Level level) {
        super(type, windowId, playerInventory, bentoBoxStack, pos, level);
    }
    
    public BentoBoxMenu(int windowId, Inventory playerInventory, ItemStack bentoBoxStack, BlockPos pos, Level level) {
        super(null, windowId, playerInventory, bentoBoxStack, pos, level);
    }
    
    // 添加一个符合MenuType要求的构造函数，只接受windowId和Inventory（用于注册）
    public BentoBoxMenu(int windowId, Inventory playerInventory) {
        super(null, windowId, playerInventory, playerInventory.player.getMainHandItem(), BlockPos.ZERO, playerInventory.player.level());
    }
    
    // 添加一个从网络数据包创建的构造函数
    public BentoBoxMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(null, windowId, playerInventory, buf.readItem(), buf.readBlockPos(), playerInventory.player.level());
    }
    
    // 重写getType方法，确保能正确返回类型
    @Override
    public MenuType<?> getType() {
        return com.rz.eattheworld.EatTheWorldMod.BENTO_BOX_MENU.get();
    }
}