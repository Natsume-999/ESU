package com.beautiful.plugin.modules.packet;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.modules.AbstractESUModule;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.configuration.MemoryConfiguration;
import top.mrxiaom.pluginbase.func.AutoRegister;

@AutoRegister(requirePlugins = {"packetevents"})
public class PacketModule extends AbstractESUModule {

    private PacketListenerCommon listener;

    public PacketModule(ESUPlugin plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        unregisterListener();
        if (!config.getBoolean("handle-network-listener.enable", true)) return;

        PacketConfig packetConfig = new PacketConfig(
                config.getBoolean("handle-network-listener.damage-indicator", true),
                config.getStringList("handle-network-listener.chat-network-content"),
                config.getString("handle-network-listener.chat-replay-popup", "")
        );
        listener = PacketEvents.getAPI().getEventManager().registerListener(
                new ESUPacketListener(packetConfig, plugin), PacketListenerPriority.LOW
        );
    }

    private void unregisterListener() {
        if (listener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            listener = null;
        }
    }

    @Override
    public void onDisable() {
        unregisterListener();
    }
}
