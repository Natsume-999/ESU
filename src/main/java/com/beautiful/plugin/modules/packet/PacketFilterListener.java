package com.beautiful.plugin.modules.packet;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.bridge.BetterHudBridge;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PacketFilterListener implements PacketListener {

    private final PacketConfig config;
    private final ESUPlugin plugin;
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    public PacketFilterListener(PacketConfig config, ESUPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PARTICLE) {
            handleParticle(event);
        } else if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            handleChat(event);
        }
    }

    private void handleParticle(PacketSendEvent event) {
        if (!config.damageIndicator()) return;
        WrapperPlayServerParticle packet = new WrapperPlayServerParticle(event);
        if (packet.getParticle().getType() == ParticleTypes.DAMAGE_INDICATOR) {
            event.setCancelled(true);
        }
    }

    private void handleChat(PacketSendEvent event) {
        List<String> filters = config.chatContent();
        if (filters.isEmpty()) return;

        String plainText = plainSerializer.serialize(new WrapperPlayServerSystemChatMessage(event).getMessage());
        if (filters.stream().noneMatch(plainText::contains)) return;

        UUID uuid = event.getUser().getUUID();
        String content = plainText;
        event.setCancelled(true);

        if (!config.popupName().isEmpty()) {
            plugin.getScheduler().runTask(() ->
                    BetterHudBridge.showPopup(config.popupName(), uuid, Map.of("content", content)));
        }
    }
}
