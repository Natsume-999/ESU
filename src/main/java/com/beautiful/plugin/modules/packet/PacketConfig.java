package com.beautiful.plugin.modules.packet;

import java.util.List;

public record PacketConfig(boolean damageIndicator, List<String> chatContent, String popupName) {}
