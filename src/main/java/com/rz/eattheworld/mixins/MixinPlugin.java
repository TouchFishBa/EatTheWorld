package com.rz.eattheworld.mixins;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin插件，用于动态控制Mixin的加载
 * 主要用于处理可选依赖（如AppleSkin）
 */
public class MixinPlugin implements IMixinConfigPlugin {
    
    private static boolean isAppleSkinLoaded = false;
    
    @Override
    public void onLoad(String mixinPackage) {
        // 检测AppleSkin是否加载
        try {
            Class.forName("squeek.appleskin.ModConfig");
            isAppleSkinLoaded = true;
            System.out.println("[EatTheWorld] AppleSkin detected, enabling compatibility mixins");
        } catch (ClassNotFoundException e) {
            isAppleSkinLoaded = false;
            System.out.println("[EatTheWorld] AppleSkin not found, disabling compatibility mixins");
        }
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // 如果是AppleSkin相关的Mixin，只有在AppleSkin加载时才应用
        if (mixinClassName.contains("AppleSkin")) {
            return isAppleSkinLoaded;
        }
        
        // 其他Mixin正常加载
        return true;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
