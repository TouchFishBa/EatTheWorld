package com.rz.eattheworld.compat;

/**
 * Diet模组兼容
 * Diet通过监听LivingEntityUseItemEvent.Finish事件来记录食物消费
 * 我们使用和SolCarrot相同的策略：触发标准事件让Diet自动检测
 * 
 * Diet模组功能：
 * - 食物分组系统（水果、谷物、蛋白质、蔬菜、糖类）
 * - 根据食物多样性给予玩家属性加成
 * - 通过配置文件自定义食物分组和效果
 * 
 * 兼容策略：
 * - 使用事件触发方式（在BentoBoxAutoFeeder中已实现）
 * - Diet会自动监听LivingEntityUseItemEvent.Finish事件
 * - 无需额外代码，Diet会自动记录食物消费
 * - 动态检测：Diet不存在时不会报错
 */
public final class DietCompat {
    
    private static boolean initialized = false;
    private static boolean dietLoaded = false;
    
    /**
     * 初始化Diet兼容检测
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        try {
            // 尝试加载Diet的主类来检测mod是否存在
            // Diet 2.x使用DietCommonMod作为主类
            Class.forName("com.illusivesoulworks.diet.DietCommonMod");
            dietLoaded = true;
            System.out.println("[EatTheWorld] ✓ Diet mod detected, compatibility enabled");
            System.out.println("[EatTheWorld] Diet uses Mixin to hook FoodData.eat() method");
        } catch (ClassNotFoundException e) {
            // 尝试旧版本的类名
            try {
                Class.forName("com.illusivesoulworks.diet.DietForgeMod");
                dietLoaded = true;
                System.out.println("[EatTheWorld] ✓ Diet mod detected (Forge), compatibility enabled");
                System.out.println("[EatTheWorld] Diet uses Mixin to hook FoodData.eat() method");
            } catch (ClassNotFoundException e2) {
                dietLoaded = false;
                System.out.println("[EatTheWorld] Diet mod not found, skipping compatibility");
            }
        } catch (Throwable e) {
            dietLoaded = false;
            System.out.println("[EatTheWorld] Failed to detect Diet mod: " + e.getMessage());
        }
    }
    
    /**
     * 检查Diet是否已加载
     */
    public static boolean isLoaded() {
        if (!initialized) {
            init();
        }
        return dietLoaded;
    }
    
    /**
     * Diet兼容说明：
     * 
     * Diet模组通过监听Forge的LivingEntityUseItemEvent.Finish事件来记录食物消费。
     * 我们在BentoBoxAutoFeeder.autoFeed()中已经触发了这个事件，因此：
     * 
     * 1. 饭盒进食时会触发LivingEntityUseItemEvent.Finish事件
     * 2. Diet的事件监听器会自动捕获这个事件
     * 3. Diet会自动更新玩家的食物分组数据
     * 4. Diet会自动应用相应的属性加成
     * 
     * 这是一个完全无侵入式的兼容方案：
     * - 不需要反射调用Diet的内部API
     * - 不需要修改Diet的任何数据
     * - Diet的所有功能都能正常工作
     * - 如果Diet不存在，不会有任何影响
     * 
     * 注意：此类仅用于检测Diet是否存在，实际的兼容性已经通过
     * BentoBoxAutoFeeder中的事件触发机制自动实现。
     */
    
    private DietCompat() {
    }
}
