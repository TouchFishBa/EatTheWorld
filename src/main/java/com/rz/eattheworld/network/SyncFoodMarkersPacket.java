package com.rz.eattheworld.network;

import com.rz.eattheworld.EatTheWorldMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncFoodMarkersPacket {
    private final Map<String, Integer> foodMarkers;
    private final Map<String, Integer> foodActualCounts;
    private final Map<String, Long> foodLastConsumedTime;
    private final long currentGameTime;
    private final long lastRecoveryTime;

    public SyncFoodMarkersPacket(Map<String, Integer> foodMarkers, Map<String, Integer> foodActualCounts, 
                                  Map<String, Long> foodLastConsumedTime, long currentGameTime, long lastRecoveryTime) {
        this.foodMarkers = foodMarkers;
        this.foodActualCounts = foodActualCounts;
        this.foodLastConsumedTime = foodLastConsumedTime;
        this.currentGameTime = currentGameTime;
        this.lastRecoveryTime = lastRecoveryTime;
    }

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(EatTheWorldMod.MODID, "sync_food_markers"))
            .networkProtocolVersion(() -> "1.0")
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel();

    public static void register() {
        CHANNEL.registerMessage(0, SyncFoodMarkersPacket.class, 
            SyncFoodMarkersPacket::encode, 
            SyncFoodMarkersPacket::decode, 
            SyncFoodMarkersPacket::handle);
    }

    public static void encode(SyncFoodMarkersPacket packet, FriendlyByteBuf buffer) {
        // 写入标记数据
        buffer.writeInt(packet.foodMarkers.size());
        for (Map.Entry<String, Integer> entry : packet.foodMarkers.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeInt(entry.getValue());
        }
        
        // 写入实际次数数据
        buffer.writeInt(packet.foodActualCounts.size());
        for (Map.Entry<String, Integer> entry : packet.foodActualCounts.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeInt(entry.getValue());
        }
        
        // 写入最后食用时间数据
        buffer.writeInt(packet.foodLastConsumedTime.size());
        for (Map.Entry<String, Long> entry : packet.foodLastConsumedTime.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeLong(entry.getValue());
        }
        
        // 写入当前游戏时间
        buffer.writeLong(packet.currentGameTime);
        
        // 写入上次恢复时间
        buffer.writeLong(packet.lastRecoveryTime);
    }

    public static SyncFoodMarkersPacket decode(FriendlyByteBuf buffer) {
        // 读取标记数据
        int markerSize = buffer.readInt();
        Map<String, Integer> foodMarkers = new HashMap<>();
        for (int i = 0; i < markerSize; i++) {
            String itemId = buffer.readUtf();
            int marker = buffer.readInt();
            foodMarkers.put(itemId, marker);
        }
        
        // 读取实际次数数据
        int countSize = buffer.readInt();
        Map<String, Integer> foodActualCounts = new HashMap<>();
        for (int i = 0; i < countSize; i++) {
            String itemId = buffer.readUtf();
            int count = buffer.readInt();
            foodActualCounts.put(itemId, count);
        }
        
        // 读取最后食用时间数据
        int timeSize = buffer.readInt();
        Map<String, Long> foodLastConsumedTime = new HashMap<>();
        for (int i = 0; i < timeSize; i++) {
            String itemId = buffer.readUtf();
            long time = buffer.readLong();
            foodLastConsumedTime.put(itemId, time);
        }
        
        // 读取当前游戏时间
        long currentGameTime = buffer.readLong();
        
        // 读取上次恢复时间
        long lastRecoveryTime = buffer.readLong();
        
        return new SyncFoodMarkersPacket(foodMarkers, foodActualCounts, foodLastConsumedTime, currentGameTime, lastRecoveryTime);
    }

    public static void handle(SyncFoodMarkersPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 在客户端更新食物标记信息、实际次数和时间数据
            ClientFoodTracker.updateFoodMarkers(packet.foodMarkers);
            ClientFoodTracker.updateFoodActualCounts(packet.foodActualCounts);
            ClientFoodTracker.updateFoodLastConsumedTime(packet.foodLastConsumedTime);
            ClientFoodTracker.updateCurrentGameTime(packet.currentGameTime);
            ClientFoodTracker.updateLastRecoveryTime(packet.lastRecoveryTime);
        });
        context.setPacketHandled(true);
    }

    public Map<String, Integer> getFoodMarkers() {
        return foodMarkers;
    }
    
    public Map<String, Integer> getFoodActualCounts() {
        return foodActualCounts;
    }
    
    public Map<String, Long> getFoodLastConsumedTime() {
        return foodLastConsumedTime;
    }
    
    public long getCurrentGameTime() {
        return currentGameTime;
    }
    
    public static void sendToPlayer(ServerPlayer player, Map<String, Integer> foodMarkers, 
                                     Map<String, Integer> foodActualCounts, 
                                     Map<String, Long> foodLastConsumedTime,
                                     long currentGameTime, long lastRecoveryTime) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
                    new SyncFoodMarkersPacket(foodMarkers, foodActualCounts, foodLastConsumedTime, currentGameTime, lastRecoveryTime));
    }
}