package br.com.petrickfaria.jetpack.network;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.item.JetpackItem;
import br.com.petrickfaria.jetpack.player.JetpackEquipment;
import br.com.petrickfaria.jetpack.player.JetpackFlight;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;

public final class JetpackNetwork {
    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JetpackMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, FlightInput.class, FlightInput::encode, FlightInput::decode,
                FlightInput::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, EquipmentClick.class, EquipmentClick::encode, EquipmentClick::decode,
                EquipmentClick::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, EquipmentState.class, EquipmentState::encode, EquipmentState::decode,
                EquipmentState::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, FlightState.class, FlightState::encode, FlightState::decode,
                FlightState::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void syncEquipment(ServerPlayer player) {
        EquipmentState message = new EquipmentState(player.getId(), JetpackEquipment.get(player));
        sendToPlayer(player, message);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), message);
    }
    public static void syncEquipmentSelf(ServerPlayer player) {
        sendToPlayer(player, new EquipmentState(player.getId(), JetpackEquipment.get(player)));
    }
    public static void syncFlightState(ServerPlayer player, boolean active, double ceiling) {
        FlightState message = new FlightState(player.getId(), active, ceiling);
        sendToPlayer(player, message);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), message);
    }
    private static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID)
    public static final class Events {
        @SubscribeEvent public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) syncEquipment(player);
        }
        @SubscribeEvent public static void onStartTracking(PlayerEvent.StartTracking event) {
            if (event.getEntity() instanceof ServerPlayer watcher && event.getTarget() instanceof ServerPlayer target) {
                sendToPlayer(watcher, new EquipmentState(target.getId(), JetpackEquipment.get(target)));
                var state = target.getPersistentData().getCompound("jetpack_flight");
                var pack = JetpackEquipment.get(target);
                double ceiling = state.getDouble("baseY");
                if (pack.getItem() instanceof JetpackItem item) ceiling += item.maximumHeight();
                sendToPlayer(watcher, new FlightState(target.getId(), state.getBoolean("active"), ceiling));
            }
        }
    }

    public static final class FlightInput {
        private final boolean toggle, up, down;
        private final float strafe, forward;
        public FlightInput(boolean toggle, boolean up, boolean down, float strafe, float forward) {
            this.toggle = toggle; this.up = up; this.down = down; this.strafe = strafe; this.forward = forward;
        }
        private static void encode(FlightInput m, FriendlyByteBuf b) {
            b.writeBoolean(m.toggle); b.writeBoolean(m.up); b.writeBoolean(m.down); b.writeFloat(m.strafe); b.writeFloat(m.forward);
        }
        private static FlightInput decode(FriendlyByteBuf b) {
            return new FlightInput(b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readFloat(), b.readFloat());
        }
        private static void handle(FlightInput m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> { if (c.getSender() != null) JetpackFlight.handleInput(c.getSender(), m.toggle, m.up, m.down, m.strafe, m.forward); });
            c.setPacketHandled(true);
        }
    }

    public static final class EquipmentClick {
        private static void encode(EquipmentClick m, FriendlyByteBuf b) {}
        private static EquipmentClick decode(FriendlyByteBuf b) { return new EquipmentClick(); }
        private static void handle(EquipmentClick m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> {
                ServerPlayer p = c.getSender();
                if (p == null || !(p.containerMenu instanceof InventoryMenu)) return;
                var cursor = p.containerMenu.getCarried();
                if (!cursor.isEmpty() && !(cursor.getItem() instanceof JetpackItem)) return;
                var equipped = JetpackEquipment.get(p);
                JetpackEquipment.set(p, cursor);
                p.containerMenu.setCarried(equipped);
                p.containerMenu.broadcastChanges();
            });
            c.setPacketHandled(true);
        }
    }

    public static final class EquipmentState {
        private final int entityId;
        private final net.minecraft.world.item.ItemStack stack;
        private EquipmentState(int entityId, net.minecraft.world.item.ItemStack stack) { this.entityId = entityId; this.stack = stack.copy(); }
        private static void encode(EquipmentState m, FriendlyByteBuf b) { b.writeVarInt(m.entityId); b.writeItem(m.stack); }
        private static EquipmentState decode(FriendlyByteBuf b) { return new EquipmentState(b.readVarInt(), b.readItem()); }
        private static void handle(EquipmentState m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> br.com.petrickfaria.jetpack.client.JetpackClientState.setEquipment(m.entityId, m.stack)));
            c.setPacketHandled(true);
        }
    }

    public static final class FlightState {
        private final int entityId;
        private final boolean active;
        private final double ceiling;
        private FlightState(int entityId, boolean active, double ceiling) { this.entityId = entityId; this.active = active; this.ceiling = ceiling; }
        private static void encode(FlightState m, FriendlyByteBuf b) { b.writeVarInt(m.entityId); b.writeBoolean(m.active); b.writeDouble(m.ceiling); }
        private static FlightState decode(FriendlyByteBuf b) { return new FlightState(b.readVarInt(), b.readBoolean(), b.readDouble()); }
        private static void handle(FlightState m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> br.com.petrickfaria.jetpack.client.JetpackClientState.setFlight(m.entityId, m.active, m.ceiling)));
            c.setPacketHandled(true);
        }
    }
    private JetpackNetwork() {}
}
