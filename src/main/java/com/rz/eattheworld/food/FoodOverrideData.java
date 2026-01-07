package com.rz.eattheworld.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FoodOverrideData {
    public final int nutrition;
    public final float saturationModifier;
    public final boolean enabled;

    public FoodOverrideData(int nutrition, float saturationModifier, boolean enabled) {
        this.nutrition = nutrition;
        this.saturationModifier = saturationModifier;
        this.enabled = enabled;
    }

    public static class Parser {
        private static final Map<ResourceLocation, FoodOverrideData> overrideMap = new HashMap<>();

        public static void parseEntries(Iterable<? extends String> entries) {
            overrideMap.clear();
            for (String entry : entries) {
                parseAndAddEntry(entry);
            }
        }

        private static void parseAndAddEntry(String entry) {
            if (entry == null || entry.trim().isEmpty()) {
                return;
            }

            String[] parts = entry.split(",");
            if (parts.length < 3) {
                return;
            }

            String itemName = parts[0].trim();
            try {
                int nutrition = Integer.parseInt(parts[1].trim());
                float saturation = Float.parseFloat(parts[2].trim());

                ResourceLocation itemKey = ResourceLocation.tryParse(itemName);
                if (itemKey == null) {
                    return;
                }

                Item item = BuiltInRegistries.ITEM.get(itemKey);
                if (item == Items.AIR) {
                    return;
                }

                overrideMap.put(itemKey, new FoodOverrideData(nutrition, saturation, true));
            } catch (NumberFormatException e) {
                // 忽略格式错误的条目
            }
        }

        public static Optional<FoodOverrideData> getOverride(ResourceLocation itemKey) {
            return Optional.ofNullable(overrideMap.get(itemKey));
        }
    }
}