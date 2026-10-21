package br.com.petrickfaria.jetpack.client;

/** Hit-test helper for the vanilla player inventory grid; contains no client rendering dependencies. */
public final class JetpackInventorySlots {
    public static int menuSlotAt(int guiWidth, int guiHeight, double mouseX, double mouseY) {
        int left = (guiWidth - 176) / 2;
        int top = (guiHeight - 166) / 2;
        int column = (int) Math.floor((mouseX - left - 8) / 18.0D);
        if (column < 0 || column >= 9) return -1;
        double relativeY = mouseY - top - 84;
        if (relativeY >= 0.0D && relativeY < 54.0D) {
            int row = (int) Math.floor(relativeY / 18.0D);
            return 9 + row * 9 + column;
        }
        relativeY = mouseY - top - 142;
        if (relativeY >= 0.0D && relativeY < 18.0D) return 36 + column;
        return -1;
    }

    private JetpackInventorySlots() {}
}
