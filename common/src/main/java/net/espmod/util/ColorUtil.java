package net.espmod.util;

public final class ColorUtil {
    private ColorUtil() {}

    public static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    public static int withAlphaInt(int rgb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    public static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0f;
    }

    public static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0f;
    }

    public static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0f;
    }

    public static float computeDistanceAlpha(double distance, double maxDistance, boolean fadeEnabled) {
        if (!fadeEnabled || maxDistance <= 1.0) {
            return 1.0f;
        }
        double ratio = distance / maxDistance;
        if (ratio <= 0.65) {
            return 1.0f;
        }
        double t = 1.0 - ((ratio - 0.65) / 0.35);
        return (float) Math.max(0.28, Math.min(1.0, t));
    }

    public static int healthColor(float healthRatio) {
        float clamped = Math.max(0.0f, Math.min(1.0f, healthRatio));
        if (clamped >= 0.6f) {
            return 0x4ADE80; // Crisp emerald green
        } else if (clamped >= 0.3f) {
            return 0xFACC15; // Warm amber
        } else {
            return 0xF87171; // Coral red
        }
    }
}
