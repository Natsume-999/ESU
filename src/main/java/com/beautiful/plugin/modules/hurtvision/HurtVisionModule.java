package com.beautiful.plugin.modules.hurtvision;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.modules.AbstractESUModule;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import top.mrxiaom.pluginbase.func.AutoRegister;

@AutoRegister
public class HurtVisionModule extends AbstractESUModule implements Listener {

    private boolean enable;
    private boolean fadeEnabled;
    private int fadeSeconds;
    private int intensityModifier;
    private int minimumHealth;
    private boolean damageMode;

    public HurtVisionModule(ESUPlugin plugin) {
        super(plugin);
        registerEvents();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        enable = config.getBoolean("hurt-vision.enable", false);
        if (!enable) return;

        fadeEnabled = config.getBoolean("hurt-vision.fade.enabled", true);
        fadeSeconds = config.getInt("hurt-vision.fade.time", 5);
        intensityModifier = config.getInt("hurt-vision.intensity-modifier", 2);
        minimumHealth = config.getInt("hurt-vision.minimum-health", -1);
        damageMode = config.getBoolean("hurt-vision.damage-mode", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!enable) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isOnline()) return;

        int health = (int) player.getHealth();
        if (minimumHealth >= 0 && health > minimumHealth) return;

        int maxHealth = (int) player.getMaxHealth();
        if (maxHealth <= 0) return;

        int percent;
        if (damageMode) {
            int damage = (int) event.getFinalDamage();
            percent = Math.max(0, 100 - (damage * 100 / maxHealth));
        } else {
            percent = BloodScreenHelper.getHealthPercentage(health, maxHealth);
        }
        sendBloodScreen(player, percent);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {
        if (!enable || fadeEnabled) return;
        if (!(event.getEntity() instanceof Player player)) return;

        int newHealth = (int) (player.getHealth() + event.getAmount());
        sendBloodScreen(player, BloodScreenHelper.getHealthPercentage(newHealth, player.getMaxHealth()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!enable || fadeEnabled) return;
        Player player = event.getPlayer();
        plugin.getScheduler().runTaskLater(() ->
                sendBloodScreen(player, BloodScreenHelper.getHealthPercentage(player.getHealth(), player.getMaxHealth())), 5L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enable || fadeEnabled) return;
        Player player = event.getPlayer();
        plugin.getScheduler().runTaskLater(() ->
                sendBloodScreen(player, BloodScreenHelper.getHealthPercentage(player.getHealth(), player.getMaxHealth())), 10L);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (!enable || fadeEnabled) return;
        Player player = event.getPlayer();
        int health = (int) player.getHealth();
        if (minimumHealth >= 0 && health > minimumHealth) return;

        plugin.getScheduler().runTaskLater(() ->
                sendBloodScreen(player, BloodScreenHelper.getHealthPercentage(player.getHealth(), player.getMaxHealth())), 2L);
    }

    private void sendBloodScreen(Player player, int healthPercent) {
        if (fadeEnabled) {
            BloodScreenHelper.fadeBorder(player, healthPercent, intensityModifier, fadeSeconds);
        } else {
            BloodScreenHelper.setBorder(player, healthPercent, intensityModifier);
        }
    }

    public static HurtVisionModule inst() {
        return instanceOf(HurtVisionModule.class);
    }
}
