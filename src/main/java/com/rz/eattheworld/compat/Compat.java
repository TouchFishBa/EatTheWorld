package com.rz.eattheworld.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

public final class Compat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        if (ModList.get().isLoaded("appleskin")) {
            try {
                AppleSkinCompat.init();
                LOGGER.info("[EatTheWorld] AppleSkin compat enabled");
            } catch (Throwable t) {
                LOGGER.warn("[EatTheWorld] AppleSkin compat failed to init", t);
            }
        }
    }

    private Compat() {
    }
}
