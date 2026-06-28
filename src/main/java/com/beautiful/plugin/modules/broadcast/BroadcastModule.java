package com.beautiful.plugin.modules.broadcast;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.modules.AbstractESUModule;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.api.IRunTask;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ColorHelper;
import top.mrxiaom.pluginbase.utils.depend.PAPI;

import java.util.*;

@AutoRegister
public class BroadcastModule extends AbstractESUModule {

    private boolean enable = false;
    private String type = "random";
    private int every = 30;
    private final List<BroadcastEntry> entries = new ArrayList<>();
    private int index = 0;
    private int lastIndex = -1;
    private IRunTask task;
    private final Random random = new Random();

    public BroadcastModule(ESUPlugin plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        stopTask();
        entries.clear();
        index = 0;
        lastIndex = -1;

        enable = config.getBoolean("auto-broadcast.enable", false);
        if (!enable) return;

        type = config.getString("auto-broadcast.type", "random");
        every = config.getInt("auto-broadcast.every", 30);

        ConfigurationSection broadcastSection = config.getConfigurationSection("auto-broadcast.broadcast");
        if (broadcastSection == null) return;

        for (String key : broadcastSection.getKeys(false)) {
            List<String> lines = broadcastSection.getStringList(key);
            if (!lines.isEmpty()) {
                entries.add(new BroadcastEntry(key, lines));
            }
        }

        if (entries.isEmpty()) return;

        long ticks = every * 20L;
        task = plugin.getScheduler().runTaskTimer(this::broadcast, ticks, ticks);
    }

    private void broadcast() {
        if (entries.isEmpty()) return;

        BroadcastEntry entry;
        if ("random".equalsIgnoreCase(type)) {
            if (entries.size() == 1) {
                entry = entries.get(0);
            } else {
                int next;
                do {
                    next = random.nextInt(entries.size());
                } while (next == lastIndex);
                lastIndex = next;
                entry = entries.get(next);
            }
        } else {
            entry = entries.get(index % entries.size());
            index++;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String line : entry.lines()) {
                ColorHelper.parseAndSend(player, PAPI.setPlaceholders(player, line));
            }
        }
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private record BroadcastEntry(String title, List<String> lines) {}
}
