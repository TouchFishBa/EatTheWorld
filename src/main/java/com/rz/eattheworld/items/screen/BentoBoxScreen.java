package com.rz.eattheworld.items.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import com.rz.eattheworld.items.container.BentoBoxMenu;
import com.rz.eattheworld.items.BentoBoxAutoFeeder;
import com.rz.eattheworld.network.PacketHandler;
import com.rz.eattheworld.network.SwitchBentoBoxModePacket;

public class BentoBoxScreen extends AbstractContainerScreen<BentoBoxMenu> {
    // 使用原版箱子的纹理文件 - 与ChestScreen完全相同
    private static final ResourceLocation CONTAINER_BACKGROUND = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private final int containerRows;
    private Button modeButton;
    
    public BentoBoxScreen(BentoBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // 单箱子有3行
        this.containerRows = 3;
        // 计算GUI高度：标题栏(17) + 容器行数*18 + 间隔(14) + 玩家背包(3*18) + 快捷栏(18) + 底部边距(7)
        this.imageHeight = 114 + this.containerRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        // 添加模式切换按钮（右上角位置，缩小尺寸避免遮挡格子）
        int buttonX = this.leftPos + this.imageWidth - 52;
        int buttonY = this.topPos + 4;
        
        this.modeButton = this.addRenderableWidget(
            Button.builder(getModeText(), button -> {
                // 点击按钮时切换模式
                switchMode();
            })
            .bounds(buttonX, buttonY, 48, 12)
            .tooltip(getModeTooltip())
            .build()
        );
    }
    
    private net.minecraft.client.gui.components.Tooltip getModeTooltip() {
        ItemStack bentoBoxStack = this.menu.getBentoBoxStack();
        String mode = "SMART"; // 默认智能模式
        if (!bentoBoxStack.isEmpty() && bentoBoxStack.getTag() != null && bentoBoxStack.getTag().contains("FeedMode")) {
            mode = bentoBoxStack.getTag().getString("FeedMode");
        }
        
        if ("SMART".equals(mode)) {
            return net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.eattheworld.smart_mode_tooltip")
            );
        } else {
            return net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.eattheworld.sequential_mode_tooltip")
            );
        }
    }
    
    private Component getModeText() {
        ItemStack bentoBoxStack = this.menu.getBentoBoxStack();
        if (bentoBoxStack.isEmpty()) {
            return Component.translatable("gui.eattheworld.sequential_mode");
        }
        
        // 从NBT读取当前模式
        String mode = "SMART"; // 默认智能模式
        if (bentoBoxStack.getTag() != null && bentoBoxStack.getTag().contains("FeedMode")) {
            mode = bentoBoxStack.getTag().getString("FeedMode");
        }
        
        if ("SMART".equals(mode)) {
            return Component.translatable("gui.eattheworld.smart_mode");
        } else {
            return Component.translatable("gui.eattheworld.sequential_mode");
        }
    }
    
    private void switchMode() {
        // 发送网络包到服务器切换模式
        PacketHandler.sendToServer(new SwitchBentoBoxModePacket());
        
        // 客户端立即切换模式（预测性更新）
        ItemStack bentoBoxStack = this.menu.getBentoBoxStack();
        if (!bentoBoxStack.isEmpty()) {
            CompoundTag tag = bentoBoxStack.getOrCreateTag();
            String currentMode = tag.getString("FeedMode");
            
            if ("SMART".equals(currentMode)) {
                tag.putString("FeedMode", "SEQUENTIAL");
            } else {
                tag.putString("FeedMode", "SMART");
            }
        }
        
        // 更新按钮文本和tooltip
        this.modeButton.setMessage(getModeText());
        this.modeButton.setTooltip(getModeTooltip());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // 渲染容器背景 - 完全按照原版ChestScreen的方式
        guiGraphics.blit(CONTAINER_BACKGROUND, x, y, 0, 0, this.imageWidth, this.containerRows * 18 + 17);
        guiGraphics.blit(CONTAINER_BACKGROUND, x, y + this.containerRows * 18 + 17, 0, 126, this.imageWidth, 96);
        
        // 高亮当前顺序索引的槽位
        highlightCurrentSlot(guiGraphics, x, y);
    }
    
    private void highlightCurrentSlot(GuiGraphics guiGraphics, int guiLeft, int guiTop) {
        // 获取饭盒物品
        ItemStack bentoBoxStack = this.menu.getBentoBoxStack();
        if (bentoBoxStack.isEmpty()) {
            return;
        }
        
        // 检查当前模式，只有顺序模式才显示高亮
        String mode = "SMART"; // 默认智能模式
        if (bentoBoxStack.getTag() != null && bentoBoxStack.getTag().contains("FeedMode")) {
            mode = bentoBoxStack.getTag().getString("FeedMode");
        }
        
        // 智能模式不显示高亮
        if ("SMART".equals(mode)) {
            return;
        }
        
        // 获取当前顺序索引
        int startIndex = BentoBoxAutoFeeder.getSequentialSlotIndex(bentoBoxStack);
        
        // 查找从当前索引开始的第一个有食物的槽位
        int highlightIndex = -1;
        net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> handlerOpt = 
            bentoBoxStack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        
        if (handlerOpt.isPresent()) {
            net.minecraftforge.items.IItemHandler itemHandler = handlerOpt.resolve().get();
            
            // 从当前索引开始循环查找
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                int checkSlot = (startIndex + i) % itemHandler.getSlots();
                ItemStack stack = itemHandler.getStackInSlot(checkSlot);
                if (!stack.isEmpty() && stack.getItem().getFoodProperties(stack, null) != null) {
                    highlightIndex = checkSlot;
                    break;
                }
            }
        }
        
        // 如果没有找到食物，就不显示高亮
        if (highlightIndex == -1) {
            return;
        }
        
        // 计算槽位的屏幕位置
        // 饭盒有27个槽位，排列为3行9列
        int row = highlightIndex / 9;
        int col = highlightIndex % 9;
        
        // 槽位起始位置：左边距8，上边距18（标题栏17 + 1）
        int slotX = guiLeft + 8 + col * 18;
        int slotY = guiTop + 18 + row * 18;
        
        // 绘制蓝色半透明覆盖层（加深颜色，但仍能看清物品）
        // RGBA: 蓝色 (100, 149, 237) 半透明 (alpha = 100, 约40%透明度)
        guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x646495ED);
    }
}