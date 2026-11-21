package net.espmod.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class EspHudStats {
    private static volatile int trackedEntities = 0;
    private static volatile int trackedBlocks = 0;

    // Smoothly interpolated health ratio per entity id for fluid Target HUD & vertical bars
    private static final Map<Integer, Float> SMOOTH_HP = new HashMap<>();
    private static long lastFrameNanos = System.nanoTime();
    private static float frameDt = 0.016f;

    private EspHudStats() {}

    public static void update(int entities, int blocks) {
        trackedEntities = Math.max(0, entities);
        trackedBlocks = Math.max(0, blocks);
    }

    public static void beginHudFrame() {
        long now = System.nanoTime();
        long diff = now - lastFrameNanos;
        lastFrameNanos = now;
        frameDt = Math.max(0.001f, Math.min(0.1f, diff * 1.0E-9f));
        if (SMOOTH_HP.size() > 512) {
            Iterator<Map.Entry<Integer, Float>> it = SMOOTH_HP.entrySet().iterator();
            for (int i = 0; i < 128 && it.hasNext(); i++) {
                it.next();
                it.remove();
            }
        }
    }

    public static float smoothHealth(int entityId, float targetRatio) {
        float clamped = Math.max(0.0f, Math.min(1.0f, targetRatio));
        Float prev = SMOOTH_HP.get(entityId);
        if (prev == null) {
            SMOOTH_HP.put(entityId, clamped);
            return clamped;
        }
        float speed = Math.min(1.0f, frameDt * 10.0f);
        float next = prev + (clamped - prev) * speed;
        if (Math.abs(clamped - next) < 0.002f) {
            next = clamped;
        }
        SMOOTH_HP.put(entityId, next);
        return next;
    }

    public static int getTrackedEntities() {
        return trackedEntities;
    }

    public static int getTrackedBlocks() {
        return trackedBlocks;
    }

    public static int getTotalTargets() {
        return trackedEntities + trackedBlocks;
    }
}
