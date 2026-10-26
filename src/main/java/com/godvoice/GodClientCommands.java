package com.godvoice;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** All commands run on YOUR client. Nothing needs to be installed on the server. */
public final class GodClientCommands {
    private GodClientCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(literal("god")
                .executes(ctx -> help(ctx))
                .then(literal("name").then(argument("name", StringArgumentType.greedyString()).executes(GodClientCommands::setName)))
                .then(literal("color").then(argument("hex", StringArgumentType.greedyString()).executes(GodClientCommands::setColor)))
                .then(literal("speak")
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("message", StringArgumentType.greedyString()).executes(GodClientCommands::speak))))
                .then(literal("broadcast")
                        .then(argument("message", StringArgumentType.greedyString()).executes(GodClientCommands::broadcast)))
                .then(literal("preview")
                        .then(argument("message", StringArgumentType.greedyString()).executes(GodClientCommands::preview)))
                .then(literal("follower")
                        .then(literal("add").then(argument("player", StringArgumentType.word()).executes(GodClientCommands::followerAdd)))
                        .then(literal("remove").then(argument("player", StringArgumentType.word()).executes(GodClientCommands::followerRemove))))
                .then(literal("followers").executes(GodClientCommands::followers))
                .then(literal("info").executes(GodClientCommands::info)));

        d.register(literal("vow")
                .then(literal("swear").then(argument("god", StringArgumentType.word()).executes(GodClientCommands::swear)))
                .then(literal("break").then(argument("god", StringArgumentType.word()).executes(GodClientCommands::breakVow)))
                .then(literal("list").executes(GodClientCommands::listVows)));
    }

    // ---------- helpers ----------

    private static void say(CommandContext<FabricClientCommandSource> ctx, String msg) {
        ctx.getSource().sendFeedback(Text.literal(msg));
    }

    private static void err(CommandContext<FabricClientCommandSource> ctx, String msg) {
        ctx.getSource().sendError(Text.literal(msg));
    }

    private static String tag() {
        GodConfig c = GodConfig.INSTANCE;
        String clean = c.name.replace('|', ' ').replace(']', ' ').replace('[', ' ');
        return String.format("[GV|%06X|%s] ", c.color & 0xFFFFFF, clean);
    }

    private static boolean whisper(String player, String text) {
        ClientPlayNetworkHandler h = MinecraftClient.getInstance().getNetworkHandler();
        if (h == null) return false;
        h.sendChatCommand("msg " + player + " " + text);
        return true;
    }

    private static String clip(String s) {
        return s.length() > 200 ? s.substring(0, 200) : s;
    }

    // ---------- /god ----------

    private static int help(CommandContext<FabricClientCommandSource> ctx) {
        say(ctx, "God commands (client side):");
        say(ctx, "/god name <name>        pick your god name");
        say(ctx, "/god color <#hex>       pick your colour");
        say(ctx, "/god speak <player> <message>");
        say(ctx, "/god broadcast <message>   (to your followers)");
        say(ctx, "/god follower add|remove <player>, /god followers");
        say(ctx, "/god preview <message>  see how it looks on your screen");
        say(ctx, "Players: /vow swear <god>, /vow break <god>, /vow list");
        return 1;
    }

    private static int setName(CommandContext<FabricClientCommandSource> ctx) {
        String n = StringArgumentType.getString(ctx, "name").trim().toUpperCase()
                .replace("|", "").replace("]", "").replace("[", "");
        if (n.isEmpty()) {
            err(ctx, "Pick a name.");
            return 0;
        }
        GodConfig.INSTANCE.name = n.length() > 32 ? n.substring(0, 32) : n;
        GodConfig.INSTANCE.save();
        say(ctx, "You are now " + GodConfig.INSTANCE.name + ".");
        return 1;
    }

    private static int setColor(CommandContext<FabricClientCommandSource> ctx) {
        String h = StringArgumentType.getString(ctx, "hex").trim();
        if (h.startsWith("#")) h = h.substring(1);
        try {
            GodConfig.INSTANCE.color = Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            err(ctx, "Colour must be hex, like #D6A244");
            return 0;
        }
        GodConfig.INSTANCE.save();
        say(ctx, "Colour set to #" + String.format("%06X", GodConfig.INSTANCE.color) + ".");
        return 1;
    }

    private static int speak(CommandContext<FabricClientCommandSource> ctx) {
        String p = StringArgumentType.getString(ctx, "player");
        String m = clip(StringArgumentType.getString(ctx, "message"));
        if (!whisper(p, tag() + m)) {
            err(ctx, "Not connected to a server.");
            return 0;
        }
        return 1;
    }

    private static int broadcast(CommandContext<FabricClientCommandSource> ctx) {
        String m = clip(StringArgumentType.getString(ctx, "message"));
        GodConfig c = GodConfig.INSTANCE;
        if (c.followers.isEmpty()) {
            err(ctx, "You have no followers yet. Add some with /god follower add <player>.");
            return 0;
        }
        for (String f : c.followers) whisper(f, tag() + m);
        say(ctx, "Spoke to " + c.followers.size() + " follower(s).");
        return 1;
    }

    private static int preview(CommandContext<FabricClientCommandSource> ctx) {
        GodConfig c = GodConfig.INSTANCE;
        DialogueOverlay.push(c.name, c.color, StringArgumentType.getString(ctx, "message"));
        return 1;
    }

    private static int followerAdd(CommandContext<FabricClientCommandSource> ctx) {
        String p = StringArgumentType.getString(ctx, "player");
        if (!GodConfig.INSTANCE.followers.contains(p)) GodConfig.INSTANCE.followers.add(p);
        GodConfig.INSTANCE.save();
        say(ctx, p + " is now your follower.");
        return 1;
    }

    private static int followerRemove(CommandContext<FabricClientCommandSource> ctx) {
        String p = StringArgumentType.getString(ctx, "player");
        GodConfig.INSTANCE.followers.remove(p);
        GodConfig.INSTANCE.save();
        say(ctx, p + " removed.");
        return 1;
    }

    private static int followers(CommandContext<FabricClientCommandSource> ctx) {
        GodConfig c = GodConfig.INSTANCE;
        say(ctx, c.followers.isEmpty() ? "No followers yet." : "Followers: " + String.join(", ", c.followers));
        return 1;
    }

    private static int info(CommandContext<FabricClientCommandSource> ctx) {
        GodConfig c = GodConfig.INSTANCE;
        say(ctx, "You are " + c.name + " (#" + String.format("%06X", c.color) + "), " + c.followers.size() + " follower(s).");
        return 1;
    }

    // ---------- /vow ----------

    private static String me() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }

    private static int swear(CommandContext<FabricClientCommandSource> ctx) {
        String god = StringArgumentType.getString(ctx, "god");
        if (!whisper(god, "[GVOW|swear|" + me() + "]")) {
            err(ctx, "Not connected to a server.");
            return 0;
        }
        if (!GodConfig.INSTANCE.serving.contains(god)) GodConfig.INSTANCE.serving.add(god);
        GodConfig.INSTANCE.save();
        say(ctx, "You swore a vow to " + god + ".");
        return 1;
    }

    private static int breakVow(CommandContext<FabricClientCommandSource> ctx) {
        String god = StringArgumentType.getString(ctx, "god");
        whisper(god, "[GVOW|break|" + me() + "]");
        GodConfig.INSTANCE.serving.remove(god);
        GodConfig.INSTANCE.save();
        say(ctx, "Your vow to " + god + " is broken.");
        return 1;
    }

    private static int listVows(CommandContext<FabricClientCommandSource> ctx) {
        GodConfig c = GodConfig.INSTANCE;
        say(ctx, c.serving.isEmpty() ? "You serve no one." : "You serve: " + String.join(", ", c.serving));
        return 1;
    }
}
