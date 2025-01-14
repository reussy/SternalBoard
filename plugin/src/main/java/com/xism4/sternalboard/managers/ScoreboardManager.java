package com.xism4.sternalboard.managers;

import com.xism4.sternalboard.Scoreboards;
import com.xism4.sternalboard.SternalBoard;
import com.xism4.sternalboard.SternalBoardHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ScoreboardManager {

    private final SternalBoard core;
    private static final ConcurrentMap<UUID, SternalBoardHandler> BOARDS_HANDLER = new ConcurrentHashMap<>();
    private Integer updateTask;

    public ScoreboardManager(SternalBoard core) {
        this.core = core;
    }

    public ConcurrentMap<UUID, SternalBoardHandler> getBoardsHandler() {
        return BOARDS_HANDLER;
    }

    public void init() {
        FileConfiguration config = core.getConfig();
        String scoreboardMode = config.getString("settings.mode", "NORMAL")
                .toUpperCase(Locale.ROOT);
        String intervalUpdatePath = "scoreboard-interval-update";
        int updateTime;

        if (config.getInt(intervalUpdatePath) <= 0) {
            config.set(intervalUpdatePath, 20);
            core.saveConfig();
        }

        updateTime = config.getInt(intervalUpdatePath);

        if (core.isAnimationEnabled()) {
            return;
        }

        ConfigurationSection defaultSection = core.getConfig().getConfigurationSection("settings.scoreboard");
        updateTask = core.getServer().getScheduler().runTaskTimerAsynchronously(core, () -> {
            for (SternalBoardHandler board : BOARDS_HANDLER.values()) {
                switch (scoreboardMode) {
                    case "WORLD":
                        processWorldScoreboard(board);
                        return;
                    case "PERMISSION":
                        processPermissionScoreboard(board);
                        return;
                    case "NORMAL":
                    default:
                        Scoreboards.updateFromSection(board, defaultSection);
                        return;
                }
            }
        }, 0, updateTime).getTaskId();
    }

    public void setScoreboard(Player player) {
        SternalBoardHandler handler = new SternalBoardHandler(player);
        FileConfiguration config = core.getConfig();
        ConfigurationSection defaultSection = core.getConfig().getConfigurationSection("settings.scoreboard");

        if (core.isAnimationEnabled() && config.getInt("settings.scoreboard.update") != 0)
            return;

        Scoreboards.updateFromSection(handler, defaultSection);
        BOARDS_HANDLER.put(player.getUniqueId(), handler);
    }

    public void removeScoreboard(Player player) {
        SternalBoardHandler board = BOARDS_HANDLER.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    public void reload() {
        if (updateTask != null) {
            Bukkit.getServer().getScheduler().cancelTask(updateTask);
        }

        // TODO: 30/11/2022 view this condition, is it necessary?
        if (core.isAnimationEnabled() && updateTask != null) {
            for (SternalBoardHandler board : this.BOARDS_HANDLER.values()) {
                board.updateLines("");
            }
        }
        init();
    }

    public void toggle(Player player) {
        SternalBoardHandler oldBoard = BOARDS_HANDLER.remove(player.getUniqueId());
        if (oldBoard != null) {
            oldBoard.delete();
        } else {
            setScoreboard(player);
        }
    }

    private void processWorldScoreboard(SternalBoardHandler handler) {
        String worldName = handler.getPlayer().getWorld().getName();
        ConfigurationSection worldSection = core.getConfig().getConfigurationSection("scoreboard-world." + worldName);

        if (worldSection == null) {
            Scoreboards.updateFromSection(handler, core.getConfig().getConfigurationSection("settings.scoreboard"));
            return;
        }

        Scoreboards.updateFromSection(handler, worldSection);
    }

    private void processPermissionScoreboard(SternalBoardHandler handler) {
        Set<String> permissions = Objects.requireNonNull(core.getConfig().getConfigurationSection("scoreboard-permission"))
                .getKeys(true);

        String nodeSection;
        ConfigurationSection permissionSection = null;
        for (String key : permissions) {
            nodeSection = core.getConfig().getString("scoreboard-permission." + key + ".node");
            if (nodeSection == null) continue;
            if (handler.getPlayer().hasPermission(nodeSection)) {
                permissionSection = core.getConfig().getConfigurationSection("scoreboard-permission." + key);
                break;
            }
        }

        if (permissionSection == null) {
            Scoreboards.updateFromSection(handler, core.getConfig().getConfigurationSection("settings.scoreboard"));
            return;
        }

        Scoreboards.updateFromSection(handler, permissionSection);
    }

    public static SternalBoardHandler fromUUID(String id) {
        return BOARDS_HANDLER.get(UUID.fromString(id));
    }

    public static void updateHandler(SternalBoardHandler handler) {
        if (handler == null) {
            throw new NullPointerException("The handler of sternal boards is null");
        }
        BOARDS_HANDLER.put(handler.getPlayer().getUniqueId(), handler);
    }
}
