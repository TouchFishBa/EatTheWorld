package com.rz.eattheworld.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.rz.eattheworld.EatTheWorldMod;
import com.rz.eattheworld.ModConfigs;
import com.rz.eattheworld.network.ClientFoodTracker;
import com.rz.eattheworld.util.FoodEffectUtils;
import java.util.List;

/**
 * AppleSkin显示兼容性
 * 只修改AppleSkin的🍖和♨显示行
 * 
 * 已禁用：AppleSkinCompat 已经在 FoodValuesEvent 中处理了数值修改
 */
//@Mod.EventBusSubscriber(modid = EatTheWorldMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FoodDisplayCompat {
    
    //@SubscribeEvent(priority = EventPriority.LOWEST) // 最低优先级，在所有mod之后执行
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!ModConfigs.foodDecrementEnabled) {
            return;
        }
        
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        
        if (player == null || !player.level().isClientSide) {
            return;
        }
        
        // 获取食物属性
        FoodProperties foodProps = stack.getItem().getFoodProperties(stack, player);
        if (foodProps == null) {
            return;
        }
        
        // 获取物品的资源位置
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemKey == null) {
            return;
        }
        
        // 获取食物标记
        int foodMarker = ClientFoodTracker.getFoodMarker(itemKey.toString());
        
        // 获取效果倍率
        double effectMultiplier = FoodEffectUtils.getEffectMultiplierForMarker(foodMarker);
        
        // 如果倍率是1.0（第一次吃），不需要修改
        if (Math.abs(effectMultiplier - 1.0) < 0.001) {
            return;
        }
        
        // 计算修改后的数值
        int originalNutrition = foodProps.getNutrition();
        float originalSaturation = foodProps.getSaturationModifier();
        
        int modifiedNutrition = (int) Math.round(originalNutrition * effectMultiplier);
        float modifiedSaturation = (float) (originalSaturation * effectMultiplier);
        
        // 调试：打印所有tooltip行
        if (ModConfigs.debugEnabled) {
            System.out.println("=== FoodDisplayCompat Debug ===");
            System.out.println("物品: " + itemKey);
            System.out.println("标记: " + foodMarker + ", 倍率: " + effectMultiplier);
            System.out.println("Tooltip内容:");
            List<Component> tooltip = event.getToolTip();
            for (int i = 0; i < tooltip.size(); i++) {
                String text = tooltip.get(i).getString();
                System.out.println("  [" + i + "]: " + text);
            }
        }
        
        // 遍历tooltip，只修改AppleSkin的那一行
        List<Component> tooltip = event.getToolTip();
        for (int i = 0; i < tooltip.size(); i++) {
            Component component = tooltip.get(i);
            String text = component.getString();
            
            // 调试：打印每一行的详细信息
            if (ModConfigs.debugEnabled && i > 0 && i < 5) {
                System.out.println("检查行[" + i + "]: '" + text + "'");
                System.out.println("  长度: " + text.length());
                System.out.println("  Trimmed: '" + text.trim() + "'");
                System.out.println("  Trimmed长度: " + text.trim().length());
                
                // 打印每个字符的Unicode码点
                System.out.print("  字符码点: ");
                for (int j = 0; j < Math.min(text.length(), 20); j++) {
                    System.out.print(String.format("U+%04X ", (int)text.charAt(j)));
                }
                System.out.println();
            }
            
            // 精确匹配AppleSkin的格式：使用长度和格式特征
            // AppleSkin的特征：
            // 1. 长度较短（通常8-15个字符）
            // 2. 包含数字和%
            // 3. 不包含中文字符
            // 4. 不包含冒号
            String trimmed = text.trim();
            
            // 检查是否是AppleSkin的行：长度短、包含%、不包含中文、不包含冒号
            if (trimmed.length() > 0 && trimmed.length() < 20 && 
                trimmed.contains("%") && 
                !trimmed.matches(".*[\\u4e00-\\u9fa5]+.*") && 
                !trimmed.contains(":")) {
                
                // 调试：检查排除条件
                if (ModConfigs.debugEnabled) {
                    System.out.println("  可能是AppleSkin行:");
                    System.out.println("    Trimmed: '" + trimmed + "'");
                    System.out.println("    Trimmed长度: " + trimmed.length());
                    System.out.println("    包含中文: " + trimmed.matches(".*[\\u4e00-\\u9fa5]+.*"));
                    System.out.println("    包含':': " + trimmed.contains(":"));
                    System.out.println("    包含数字: " + trimmed.matches(".*\\d+.*"));
                }
                
                // 进一步检查：必须包含数字
                if (trimmed.matches(".*\\d+.*")) {
                    // 从原始文本中提取百分比数字
                    // 格式：🍖数字 ♨数字%
                    // 使用正则表达式提取两个数字
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+).*?(\\d+)%");
                    java.util.regex.Matcher matcher = pattern.matcher(trimmed);
                    
                    if (matcher.find()) {
                        int displayedHunger = Integer.parseInt(matcher.group(1));
                        int displayedSaturation = Integer.parseInt(matcher.group(2));
                        
                        // 应用倍率到饱和度百分比
                        int newSaturationPercent = (int) Math.round(displayedSaturation * effectMultiplier);
                        
                        // 构建新的显示文本
                        String newText = String.format("🍖%d ♨%d%%", 
                            modifiedNutrition, 
                            newSaturationPercent);
                        
                        // 调试：显示计算过程
                        if (ModConfigs.debugEnabled) {
                            System.out.println("  计算过程:");
                            System.out.println("    原始饥饿度显示: " + displayedHunger);
                            System.out.println("    原始饱和度显示: " + displayedSaturation + "%");
                            System.out.println("    倍率: " + effectMultiplier);
                            System.out.println("    修改后饥饿度: " + modifiedNutrition);
                            System.out.println("    修改后饱和度: " + newSaturationPercent + "%");
                        }
                            
                        // 替换这一行
                        tooltip.set(i, Component.literal(newText));
                        
                        // 调试输出
                        if (ModConfigs.debugEnabled) {
                            System.out.println("FoodDisplayCompat: 修改AppleSkin显示");
                            System.out.println("  原始文本: '" + text + "'");
                            System.out.println("  新文本: '" + newText + "'");
                        }
                        
                        break;
                    }
                }
            }
        }
        
        if (ModConfigs.debugEnabled) {
            System.out.println("=== End Debug ===");
        }
    }
}