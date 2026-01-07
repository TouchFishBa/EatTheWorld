package com.rz.eattheworld.debug;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * SolCarrot API 测试类
 * 用于测试 SolCarrot mod 的兼容性
 */
public class SolCarrotApiTest {
    
    public static void testSolCarrotIntegration(Player player) {
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
            // 测试 SolCarrot 集成
            if (player != null) {
                // 这里可以添加 SolCarrot API 测试代码
            }
        });
    }
}
