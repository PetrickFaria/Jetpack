package br.com.petrickfaria.jetpack.network;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.player.JetpackFlight;
import br.com.petrickfaria.jetpack.player.JetpackEquipment;
import br.com.petrickfaria.jetpack.item.JetpackItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;

public final class JetpackNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JetpackMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, FlightInput.class, FlightInput::encode, FlightInput::decode,
                FlightInput::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, EquipmentClick.class, EquipmentClick::encode, EquipmentClick::decode,
                EquipmentClick::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, EquipmentState.class, EquipmentState::encode, EquipmentState::decode,
                EquipmentState::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void syncEquipment(ServerPlayer player) {
        CHANNEL.sendTo(new EquipmentState(JetpackEquipment.get(player)), player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT);
    }

    @Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID)
    public static final class Events {
        @SubscribeEvent public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) syncEquipment(player);
        }
    }

    public static final class FlightInput {
        private final boolean toggle;
        private final boolean up;
        private final boolean down;
        public FlightInput(boolean toggle, boolean up, boolean down) {
            this.toggle = toggle; this.up = up; this.down = down;
        }
        private static void encode(FlightInput msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.toggle); buf.writeBoolean(msg.up); buf.writeBoolean(msg.down);
        }
        private static FlightInput decode(FriendlyByteBuf buf) {
            return new FlightInput(buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
        }
        private static void handle(FlightInput msg, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                if (context.getSender() != null)
                    JetpackFlight.handleInput(context.getSender(), msg.toggle, msg.up, msg.down);
            });
            context.setPacketHandled(true);
        }
    }

    public static final class EquipmentClick {
        private static void encode(EquipmentClick msg, FriendlyByteBuf buf) {}
        private static EquipmentClick decode(FriendlyByteBuf buf) { return new EquipmentClick(); }
        private static void handle(EquipmentClick msg, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !(player.containerMenu instanceof InventoryMenu)) return;
                var cursor = player.containerMenu.getCarried();
                if (!cursor.isEmpty() && !(cursor.getItem() instanceof JetpackItem)) return;
                var equipped = JetpackEquipment.get(player);
                JetpackEquipment.set(player, cursor);
                player.containerMenu.setCarried(equipped);
                player.containerMenu.broadcastChanges();
                syncEquipment(player);
            });
            context.setPacketHandled(true);
        }
    }

    public static final class EquipmentState {
        private final net.minecraft.world.item.ItemStack stack;
        private EquipmentState(net.minecraft.world.item.ItemStack stack) { this.stack = stack.copy(); }
        private static void encode(EquipmentState msg, FriendlyByteBuf buf) { buf.writeItem(msg.stack); }
        private static EquipmentState decode(FriendlyByteBuf buf) { return new EquipmentState(buf.readItem()); }
        private static void handle(EquipmentState msg, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> br.com.petrickfaria.jetpack.client.JetpackClientState.setEquipment(msg.stack)));
            context.setPacketHandled(true);
        }
    }

    private JetpackNetwork() {}
}
