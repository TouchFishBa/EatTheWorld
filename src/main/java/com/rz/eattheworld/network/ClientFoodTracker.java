package com.rz.eattheworld.network;

import java.util.HashMap;
import java.util.Map;

public class ClientFoodTracker {
    private static final Map<String, Integer> foodMarkers = new HashMap<>();
    private static final Map<String, Integer> foodActualCounts = new HashMap<>();
    private static final Map<String, Long> foodLastConsumedTime = new HashMap<>();
    private static long currentGameTime = 0;
    private static long lastRecoveryTime = 0;

    public static void updateFoodMarkers(Map<String, Integer> newMarkers) {
        foodMarkers.clear();
        foodMarkers.putAll(newMarkers);
    }
    
    public static void updateFoodActualCounts(Map<String, Integer> newCounts) {
        foodActualCounts.clear();
        foodActualCounts.putAll(newCounts);
    }
    
    public static void updateFoodLastConsumedTime(Map<String, Long> newTimes) {
        foodLastConsumedTime.clear();
        foodLastConsumedTime.putAll(newTimes);
    }
    
    public static void updateCurrentGameTime(long gameTime) {
        currentGameTime = gameTime;
    }
    
    public static void updateLastRecoveryTime(long recoveryTime) {
        lastRecoveryTime = recoveryTime;
    }

    public static int getFoodMarker(String itemId) {
        return foodMarkers.getOrDefault(itemId, 1);  // 默认值为1，表示未食用过（第一次吃应该100%效果）
    }
    
    public static int getFoodActualCount(String itemId) {
        return foodActualCounts.getOrDefault(itemId, 0);
    }
    
    public static long getFoodLastConsumedTime(String itemId) {
        return foodLastConsumedTime.getOrDefault(itemId, 0L);
    }
    
    public static long getCurrentGameTime() {
        return currentGameTime;
    }
    
    public static long getLastRecoveryTime() {
        return lastRecoveryTime;
    }

    public static Map<String, Integer> getFoodMarkersCopy() {
        return new HashMap<>(foodMarkers);
    }
    
    public static Map<String, Integer> getFoodActualCountsCopy() {
        return new HashMap<>(foodActualCounts);
    }
    
    public static Map<String, Long> getFoodLastConsumedTimeCopy() {
        return new HashMap<>(foodLastConsumedTime);
    }

    public static void clear() {
        foodMarkers.clear();
        foodActualCounts.clear();
        foodLastConsumedTime.clear();
        currentGameTime = 0;
        lastRecoveryTime = 0;
    }
}