package com.rz.eattheworld.network;

/**
 * 客户端缓存饭盒进食时间
 * 由服务器同步过来的正确进食时间
 */
public class BentoBoxDurationCache {
    private static int cachedDuration = 32;
    private static long lastUpdateTime = 0;
    
    public static void setDuration(int duration) {
        cachedDuration = duration;
        lastUpdateTime = System.currentTimeMillis();
    }
    
    public static int getDuration() {
        // 如果缓存超过100ms没更新，返回默认值
        // 100ms足够网络包到达，但不会让旧数据影响新的进食
        if (System.currentTimeMillis() - lastUpdateTime > 100) {
            return 32;
        }
        return cachedDuration;
    }
    
    public static void reset() {
        cachedDuration = 32;
        lastUpdateTime = 0;
    }
}
