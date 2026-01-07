package com.rz.eattheworld;

import com.mojang.logging.LogUtils;
import com.rz.eattheworld.compat.Compat;
import com.rz.eattheworld.network.SyncFoodMarkersPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Inventory;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EatTheWorldMod.MODID)
public class EatTheWorldMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "eattheworld";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // Template registers (safe to keep; will be cleaned up when implementing real features)
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // 添加容器类型注册
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // 注册饭盒物品
    public static final RegistryObject<Item> BENTO_BOX = ITEMS.register("bento_box", () -> new com.rz.eattheworld.items.BentoBoxItem());

    // 注册饭盒容器类型 - 使用正确的构造方式
    public static final RegistryObject<MenuType<com.rz.eattheworld.items.container.BentoBoxMenu>> BENTO_BOX_MENU = 
        MENU_TYPES.register("bento_box_menu", 
            () -> new MenuType<>(com.rz.eattheworld.items.container.BentoBoxMenu::new, FeatureFlagSet.of(FeatureFlags.VANILLA)));

    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("eattheworld", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.eattheworld.eattheworld"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> BENTO_BOX.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // 添加饭盒到创造模式标签页
                output.accept(BENTO_BOX.get());
            })
            .build());

    public EatTheWorldMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        // 注册容器类型
        MENU_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfigs.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Compat.init();
        
        // 注册网络包 - 必须在enqueueWork之前触发PacketHandler类加载
        // 这样CHANNEL的静态初始化会在正确的时机执行
        Class<?> packetHandlerClass = com.rz.eattheworld.network.PacketHandler.class;
        
        event.enqueueWork(() -> {
            SyncFoodMarkersPacket.register();
            com.rz.eattheworld.network.PacketHandler.register();
            
            // 在enqueueWork中初始化mod兼容性，确保所有mod都已加载
            System.out.println("[EatTheWorld] Initializing mod compatibility in common setup...");
            com.rz.eattheworld.compat.SolCarrotCompat.init();
            com.rz.eattheworld.compat.DietCompat.init();
        });

        if (ModConfigs.debugEnabled) {
            LOGGER.info("EatTheWorld: common setup");
            LOGGER.info("debug.logFoodEvents={}, debug.logRegen={}", ModConfigs.debugLogFoodEvents, ModConfigs.debugLogRegen);
        }
    }


    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 不需要添加额外物品到其他标签页
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("EatTheWorld: server starting");
        
        // 测试日志
        System.out.println("[EatTheWorld] DEBUG: onServerStarting called");
        
        // 服务器启动时再次尝试初始化mod兼容性
        // 这时所有mod都应该已经加载完成
        System.out.println("[EatTheWorld] Initializing mod compatibility on server start...");
        com.rz.eattheworld.compat.SolCarrotCompat.init();
        com.rz.eattheworld.compat.DietCompat.init();
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("EatTheWorld: client setup");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}