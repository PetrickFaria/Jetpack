package br.com.petrickfaria.jetpack.network;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.player.JetpackFlight;
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

    private JetpackNetwork() {}
}
