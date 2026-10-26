package com.godvoice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * God dialogue HUD, styled like the "SIRIUS" reference:
 *   - small coloured god name, a thin line of the same colour under it
 *   - plain white text, with a soft fade that reaches exactly to the bar on the right, fading out to
 *     fully transparent back toward the crosshair - same reach every time, whatever the message's length
 *   - sits just right of the crosshair, with a thin vertical bar at the far right edge of the screen
 * Fixed to the screen (does not follow the camera).
 */
public final class DialogueOverlay {
    private record Line(String title, int color, String message) {}

    // ---- tweak these ----
    private static final float SCALE = 1.0f;          // text size (1.0 = normal Minecraft text)
    private static final float LEFT_FROM_CENTER = 0.127f; // how far right of the crosshair the text starts (fraction of screen width)
    private static final float TEXT_WIDTH = 0.22f;    // text/underline width (fraction of screen width)
    private static final int TOP_FROM_CENTER = -10;   // vertical start relative to the crosshair (negative = above it)
    private static final int BAR_MARGIN = 9;          // gap from the right screen edge to the vertical bar
    private static final float BG_ALPHA = 0.5f;       // strength of the fade at its darkest (right) edge. 0 = no fade at all.
    // ---------------------

    private static final long MS_PER_CHAR = 35;
    private static final long FADE_IN = 250;
    private static final long FADE_OUT = 600;

    private static final Deque<Line> queue = new ArrayDeque<>();
    private static Line current;
    private static List<String> lines = new ArrayList<>();
    private static int totalChars;
    private static int lineW = 60;
    private static long startMs;

    private DialogueOverlay() {}

    public static void push(String title, int color, String message) {
        queue.add(new Line(title, color, message));
    }

    private static long holdMs() {
        return 2200 + totalChars * 45L;
    }

    private static long totalMs() {
        return totalChars * MS_PER_CHAR + holdMs() + FADE_OUT;
    }

    private static int textWidthPx(MinecraftClient mc) {
        return Math.max(110, (int) (mc.getWindow().getScaledWidth() * TEXT_WIDTH));
    }

    private static void start(Line next, MinecraftClient mc, long now) {
        TextRenderer font = mc.textRenderer;
        current = next;
        startMs = now;
        lines = new ArrayList<>();
        totalChars = 0;
        int capPx = (int) (textWidthPx(mc) / SCALE);
        for (StringVisitable v : font.getTextHandler().wrapLines(next.message(), capPx, Style.EMPTY)) {
            String s = v.getString();
            lines.add(s);
            totalChars += s.length();
        }
        int widest = font.getWidth(next.title());
        for (String s : lines) widest = Math.max(widest, font.getWidth(s));
        // the line under the name, and the fade, are as wide as the text, plus a little -
        // capped so a long word with no spaces (which can't wrap) can't stretch them across the screen
        lineW = Math.min(Math.max(60, widest + 8), capPx + 16);
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer font = mc.textRenderer;
        long now = System.currentTimeMillis();

        if (current != null && now - startMs > totalMs()) current = null;
        if (current == null) {
            Line next = queue.poll();
            if (next == null) return;
            start(next, mc, now);
        }

        long el = now - startMs;
        float alpha = 1f;
        if (el < FADE_IN) alpha = el / (float) FADE_IN;
        long fadeStart = totalMs() - FADE_OUT;
        if (el > fadeStart) alpha = 1f - (el - fadeStart) / (float) FADE_OUT;
        alpha = Math.max(0f, Math.min(1f, alpha));
        if (alpha < 0.03f) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int lineH = font.fontHeight + 1;
        float x0 = sw / 2f + sw * LEFT_FROM_CENTER;
        float y0 = sh / 2f + TOP_FROM_CENTER;
        int bodyTop = 19;
        int localH = bodyTop + lines.size() * lineH + 4;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x0, y0);
        ctx.getMatrices().scale(SCALE, SCALE);

        if (BG_ALPHA > 0f) {
            // Fade spans from right where the bar sits, fading out to fully transparent back toward the
            // crosshair - same reach every time, whether the message is one word or several lines.
            float barX = sw - BAR_MARGIN;
            float fullW = Math.max(40f, (barX - x0) / SCALE);
            int bands = 40;
            float[] xs = new float[bands + 1];
            for (int i = 0; i <= bands; i++) xs[i] = -8 + fullW * (i / (float) bands);
            for (int i = 0; i < bands; i++) {
                int x1 = Math.round(xs[i]);
                int x2 = Math.max(x1 + 1, Math.round(xs[i + 1]));
                float t = (i + 0.5f) / bands;     // 0 near the crosshair, 1 near the right edge of the box
                float fa = alpha * BG_ALPHA * t;  // transparent on the left, solid on the right
                ctx.fill(x1, -4, x2, localH, argb(fa, 0x000000));
            }
        }

        // god name (in the god's colour) + thin line under it
        ctx.drawText(font, current.title(), 0, 0, argb(alpha, current.color()), true);
        ctx.fill(0, 15, lineW, 16, argb(alpha, current.color()));

        // typewriter body, plain white
        int remaining = (int) Math.min(totalChars, el / MS_PER_CHAR);
        int ty = bodyTop;
        for (String s : lines) {
            if (remaining <= 0) break;
            String part = s.substring(0, Math.min(s.length(), remaining));
            remaining -= part.length();
            ctx.drawText(font, part, 0, ty, argb(alpha, 0xFFFFFF), true);
            ty += lineH;
        }
        ctx.getMatrices().popMatrix();

        // thin vertical bar at the far right edge of the screen
        int bx = sw - BAR_MARGIN;
        int by0 = (int) (y0 - 15 * SCALE);                                        // starts above the name
        int by1 = (int) (y0 + (bodyTop + lines.size() * lineH + 22) * SCALE);    // runs well below the text
        ctx.fill(bx, by0, bx + 2, by1, argb(alpha, current.color()));
    }
}
