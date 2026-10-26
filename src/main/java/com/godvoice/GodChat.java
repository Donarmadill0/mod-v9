package com.godvoice;

import net.minecraft.client.MinecraftClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Receiving side. Dialogue travels as an ordinary private message that starts with a tag:
 *     [GV|D6A244|SIRIUS] Be safe.
 * A client with this mod hides the chat line and shows the dialogue box instead.
 * (A player WITHOUT the mod just sees the plain whisper.)
 */
public final class GodChat {
    private static final Pattern DIALOGUE = Pattern.compile("\\[GV\\|([0-9A-Fa-f]{6})\\|([^\\]|]{1,32})\\]\\s*(.+)", Pattern.DOTALL);
    private static final Pattern VOW = Pattern.compile("\\[GVOW\\|(swear|break)\\|([A-Za-z0-9_]{1,16})\\]");

    private static String lastKey = "";
    private static long lastTime = 0;

    private GodChat() {}

    /** @return true if the message was a god message (so the chat line should be hidden). */
    public static boolean handle(String raw) {
        if (raw == null) return false;
        Matcher v = VOW.matcher(raw);
        if (v.find()) {
            final String action = v.group(1);
            final String who = v.group(2);
            MinecraftClient.getInstance().execute(() -> onVow(action, who));
            return true;
        }
        Matcher m = DIALOGUE.matcher(raw);
        if (m.find()) {
            final int color = Integer.parseInt(m.group(1), 16);
            final String title = m.group(2).trim().toUpperCase();
            final String text = m.group(3).trim();
            final String key = title + "|" + text;
            long now = System.currentTimeMillis();
            if (key.equals(lastKey) && now - lastTime < 1500) return true; // echo of your own whisper
            lastKey = key;
            lastTime = now;
            MinecraftClient.getInstance().execute(() -> DialogueOverlay.push(title, color, text));
            return true;
        }
        return false;
    }

    private static void onVow(String action, String who) {
        GodConfig c = GodConfig.INSTANCE;
        if (action.equals("swear")) {
            if (!c.followers.contains(who)) c.followers.add(who);
            c.save();
            DialogueOverlay.push(c.name, c.color, who + " has sworn a vow to you.");
        } else {
            c.followers.remove(who);
            c.save();
            DialogueOverlay.push(c.name, c.color, who + " has broken their vow.");
        }
    }
}
