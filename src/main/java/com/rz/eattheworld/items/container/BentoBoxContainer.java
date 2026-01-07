package com.rz.eattheworld.items.container;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.world.level.Level;

public class BentoBoxContainer extends AbstractContainerMenu {
    private static final int SLOTS_COUNT = 27; // 3行9列，共27个槽位
    private final ItemStack bentoBoxStack;
    private final BlockPos pos;
    private final IItemHandler itemHandler;
    private final Level level;
    private final MenuType<?> menuType;

    public BentoBoxContainer(MenuType<?> type, int windowId, Inventory playerInventory, ItemStack bentoBoxStack, BlockPos pos, Level level) {
        super(type, windowId);
        this.menuType = type;
        this.bentoBoxStack = bentoBoxStack;
        this.pos = pos;
        this.level = level;

        // 获取饭盒的物品处理器
        LazyOptional<IItemHandler> handlerOpt = bentoBoxStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (handlerOpt.isPresent()) {
            this.itemHandler = handlerOpt.resolve().get();
        } else {
            // 如果没有能力，创建一个临时的处理器
            this.itemHandler = new ItemStackHandler(SLOTS_COUNT);
        }

        // 添加饭盒的27个槽位 (3行9列)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new SlotItemHandler(itemHandler, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // 只允许放入食物
                        return stack.getItem().getFoodProperties(stack, null) != null;
                    }
                });
            }
        }

        // 添加玩家背包槽位 (3行9列)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 添加玩家快捷栏槽位
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }
    
    public BentoBoxContainer(int windowId, Inventory playerInventory, ItemStack bentoBoxStack, BlockPos pos, Level level) {
        this(null, windowId, playerInventory, bentoBoxStack, pos, level);
    }

    @Override
    public boolean stillValid(Player player) {
        return !bentoBoxStack.isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();
            
            if (index < SLOTS_COUNT) {
                // 从饭盒槽位移动到玩家背包
                if (!this.moveItemStackTo(slotStack, SLOTS_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移动到饭盒槽位（如果是食物）
                if (slotStack.getItem().getFoodProperties(slotStack, player) != null) {
                    if (!this.moveItemStackTo(slotStack, 0, SLOTS_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // 非食物不能放入饭盒
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
    }
    
    public ItemStack getBentoBoxStack() {
        return bentoBoxStack;
    }
    
    @Override
    public MenuType<?> getType() {
        return this.menuType;
    }
}