package com.godvoice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Your god settings, saved in .minecraft/config/godvoice.json */
public final class GodConfig {
    public String name = "GOD";
    public int color = 0xD6A244;
    /** players who follow YOU */
    public List<String> followers = new ArrayList<>();
    /** gods YOU serve */
    public List<String> serving = new ArrayList<>();

    public static final GodConfig INSTANCE = load();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("godvoice.json");
    }

    private static GodConfig load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                GodConfig c = new Gson().fromJson(Files.readString(p), GodConfig.class);
                if (c != null) {
                    if (c.name == null || c.name.isBlank()) c.name = "GOD";
                    if (c.followers == null) c.followers = new ArrayList<>();
                    if (c.serving == null) c.serving = new ArrayList<>();
                    return c;
                }
            }
        } catch (Exception ignored) {
        }
        return new GodConfig();
    }

    public void save() {
        try {
            Files.writeString(path(), new GsonBuilder().setPrettyPrinting().create().toJson(this));
        } catch (IOException ignored) {
        }
    }
}
