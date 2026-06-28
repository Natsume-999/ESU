package com.beautiful.plugin.modules.hurtvision;

import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

public final class BloodScreenHelper {

    private BloodScreenHelper() {}

    public static int getHealthPercentage(double health, double maxHealth) {
        if (maxHealth <= 0) return 100;
        return Math.round((float) (health * 100 / maxHealth));
    }

    private static double calculateBorderSize(int size) {
        return -10000.0 * size + 1300000.0;
    }

    public static void setBorder(Player player, int healthPercent, int intensity) {
        int size = Math.round((float) healthPercent / Math.max(intensity, 1));
        sendWorldBorderPacket(player, calculateBorderSize(size), 0);
    }

    public static void fadeBorder(Player player, int healthPercent, int intensity, int fadeSeconds) {
        int size = Math.round((float) healthPercent / Math.max(intensity, 1));
        sendWorldBorderPacket(player, calculateBorderSize(size), fadeSeconds + 4);
    }

    public static void removeBorder(Player player) {
        sendWorldBorderPacket(player, 200000.0, 0);
    }

    private static void sendWorldBorderPacket(Player player, double targetSize, long seconds) {
        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(player.getLocation());
        border.setWarningTime(15);
        border.setDamageBuffer(99999.0);
        border.setDamageAmount(0);

        if (seconds > 0) {
            border.setSize(200000.0);
            player.setWorldBorder(border);
            border.setSize(targetSize, seconds);
        } else {
            border.setSize(targetSize);
            player.setWorldBorder(border);
        }
    }
}
