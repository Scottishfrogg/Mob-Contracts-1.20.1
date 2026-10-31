package net.scottishfrogg.mobcontracts.client;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ClientNotifications {
    public static final int DISPLAY_TICKS = 70;
    public static final int FADE_TICKS = 10;
    private static final Deque<Component> QUEUE = new ArrayDeque<>();
    @Nullable private static Component current;
    private static int remaining;
    private ClientNotifications() {}
    public static void push(Component message) {
        QUEUE.add(message);
    }
    public static void clear() {
        QUEUE.clear();
        current = null;
    }
    public static void tick() {
        if (current != null && --remaining <= 0) current = null;
        if (current == null && !QUEUE.isEmpty()) {
            current = QUEUE.poll();
            remaining = DISPLAY_TICKS;
        }
    }
    @Nullable
    public static Component getCurrent() {
        return current;
    }
    public static int getAlpha(float partialTick) {
        float left = remaining - partialTick;
        float age = DISPLAY_TICKS - left;
        float f = Math.min(1f, Math.min(left, age) / FADE_TICKS);
        return Math.max(0, (int) (f * 255));
    }
}
