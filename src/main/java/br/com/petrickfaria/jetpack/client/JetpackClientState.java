package br.com.petrickfaria.jetpack.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/** Client-side snapshots received from the server for the local player and tracked players. */
public final class JetpackClientState {
    private static final Map<Integer, ItemStack> EQUIPMENT = new HashMap<>();
    private static final Map<Integer, FlightState> FLIGHT = new HashMap<>();

    public static void setEquipment(int entityId, ItemStack stack) {
        if (stack.isEmpty()) EQUIPMENT.remove(entityId);
        else EQUIPMENT.put(entityId, stack.copy());
    }
    public static ItemStack getEquipment(int entityId) {
        ItemStack stack = EQUIPMENT.get(entityId);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }
    public static void setFlight(int entityId, boolean active, double ceiling) {
        if (active) FLIGHT.put(entityId, new FlightState(true, ceiling));
        else FLIGHT.remove(entityId);
    }
    public static FlightState getFlight(int entityId) { return FLIGHT.getOrDefault(entityId, FlightState.OFF); }
    public static void remove(int entityId) { EQUIPMENT.remove(entityId); FLIGHT.remove(entityId); }
    public static void clear() { EQUIPMENT.clear(); FLIGHT.clear(); }

    public record FlightState(boolean active, double ceiling) {
        private static final FlightState OFF = new FlightState(false, 0.0D);
    }
    private JetpackClientState() {}
}
