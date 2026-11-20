package net.espmod.render;

public final class EspHudStats {
    private static volatile int trackedEntities = 0;
    private static volatile int trackedBlocks = 0;

    private EspHudStats() {}

    public static void update(int entities, int blocks) {
        trackedEntities = Math.max(0, entities);
        trackedBlocks = Math.max(0, blocks);
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
