package com.rz.eattheworld.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

/**
 * SolCarrot (Spice of Life: Carrot Edition) 兼容
 * 使用反射动态检测和调用，不强制依赖
 */
public final class SolCarrotCompat {
    
    private static boolean initialized = false;
    private static boolean solCarrotLoaded = false;
    private static Object foodCapability = null;
    
    /**
     * 初始化SolCarrot兼容
     */
    public static void init() {
        System.out.println("[EatTheWorld] SolCarrotCompat.init() called");
        
        if (initialized) {
            System.out.println("[EatTheWorld] SolCarrotCompat already initialized, solCarrotLoaded=" + solCarrotLoaded + ", foodCapability=" + (foodCapability != null));
            return;
        }
        initialized = true;
        
        try {
            System.out.println("[EatTheWorld] Trying to load SolCarrot classes...");
            
            // 尝试加载SolCarrot的类来检测mod是否存在
            Class.forName("com.cazsius.solcarrot.SOLCarrot");
            System.out.println("[EatTheWorld] Found SOLCarrot class");
            
            // 尝试获取FoodCapability
            Class<?> foodCapabilityClass = Class.forName("com.cazsius.solcarrot.api.FoodCapability");
            System.out.println("[EatTheWorld] Found FoodCapability class");
            
            // 列出所有字段（包括私有字段）
            System.out.println("[EatTheWorld] Available public fields in FoodCapability:");
            for (java.lang.reflect.Field field : foodCapabilityClass.getFields()) {
                System.out.println("[EatTheWorld]   PUBLIC: " + field.getName() + " : " + field.getType().getSimpleName() + " (static=" + java.lang.reflect.Modifier.isStatic(field.getModifiers()) + ")");
            }
            
            System.out.println("[EatTheWorld] Available declared fields in FoodCapability:");
            for (java.lang.reflect.Field field : foodCapabilityClass.getDeclaredFields()) {
                System.out.println("[EatTheWorld]   DECLARED: " + field.getName() + " : " + field.getType().getSimpleName() + " (static=" + java.lang.reflect.Modifier.isStatic(field.getModifiers()) + ")");
            }
            
            // 列出所有方法来了解API结构
            System.out.println("[EatTheWorld] Available methods in FoodCapability:");
            for (java.lang.reflect.Method method : foodCapabilityClass.getMethods()) {
                System.out.println("[EatTheWorld]   " + method.getName() + "(" + 
                    java.util.Arrays.toString(method.getParameterTypes()) + ") -> " + method.getReturnType().getSimpleName() + 
                    " (static=" + java.lang.reflect.Modifier.isStatic(method.getModifiers()) + ")");
            }
            
            // 尝试查找SolCarrot的其他API类
            try {
                Class<?> foodTrackerClass = Class.forName("com.cazsius.solcarrot.tracking.FoodTracker");
                System.out.println("[EatTheWorld] Found FoodTracker class");
                
                System.out.println("[EatTheWorld] Available methods in FoodTracker:");
                for (java.lang.reflect.Method method : foodTrackerClass.getMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) || java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                        System.out.println("[EatTheWorld]   " + method.getName() + "(" + 
                            java.util.Arrays.toString(method.getParameterTypes()) + ") -> " + method.getReturnType().getSimpleName() + 
                            " (static=" + java.lang.reflect.Modifier.isStatic(method.getModifiers()) + ")");
                    }
                }
                
                // 尝试通过FoodTracker调用
                String[] trackerMethods = {"addFood", "eatFood", "recordFood", "onFoodEaten"};
                for (String methodName : trackerMethods) {
                    try {
                        java.lang.reflect.Method method = foodTrackerClass.getMethod(methodName, ServerPlayer.class, Item.class);
                        if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                            foodCapability = "FoodTracker"; // 标记找到了可用的方法
                            System.out.println("[EatTheWorld] Found static FoodTracker method: " + methodName);
                            break;
                        }
                    } catch (NoSuchMethodException e) {
                        // 继续尝试
                    }
                }
            } catch (ClassNotFoundException e) {
                System.out.println("[EatTheWorld] FoodTracker class not found");
            }
            
            // 尝试查找CapabilityHandler
            try {
                Class<?> capabilityHandlerClass = Class.forName("com.cazsius.solcarrot.tracking.CapabilityHandler");
                System.out.println("[EatTheWorld] Found CapabilityHandler class");
                
                System.out.println("[EatTheWorld] Available methods in CapabilityHandler:");
                for (java.lang.reflect.Method method : capabilityHandlerClass.getMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) || java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                        System.out.println("[EatTheWorld]   " + method.getName() + "(" + 
                            java.util.Arrays.toString(method.getParameterTypes()) + ") -> " + method.getReturnType().getSimpleName() + 
                            " (static=" + java.lang.reflect.Modifier.isStatic(method.getModifiers()) + ")");
                    }
                }
            } catch (ClassNotFoundException e) {
                System.out.println("[EatTheWorld] CapabilityHandler class not found");
            }
            
            // 由于没有静态字段，尝试通过方法获取capability
            // 先尝试常见的静态方法模式
            String[] capabilityMethods = {"getInstance", "getCapability", "get", "create"};
            for (String methodName : capabilityMethods) {
                try {
                    java.lang.reflect.Method method = foodCapabilityClass.getMethod(methodName);
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        foodCapability = method.invoke(null);
                        System.out.println("[EatTheWorld] Found capability through static method '" + methodName + "': " + foodCapability);
                        System.out.println("[EatTheWorld] Capability type: " + foodCapability.getClass().getName());
                        break;
                    }
                } catch (NoSuchMethodException e) {
                    System.out.println("[EatTheWorld] Static method '" + methodName + "' not found");
                } catch (Exception e) {
                    System.out.println("[EatTheWorld] Failed to call static method '" + methodName + "': " + e.getMessage());
                }
            }
            
            // 如果还是没找到，尝试通过Forge的CapabilityManager
            if (foodCapability == null) {
                System.out.println("[EatTheWorld] Trying to find capability through Forge CapabilityManager...");
                try {
                    // 尝试通过CapabilityManager获取已注册的capability
                    Class<?> capabilityManagerClass = Class.forName("net.minecraftforge.common.capabilities.CapabilityManager");
                    java.lang.reflect.Method getMethod = capabilityManagerClass.getMethod("get", Class.class);
                    
                    // 尝试获取FoodList接口的capability
                    try {
                        Class<?> foodListClass = Class.forName("com.cazsius.solcarrot.tracking.FoodList");
                        Object capability = getMethod.invoke(null, foodListClass);
                        if (capability != null) {
                            foodCapability = capability;
                            System.out.println("[EatTheWorld] Found capability through CapabilityManager for FoodList: " + foodCapability);
                        }
                    } catch (ClassNotFoundException e) {
                        System.out.println("[EatTheWorld] FoodList class not found");
                    }
                } catch (Exception e) {
                    System.out.println("[EatTheWorld] Failed to access CapabilityManager: " + e.getMessage());
                }
            }
            
            // 最后的备选方案：标记为已加载但没有capability，使用直接方法调用
            if (foodCapability == null) {
                System.out.println("[EatTheWorld] No capability found, will try direct method calls");
            }
            
            solCarrotLoaded = true;
            System.out.println("[EatTheWorld] ✓ SolCarrot detected, compatibility enabled (foodCapability=" + (foodCapability != null) + ")");
        } catch (ClassNotFoundException e) {
            solCarrotLoaded = false;
            System.out.println("[EatTheWorld] ✗ SolCarrot not found: " + e.getMessage());
        } catch (Throwable e) {
            solCarrotLoaded = false;
            System.out.println("[EatTheWorld] ✗ Failed to initialize SolCarrot compatibility: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 通知SolCarrot玩家吃了食物
     * 
     * @param player 玩家
     * @param foodStack 食物ItemStack
     */
    public static void notifyFoodEaten(ServerPlayer player, ItemStack foodStack) {
        // 检查配置是否启用SolCarrot兼容
        if (!com.rz.eattheworld.ModConfigs.foodSolCarrotCompatEnabled) {
            System.out.println("[EatTheWorld] SolCarrot compatibility disabled in config");
            return;
        }
        
        System.out.println("[EatTheWorld] notifyFoodEaten called for: " + foodStack.getItem());
        
        if (!solCarrotLoaded) {
            System.out.println("[EatTheWorld] SolCarrot not loaded, skipping");
            return;
        }
        
        // 如果FOOD_CAPABILITY为null，尝试重新初始化
        if (foodCapability == null) {
            System.out.println("[EatTheWorld] FOOD_CAPABILITY is null, trying to reinitialize...");
            init(); // 重新尝试初始化
            
            if (foodCapability == null) {
                System.out.println("[EatTheWorld] FOOD_CAPABILITY still null after reinit, skipping");
                return;
            }
        }
        
        try {
            System.out.println("[EatTheWorld] Getting FoodList from player capability...");
            
            // 如果有capability，使用标准的Forge Capability系统
            if (foodCapability != null && !foodCapability.equals("FoodTracker")) {
                net.minecraftforge.common.util.LazyOptional<?> lazyOptional = 
                    player.getCapability((net.minecraftforge.common.capabilities.Capability<?>) foodCapability);
                
                System.out.println("[EatTheWorld] LazyOptional present: " + lazyOptional.isPresent());
                
                if (lazyOptional.isPresent()) {
                    Object foodList = lazyOptional.resolve().get();
                    System.out.println("[EatTheWorld] ✓ FoodList obtained: " + foodList.getClass().getName());
                    
                    // 尝试调用addFood方法
                    if (tryCallAddFood(foodList, foodStack)) {
                        return; // 成功了就返回
                    }
                } else {
                    System.out.println("[EatTheWorld] ✗ Player does not have FoodCapability");
                }
            }
            
            // 尝试通过FoodTracker类直接调用
            System.out.println("[EatTheWorld] Trying FoodTracker direct approach...");
            try {
                Class<?> foodTrackerClass = Class.forName("com.cazsius.solcarrot.tracking.FoodTracker");
                
                // 尝试各种可能的方法名和参数组合
                String[] methodNames = {"addFood", "eatFood", "recordFood", "onFoodEaten", "trackFood"};
                boolean success = false;
                
                for (String methodName : methodNames) {
                    // 尝试静态方法 (Player, Item)
                    try {
                        java.lang.reflect.Method method = foodTrackerClass.getMethod(methodName, ServerPlayer.class, Item.class);
                        if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                            method.invoke(null, player, foodStack.getItem());
                            System.out.println("[EatTheWorld] ✓ Successfully called static FoodTracker." + methodName + "(Player, Item)");
                            success = true;
                            break;
                        }
                    } catch (NoSuchMethodException e) {
                        // 尝试静态方法 (Player, ItemStack)
                        try {
                            java.lang.reflect.Method method = foodTrackerClass.getMethod(methodName, ServerPlayer.class, ItemStack.class);
                            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                                method.invoke(null, player, foodStack);
                                System.out.println("[EatTheWorld] ✓ Successfully called static FoodTracker." + methodName + "(Player, ItemStack)");
                                success = true;
                                break;
                            }
                        } catch (NoSuchMethodException e2) {
                            // 尝试实例方法 - 需要先获取实例
                            try {
                                // 尝试获取FoodTracker实例
                                java.lang.reflect.Constructor<?> constructor = foodTrackerClass.getConstructor();
                                Object trackerInstance = constructor.newInstance();
                                
                                java.lang.reflect.Method method = foodTrackerClass.getMethod(methodName, ServerPlayer.class, Item.class);
                                method.invoke(trackerInstance, player, foodStack.getItem());
                                System.out.println("[EatTheWorld] ✓ Successfully called FoodTracker instance." + methodName + "(Player, Item)");
                                success = true;
                                break;
                            } catch (Exception e3) {
                                // 继续尝试下一个方法
                            }
                        } catch (Exception e2) {
                            System.out.println("[EatTheWorld] Error calling FoodTracker." + methodName + "(Player, ItemStack): " + e2.getMessage());
                        }
                    } catch (Exception e) {
                        System.out.println("[EatTheWorld] Error calling FoodTracker." + methodName + "(Player, Item): " + e.getMessage());
                    }
                }
                
                if (success) {
                    return; // 成功了就返回
                }
            } catch (ClassNotFoundException e) {
                System.out.println("[EatTheWorld] FoodTracker class not found");
            }
            
            // 如果FoodTracker失败，尝试通过事件系统
            System.out.println("[EatTheWorld] Trying event-based approach...");
            try {
                // 尝试触发Forge的LivingEntityUseItemEvent.Finish事件
                // 这可能会被SolCarrot监听到
                // 构造函数需要: LivingEntity, ItemStack, int, ItemStack
                // 第四个参数是结果ItemStack，对于食物通常是空的或者是原ItemStack
                net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish finishEvent = 
                    new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish(
                        player, foodStack, 0, ItemStack.EMPTY);
                
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(finishEvent);
                System.out.println("[EatTheWorld] ✓ Posted LivingEntityUseItemEvent.Finish for SolCarrot");
                return;
            } catch (Exception e) {
                System.out.println("[EatTheWorld] Failed to post finish event: " + e.getMessage());
            }
            
            // 如果capability方式失败，尝试直接通过FoodCapability类的静态方法
            System.out.println("[EatTheWorld] Trying direct static method approach...");
            Class<?> foodCapabilityClass = Class.forName("com.cazsius.solcarrot.api.FoodCapability");
            
            // 尝试静态方法来直接操作
            String[] staticMethods = {"get", "getCapability", "from", "addFood", "eatFood", "recordFood"};
            boolean success = false;
            
            for (String methodName : staticMethods) {
                try {
                    // 尝试 (Player, Item) 参数
                    java.lang.reflect.Method method = foodCapabilityClass.getMethod(methodName, ServerPlayer.class, Item.class);
                    method.invoke(null, player, foodStack.getItem());
                    System.out.println("[EatTheWorld] ✓ Successfully called static " + methodName + "(Player, Item)");
                    success = true;
                    break;
                } catch (NoSuchMethodException e1) {
                    try {
                        // 尝试 (Player, ItemStack) 参数
                        java.lang.reflect.Method method = foodCapabilityClass.getMethod(methodName, ServerPlayer.class, ItemStack.class);
                        method.invoke(null, player, foodStack);
                        System.out.println("[EatTheWorld] ✓ Successfully called static " + methodName + "(Player, ItemStack)");
                        success = true;
                        break;
                    } catch (NoSuchMethodException e2) {
                        // 尝试只有Player参数的方法，然后在返回对象上调用addFood
                        try {
                            java.lang.reflect.Method method = foodCapabilityClass.getMethod(methodName, ServerPlayer.class);
                            Object result = method.invoke(null, player);
                            if (result != null && (methodName.equals("get") || methodName.equals("getCapability") || methodName.equals("from"))) {
                                // 如果是get方法，尝试在返回的对象上调用addFood
                                if (tryCallAddFood(result, foodStack)) {
                                    System.out.println("[EatTheWorld] ✓ Successfully called " + methodName + "(Player).addFood");
                                    success = true;
                                    break;
                                }
                            }
                        } catch (NoSuchMethodException e3) {
                            // 继续尝试下一个方法
                        } catch (Exception e3) {
                            System.out.println("[EatTheWorld] Error calling " + methodName + "(Player): " + e3.getMessage());
                        }
                    } catch (Exception e2) {
                        System.out.println("[EatTheWorld] Error calling static " + methodName + "(Player, ItemStack): " + e2.getMessage());
                    }
                } catch (Exception e1) {
                    System.out.println("[EatTheWorld] Error calling static " + methodName + "(Player, Item): " + e1.getMessage());
                }
            }
            
            if (!success) {
                System.out.println("[EatTheWorld] ✗ No suitable method found in FoodCapability, trying FoodList class directly...");
                
                // 最后尝试直接通过FoodList类
                try {
                    Class<?> foodListClass = Class.forName("com.cazsius.solcarrot.tracking.FoodList");
                    
                    // 尝试静态方法
                    String[] foodListMethods = {"addFood", "eatFood", "recordFood"};
                    for (String methodName : foodListMethods) {
                        try {
                            java.lang.reflect.Method method = foodListClass.getMethod(methodName, ServerPlayer.class, Item.class);
                            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                                method.invoke(null, player, foodStack.getItem());
                                System.out.println("[EatTheWorld] ✓ Successfully called static FoodList." + methodName + "(Player, Item)");
                                success = true;
                                break;
                            }
                        } catch (NoSuchMethodException e) {
                            // 继续尝试
                        } catch (Exception e) {
                            System.out.println("[EatTheWorld] Error calling static FoodList." + methodName + ": " + e.getMessage());
                        }
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("[EatTheWorld] FoodList class not found");
                }
            }
            
            if (!success) {
                System.out.println("[EatTheWorld] ✗ All SolCarrot integration attempts failed");
            }
            
        } catch (Throwable e) {
            System.out.println("[EatTheWorld] ✗ Failed to notify SolCarrot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 尝试在给定对象上调用addFood方法
     */
    private static boolean tryCallAddFood(Object foodList, ItemStack foodStack) {
        try {
            // 首先尝试Item参数
            java.lang.reflect.Method addFoodMethod = foodList.getClass().getMethod("addFood", Item.class);
            addFoodMethod.invoke(foodList, foodStack.getItem());
            System.out.println("[EatTheWorld] ✓ Successfully called addFood(Item) with: " + foodStack.getItem());
            return true;
        } catch (NoSuchMethodException e1) {
            System.out.println("[EatTheWorld] addFood(Item) not found, trying addFood(ItemStack)...");
            try {
                // 尝试ItemStack参数
                java.lang.reflect.Method addFoodMethod = foodList.getClass().getMethod("addFood", ItemStack.class);
                addFoodMethod.invoke(foodList, foodStack);
                System.out.println("[EatTheWorld] ✓ Successfully called addFood(ItemStack) with: " + foodStack);
                return true;
            } catch (NoSuchMethodException e2) {
                System.out.println("[EatTheWorld] Neither addFood(Item) nor addFood(ItemStack) found");
                
                // 列出所有可用的方法
                System.out.println("[EatTheWorld] Available methods in " + foodList.getClass().getSimpleName() + ":");
                for (java.lang.reflect.Method method : foodList.getClass().getMethods()) {
                    if (method.getDeclaringClass() == foodList.getClass()) {
                        System.out.println("[EatTheWorld]   " + method.getName() + "(" + 
                            java.util.Arrays.toString(method.getParameterTypes()) + ")");
                    }
                }
                
                // 尝试其他可能的方法名
                String[] possibleMethods = {"eatFood", "eat", "addEatenFood", "recordFood", "trackFood"};
                for (String methodName : possibleMethods) {
                    try {
                        java.lang.reflect.Method method = foodList.getClass().getMethod(methodName, Item.class);
                        method.invoke(foodList, foodStack.getItem());
                        System.out.println("[EatTheWorld] ✓ Successfully called " + methodName + "(Item) with: " + foodStack.getItem());
                        return true;
                    } catch (NoSuchMethodException e3) {
                        try {
                            java.lang.reflect.Method method = foodList.getClass().getMethod(methodName, ItemStack.class);
                            method.invoke(foodList, foodStack);
                            System.out.println("[EatTheWorld] ✓ Successfully called " + methodName + "(ItemStack) with: " + foodStack);
                            return true;
                        } catch (NoSuchMethodException e4) {
                            // 继续尝试下一个方法
                        } catch (Exception e4) {
                            System.out.println("[EatTheWorld] Error calling " + methodName + "(ItemStack): " + e4.getMessage());
                        }
                    } catch (Exception e3) {
                        System.out.println("[EatTheWorld] Error calling " + methodName + "(Item): " + e3.getMessage());
                    }
                }
                
                return false;
            } catch (Exception e2) {
                System.out.println("[EatTheWorld] Error calling addFood(ItemStack): " + e2.getMessage());
                return false;
            }
        } catch (Exception e) {
            System.out.println("[EatTheWorld] Error calling addFood(Item): " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查SolCarrot是否已加载
     */
    public static boolean isLoaded() {
        return solCarrotLoaded;
    }
    
    private SolCarrotCompat() {
    }
}
