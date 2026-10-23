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
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class JetpackNetwork {
    private static final String PROTOCOL = "6";
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
        CHANNEL.registerMessage(4, ShiftClickEquip.class, ShiftClickEquip::encode, ShiftClickEquip::decode,
                ShiftClickEquip::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5, OpenFuelFromInventory.class, OpenFuelFromInventory::encode, OpenFuelFromInventory::decode,
                OpenFuelFromInventory::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(6, ThrustSoundState.class, ThrustSoundState::encode, ThrustSoundState::decode,
                ThrustSoundState::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(7, ShiftClickUnequip.class, ShiftClickUnequip::encode, ShiftClickUnequip::decode,
                ShiftClickUnequip::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(8, OpenFuelFromEquipment.class, OpenFuelFromEquipment::encode, OpenFuelFromEquipment::decode,
                OpenFuelFromEquipment::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
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
    public static void syncThrustSound(ServerPlayer player, boolean playing) {
        ThrustSoundState message = new ThrustSoundState(player.getId(), playing);
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
                double ceiling = state.getDouble("ceiling");
                if (!state.contains("ceiling") && pack.getItem() instanceof JetpackItem item)
                    ceiling = state.getDouble("baseY") + item.maximumHeight();
                sendToPlayer(watcher, new FlightState(target.getId(), state.getBoolean("active"), ceiling));
                sendToPlayer(watcher, new ThrustSoundState(target.getId(), state.getBoolean("thrustSound")));
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
                ItemStack cursor = p.containerMenu.getCarried();
                if (!cursor.isEmpty() && !(cursor.getItem() instanceof JetpackItem)) return;
                ItemStack equipped = JetpackEquipment.get(p);
                if (cursor.isEmpty() && equipped.isEmpty()) return;
                JetpackEquipment.set(p, cursor);
                p.containerMenu.setCarried(equipped);
                p.containerMenu.broadcastChanges();
            });
            c.setPacketHandled(true);
        }
    }

    /** Moves a jetpack from a player inventory slot into the accessory slot, without replacing it. */
    public static final class ShiftClickEquip {
        private final int menuSlot;
        public ShiftClickEquip(int menuSlot) { this.menuSlot = menuSlot; }
        private static void encode(ShiftClickEquip m, FriendlyByteBuf b) { b.writeVarInt(m.menuSlot); }
        private static ShiftClickEquip decode(FriendlyByteBuf b) { return new ShiftClickEquip(b.readVarInt()); }
        private static void handle(ShiftClickEquip m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> {
                ServerPlayer p = c.getSender();
                if (p == null || !(p.containerMenu instanceof InventoryMenu) || !isPlayerInventorySlot(m.menuSlot)) return;
                if (!JetpackEquipment.get(p).isEmpty()) return;
                ItemStack source = inventoryStack(p, m.menuSlot);
                if (!(source.getItem() instanceof JetpackItem)) return;
                ItemStack moved = source.split(1);
                JetpackEquipment.set(p, moved);
                p.getInventory().setChanged();
                p.containerMenu.broadcastChanges();
            });
            c.setPacketHandled(true);
        }
    }

    /** Returns the equipped jetpack to the first free player inventory slot. */
    public static final class ShiftClickUnequip {
        private static void encode(ShiftClickUnequip m, FriendlyByteBuf b) {}
        private static ShiftClickUnequip decode(FriendlyByteBuf b) { return new ShiftClickUnequip(); }
        private static void handle(ShiftClickUnequip m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> {
                ServerPlayer p = c.getSender();
                if (p == null || !(p.containerMenu instanceof InventoryMenu)) return;
                ItemStack equipped = JetpackEquipment.get(p);
                if (!(equipped.getItem() instanceof JetpackItem)) return;
                for (int slot = 0; slot < p.getInventory().items.size(); slot++) {
                    if (p.getInventory().items.get(slot).isEmpty()) {
                        p.getInventory().setItem(slot, equipped.copy());
                        p.getInventory().setChanged();
                        JetpackEquipment.set(p, ItemStack.EMPTY);
                        p.containerMenu.broadcastChanges();
                        return;
                    }
                }
                // A full inventory leaves the original stack and its NBT equipped.
            });
            c.setPacketHandled(true);
        }
    }

    /** Opens a fuel menu for the exact jetpack clicked in the player's inventory. */
    public static final class OpenFuelFromInventory {
        private final int menuSlot;
        public OpenFuelFromInventory(int menuSlot) { this.menuSlot = menuSlot; }
        private static void encode(OpenFuelFromInventory m, FriendlyByteBuf b) { b.writeVarInt(m.menuSlot); }
        private static OpenFuelFromInventory decode(FriendlyByteBuf b) { return new OpenFuelFromInventory(b.readVarInt()); }
        private static void handle(OpenFuelFromInventory m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> {
                ServerPlayer p = c.getSender();
                if (p == null || !(p.containerMenu instanceof InventoryMenu) || !isPlayerInventorySlot(m.menuSlot)) return;
                ItemStack pack = inventoryStack(p, m.menuSlot);
                if (!(pack.getItem() instanceof JetpackItem)) return;
                UUID id = JetpackItem.getOrCreateInstanceId(pack);
                p.getInventory().setChanged();
                openFuelMenu(p, id, pack);
            });
            c.setPacketHandled(true);
        }
    }

    /** Opens the fuel interface for the exact stack stored in the accessory slot. */
    public static final class OpenFuelFromEquipment {
        private static void encode(OpenFuelFromEquipment m, FriendlyByteBuf b) {}
        private static OpenFuelFromEquipment decode(FriendlyByteBuf b) { return new OpenFuelFromEquipment(); }
        private static void handle(OpenFuelFromEquipment m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> {
                ServerPlayer p = c.getSender();
                if (p == null || !(p.containerMenu instanceof InventoryMenu)) return;
                ItemStack pack = JetpackEquipment.get(p);
                if (!(pack.getItem() instanceof JetpackItem)) return;
                UUID id = JetpackItem.getOrCreateInstanceId(pack);
                JetpackEquipment.set(p, pack); // Persist the ID on the equipped item before opening its menu.
                openFuelMenu(p, id, pack);
            });
            c.setPacketHandled(true);
        }
    }

    private static void openFuelMenu(ServerPlayer player, UUID id, ItemStack pack) {
        ItemStack snapshot = pack.copy();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inventory, user) -> new br.com.petrickfaria.jetpack.menu.JetpackFuelMenu(containerId, inventory, id),
                Component.literal("Abastecer Jetpack")), buf -> { buf.writeUUID(id); buf.writeItem(snapshot); });
    }

    private static boolean isPlayerInventorySlot(int menuSlot) { return menuSlot >= 9 && menuSlot <= 44; }
    private static ItemStack inventoryStack(ServerPlayer player, int menuSlot) {
        int inventoryIndex = menuSlot <= 35 ? menuSlot : menuSlot - 36;
        return player.getInventory().items.get(inventoryIndex);
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

    public static final class ThrustSoundState {
        private final int entityId;
        private final boolean playing;
        private ThrustSoundState(int entityId, boolean playing) { this.entityId = entityId; this.playing = playing; }
        private static void encode(ThrustSoundState m, FriendlyByteBuf b) { b.writeVarInt(m.entityId); b.writeBoolean(m.playing); }
        private static ThrustSoundState decode(FriendlyByteBuf b) { return new ThrustSoundState(b.readVarInt(), b.readBoolean()); }
        private static void handle(ThrustSoundState m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> br.com.petrickfaria.jetpack.client.JetpackClientSounds.setThrust(m.entityId, m.playing)));
            c.setPacketHandled(true);
        }
    }
    private JetpackNetwork() {}
}
