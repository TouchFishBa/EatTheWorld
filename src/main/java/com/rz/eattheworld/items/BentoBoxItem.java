package com.rz.eattheworld.items;

import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import com.rz.eattheworld.runtime.PlayerRuntimeState;
import com.rz.eattheworld.events.CommonForgeEvents;
import com.rz.eattheworld.ModConfigs;
import com.rz.eattheworld.network.ClientFoodTracker;
import java.util.List;

public class BentoBoxItem extends Item {
    public BentoBoxItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        // 检查是否蹲下（潜行）
        if (player.isShiftKeyDown()) {
            // 潜行+右键：打开GUI
            if (!level.isClientSide) {
                openGui(level, player, itemStack);
            }
            return InteractionResultHolder.success(itemStack);
        } else {
            // 普通右键：开始进食
            // 检查是否有食物可以吃
            if (BentoBoxAutoFeeder.hasFood(itemStack) && player.canEat(false)) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(itemStack);
            }
            return InteractionResultHolder.fail(itemStack);
        }
    }
    
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            // 执行自动进食
            BentoBoxAutoFeeder.autoFeed(player, stack);
        }
        return stack;
    }
    
    @Override
    public int getUseDuration(ItemStack stack) {
        // 获取当前要进食的食物的使用时间（受标记影响）
        return BentoBoxAutoFeeder.getUseDuration(stack);
    }
    
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }
    
    @Override
    public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) {
        // 返回当前要进食的食物的属性，并应用标记效果
        ItemStack currentFood = getNextFoodFromBentoBox(stack);
        if (currentFood.isEmpty()) {
            return null;
        }
        
        FoodProperties originalProps = currentFood.getFoodProperties(entity);
        if (originalProps == null) {
            return null;
        }
        
        // 如果没有启用食物递减或全局倍率，直接返回原始属性
        if (!ModConfigs.foodDecrementEnabled && !ModConfigs.foodGlobalEnabled) {
            return originalProps;
        }
        
        // 获取当前食物的标记
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(currentFood.getItem()).toString();
        int marker = ClientFoodTracker.getFoodMarker(itemId);
        
        // 计算调整后的营养值
        int nutrition = originalProps.getNutrition();
        float saturationMod = originalProps.getSaturationModifier();
        
        // 应用全局倍率
        if (ModConfigs.foodGlobalEnabled) {
            nutrition = (int) Math.round(nutrition * ModConfigs.foodGlobalNutritionMultiplier);
            saturationMod = (float) (saturationMod * ModConfigs.foodGlobalSaturationMultiplier);
        }
        
        // 应用食物递减效果（同时应用到营养值和饱和度）
        if (ModConfigs.foodDecrementEnabled) {
            double effectMultiplier = getEffectMultiplier(marker);
            nutrition = (int) Math.round(nutrition * effectMultiplier);
            saturationMod = (float) (saturationMod * effectMultiplier);
        }
        
        // 创建新的FoodProperties
        FoodProperties.Builder builder = new FoodProperties.Builder()
            .nutrition(nutrition)
            .saturationMod(saturationMod);
        
        // 复制其他属性
        if (originalProps.isMeat()) {
            builder.meat();
        }
        if (originalProps.canAlwaysEat()) {
            builder.alwaysEat();
        }
        if (originalProps.isFastFood()) {
            builder.fast();
        }
        
        // 复制药水效果
        for (var effect : originalProps.getEffects()) {
            builder.effect(effect::getFirst, effect.getSecond());
        }
        
        return builder.build();
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        if (!ModConfigs.coreEnabled) {
            return;
        }
        
        // 获取当前要进食的食物
        ItemStack currentFood = getNextFoodFromBentoBox(stack);
        if (currentFood.isEmpty()) {
            tooltip.add(Component.translatable("gui.eattheworld.bento_box_empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        
        // 显示当前食物的名称
        String foodName = currentFood.getHoverName().getString();
        tooltip.add(Component.translatable("gui.eattheworld.current_food", foodName).withStyle(ChatFormatting.AQUA));
        
        // 显示当前模式
        BentoBoxAutoFeeder.FeedMode mode = BentoBoxAutoFeeder.getFeedMode(stack);
        String modeKey = mode == BentoBoxAutoFeeder.FeedMode.SEQUENTIAL ? "gui.eattheworld.sequential_mode" : "gui.eattheworld.smart_mode";
        tooltip.add(Component.translatable("gui.eattheworld.feed_mode", Component.translatable(modeKey)).withStyle(ChatFormatting.GRAY));
        
        // 显示食物标记信息
        if (ModConfigs.foodDecrementEnabled) {
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(currentFood.getItem()).toString();
            int marker = ClientFoodTracker.getFoodMarker(itemId);
            int actualCount = ClientFoodTracker.getFoodActualCount(itemId);
            double effectMultiplier = getEffectMultiplier(marker);
            
            net.minecraft.network.chat.MutableComponent markerText;
            ChatFormatting textColor;
            if (actualCount == 0) {
                markerText = Component.translatable("gui.eattheworld.food_never_eaten");
                textColor = ChatFormatting.BLUE;
            } else {
                markerText = Component.translatable("gui.eattheworld.food_eaten_count", actualCount);
                textColor = ChatFormatting.GREEN;
            }
            tooltip.add(markerText.withStyle(textColor));
            
            // 添加标记等级信息
            tooltip.add(Component.translatable("gui.eattheworld.marker_level", marker, effectMultiplier * 100).withStyle(ChatFormatting.YELLOW));
        }
        
        // AppleSkin会自动添加饥饿度和饱和度信息（因为我们实现了getFoodProperties）
    }
    
    /**
     * 从饭盒中获取下一个要进食的食物（不消耗）
     * 注意：这个方法用于客户端显示，应该和服务端的选择逻辑保持一致
     */
    private ItemStack getNextFoodFromBentoBox(ItemStack bentoBoxStack) {
        LazyOptional<IItemHandler> handlerOpt = bentoBoxStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (!handlerOpt.isPresent()) {
            return ItemStack.EMPTY;
        }
        
        IItemHandler itemHandler = handlerOpt.resolve().get();
        BentoBoxAutoFeeder.FeedMode mode = BentoBoxAutoFeeder.getFeedMode(bentoBoxStack);
        
        if (mode == BentoBoxAutoFeeder.FeedMode.SEQUENTIAL) {
            // 顺序模式：从当前索引开始查找
            int startIndex = BentoBoxAutoFeeder.getSequentialSlotIndex(bentoBoxStack);
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                int checkSlot = (startIndex + i) % itemHandler.getSlots();
                ItemStack stack = itemHandler.getStackInSlot(checkSlot);
                if (!stack.isEmpty() && stack.getItem().getFoodProperties(stack, null) != null) {
                    return stack;
                }
            }
        } else {
            // 智能模式：使用和服务端相同的逻辑
            int bestSlot = -1;
            int bestMarker = Integer.MAX_VALUE;
            int bestHunger = 0;
            ItemStack bestFood = ItemStack.EMPTY;
            
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem().getFoodProperties(stack, null) != null) {
                    String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    int marker = ClientFoodTracker.getFoodMarker(itemId);
                    
                    // 获取食物的饥饿度
                    FoodProperties foodProps = stack.getItem().getFoodProperties(stack, null);
                    int nutrition = foodProps != null ? foodProps.getNutrition() : 0;
                    
                    // 应用效果倍率后的饥饿度
                    if (ModConfigs.foodGlobalEnabled) {
                        nutrition = (int) Math.round(nutrition * ModConfigs.foodGlobalNutritionMultiplier);
                    }
                    
                    if (ModConfigs.foodDecrementEnabled) {
                        double effectMultiplier = getEffectMultiplier(marker);
                        nutrition = (int) Math.round(nutrition * effectMultiplier);
                    }
                    
                    // 选择逻辑：和服务端保持一致
                    // 1. 优先选择标记更小的
                    // 2. 如果标记相同，选择饥饿度更高的
                    if (bestSlot == -1 || marker < bestMarker || (marker == bestMarker && nutrition > bestHunger)) {
                        bestSlot = i;
                        bestMarker = marker;
                        bestHunger = nutrition;
                        bestFood = stack;
                    }
                }
            }
            
            return bestFood;
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * 根据标记获取效果倍率
     */
    private static double getEffectMultiplier(int marker) {
        switch (marker) {
            case 1: return 1.0; // 第一次吃 - 100%效果
            case 2: return ModConfigs.foodDecrementMarker2Effect; // 第二次吃
            case 3: return ModConfigs.foodDecrementMarker3Effect; // 第三次吃
            case 4: return ModConfigs.foodDecrementMarker4Effect; // 第四次吃
            case 5: default: return ModConfigs.foodDecrementMarker5Effect; // 第五次及以上
        }
    }

    private void openGui(Level level, Player player, ItemStack itemStack) {
        // 打开饭盒GUI
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.eattheworld.bento_box");
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId, net.minecraft.world.entity.player.Inventory inv, Player player) {
                // 从物品中获取容器数据并创建菜单
                return new com.rz.eattheworld.items.container.BentoBoxMenu(windowId, inv, itemStack, 
                    new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ()), level);
            }
        };
        
        NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, provider, buf -> {
            // 写入网络数据
            buf.writeBlockPos(new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ()));
            // 写入饭盒物品数据
            buf.writeItem(itemStack);
        });
    }
    
    // 为饭盒添加物品槽能力（27个槽位）
    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new net.minecraftforge.common.capabilities.ICapabilityProvider() {
            private final ItemStackHandler inventory = new ItemStackHandler(27) {
                @Override
                protected void onContentsChanged(int slot) {
                    super.onContentsChanged(slot);
                    // 保存到NBT
                    if (!stack.hasTag()) {
                        stack.setTag(new CompoundTag());
                    }
                    stack.getTag().put("Inventory", this.serializeNBT());
                }
                
                @Override
                public void deserializeNBT(CompoundTag nbt) {
                    super.deserializeNBT(nbt);
                }
            };
            private final LazyOptional<ItemStackHandler> inventoryLazyOptional = LazyOptional.of(() -> {
                // 从NBT加载数据
                if (stack.hasTag() && stack.getTag().contains("Inventory")) {
                    inventory.deserializeNBT(stack.getTag().getCompound("Inventory"));
                }
                return inventory;
            });

            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, net.minecraft.core.Direction side) {
                if (cap == ForgeCapabilities.ITEM_HANDLER) {
                    return inventoryLazyOptional.cast();
                }
                return LazyOptional.empty();
            }
        };
    }
}