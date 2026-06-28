package com.beautiful.plugin.modules.dialog;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.bridge.BetterHudBridge;
import com.beautiful.plugin.bridge.ChemdahBridge;
import com.beautiful.plugin.modules.AbstractESUModule;
import org.bukkit.Location;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;

import java.util.*;

@AutoRegister(requirePlugins = {"BetterHud"})
public class DialogModule extends AbstractESUModule implements Listener {

    private boolean enable = false;
    private List<String> hideList = List.of();
    private String themeName = "";
    private int speed = 1;
    private int delay = 20;
    private int distance = 2;
    private int size = 4;
    private String titleFormat = "{title}";
    private String replySelect = "<white>{player_side}";
    private String replyDefault = "<gray>{player_side}";

    private final Map<UUID, PlayerSessionData> sessions = new HashMap<>();
    private final boolean chemdahAvailable;

    public DialogModule(ESUPlugin plugin) {
        super(plugin);
        chemdahAvailable = Util.isPresent("ink.ptms.chemdah.api.ChemdahAPI");
        registerEvents();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        enable = config.getBoolean("type-writer.enable", false);
        if (!enable) return;

        hideList = config.getStringList("type-writer.hide");
        themeName = config.getString("type-writer.theme.name", "theme_hud");
        speed = config.getInt("type-writer.theme.speed", 1);
        delay = config.getInt("type-writer.theme.delay", 20);
        distance = config.getInt("type-writer.theme.distance", 2);
        size = config.getInt("type-writer.theme.size", 4);
        titleFormat = config.getString("type-writer.theme.format.title", "{title}");
        replySelect = config.getString("type-writer.theme.format.reply.select", "<white>{player_side}");
        replyDefault = config.getString("type-writer.theme.format.reply.default", "<gray>{player_side}");
    }

    public boolean isEnable() { return enable; }
    public List<String> getHideList() { return hideList; }
    public String getThemeName() { return themeName; }
    public int getSpeed() { return speed; }
    public int getDelay() { return delay; }
    public int getDistance() { return distance; }
    public int getSize() { return size; }
    public String getTitleFormat() { return titleFormat; }
    public String getReplySelect() { return replySelect; }
    public String getReplyDefault() { return replyDefault; }
    public PlayerSessionData getSession(UUID uuid) { return sessions.get(uuid); }

    public void startDialog(Player player) {
        if (!enable) return;
        UUID uuid = player.getUniqueId();
        PlayerSessionData data = sessions.computeIfAbsent(uuid, k -> new PlayerSessionData());
        data.inDialog = true;
        data.selectedReply = 0;
        data.dialogLocation = player.getLocation();
        for (String hudName : hideList) {
            BetterHudBridge.hidePopup(hudName, uuid);
        }
    }

    public void endDialog(Player player) {
        if (!enable) return;
        UUID uuid = player.getUniqueId();
        PlayerSessionData data = sessions.get(uuid);
        if (data == null || !data.inDialog) return;
        data.inDialog = false;
        for (String hudName : hideList) {
            BetterHudBridge.showPopup(hudName, uuid);
        }
    }

    public boolean isInDialog(UUID uuid) {
        PlayerSessionData data = sessions.get(uuid);
        return data != null && data.inDialog;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (enable) {
            sessions.put(event.getPlayer().getUniqueId(), new PlayerSessionData());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enable || distance <= 0) return;
        Player player = event.getPlayer();
        PlayerSessionData data = sessions.get(player.getUniqueId());
        if (data == null || !data.inDialog || data.dialogLocation == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (!data.dialogLocation.getWorld().equals(to.getWorld())
                || data.dialogLocation.distanceSquared(to) > (double) distance * distance) {
            endDialog(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enable || !chemdahAvailable) return;
        Player player = event.getPlayer();
        if (ChemdahBridge.isInConversation(player)) {
            plugin.getScheduler().runTask(() -> startDialog(player));
        }
    }

    public static class PlayerSessionData {
        boolean inDialog = false;
        int selectedReply = 0;
        Location dialogLocation = null;
    }

    public static DialogModule inst() {
        return instanceOf(DialogModule.class);
    }
}
