package com.rz.eattheworld.util;

import com.rz.eattheworld.ModConfigs;

public class FoodEffectUtils {
    
    public static double getEffectMultiplierForMarker(int marker) {
        switch (marker) {
            case 1: return 1.0;  // 第一次吃 - 100%效果
            case 2: return ModConfigs.foodDecrementMarker2Effect;  // 第二次吃
            case 3: return ModConfigs.foodDecrementMarker3Effect;  // 第三次吃
            case 4: return ModConfigs.foodDecrementMarker4Effect;  // 第四次吃
            case 5: default: return ModConfigs.foodDecrementMarker5Effect;  // 第五次及以上
        }
    }
    
    public static double getSpeedMultiplierForMarker(int marker) {
        switch (marker) {
            case 1: return 1.0;  // 第一次吃 - 正常速度
            case 2: return ModConfigs.foodDecrementSpeedPenalty2;  // 第二次吃
            case 3: return ModConfigs.foodDecrementSpeedPenalty3;  // 第三次吃
            case 4: return ModConfigs.foodDecrementSpeedPenalty4;  // 第四次吃
            case 5: default: return ModConfigs.foodDecrementSpeedPenalty5;  // 第五次及以上
        }
    }
}