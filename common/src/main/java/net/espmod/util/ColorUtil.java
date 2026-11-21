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

    public static int lerpRgb(int c1, int c2, float t) {
        float k = Math.max(0.0f, Math.min(1.0f, t));
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = Math.round(r1 + (r2 - r1) * k);
        int g = Math.round(g1 + (g2 - g1) * k);
        int b = Math.round(b1 + (b2 - b1) * k);
        return (r << 16) | (g << 8) | b;
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
            return lerpRgb(0xFACC15, 0x34D399, (clamped - 0.6f) / 0.4f);
        } else if (clamped >= 0.3f) {
            return lerpRgb(0xF87171, 0xFACC15, (clamped - 0.3f) / 0.3f);
        } else {
            return 0xF87171;
        }
    }

    /** Returns monotonic animation seconds for smooth 3D rings, spirals, and HUD pulses. */
    public static double animSeconds() {
        return (System.nanoTime() * 1.0E-9);
    }
}
