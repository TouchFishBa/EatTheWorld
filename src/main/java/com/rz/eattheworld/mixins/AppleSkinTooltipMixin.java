package com.rz.eattheworld.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to AppleSkin's TooltipOverlayHandler to force consistent icon rendering
 * 
 * 强制 AppleSkin 始终使用相同的图标样式（红色鸡腿和金色鸡腿）
 * 无论玩家是否吃过该食物
 */
@Pseudo
@Mixin(targets = "squeek.appleskin.client.TooltipOverlayHandler", remap = false)
public class AppleSkinTooltipMixin {
    
    /**
     * Intercept the getTextureOffsets method to force normal icon style
     * This modifies the TextureOffsets object to always use y=18 (normal icons)
     */
    @ModifyVariable(
        method = "onRenderTooltip",
        at = @At("STORE"),
        ordinal = 0,
        remap = false
    )
    private static Object forceNormalTextureOffsets(Object textureOffsets) {
        try {
            // 通过反射修改 y 字段
            var yField = textureOffsets.getClass().getDeclaredField("y");
            yField.setAccessible(true);
            int currentY = yField.getInt(textureOffsets);
            
            System.out.println("[AppleSkinTooltipMixin] 当前 y 值: " + currentY);
            
            // 如果 y >= 27，说明是"已食用"图标，修改为正常图标
            if (currentY >= 27) {
                yField.setInt(textureOffsets, currentY - 9);
                System.out.println("[AppleSkinTooltipMixin] 修改 y 值为: " + (currentY - 9));
            }
        } catch (Exception e) {
            System.err.println("[AppleSkinTooltipMixin] 错误: " + e.getMessage());
        }
        
        return textureOffsets;
    }
}
