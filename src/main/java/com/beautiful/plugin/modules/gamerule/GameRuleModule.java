package com.beautiful.plugin.modules.gamerule;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.modules.AbstractESUModule;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import top.mrxiaom.pluginbase.func.AutoRegister;

import java.util.HashMap;
import java.util.Map;

@AutoRegister
public class GameRuleModule extends AbstractESUModule {

    private final Map<String, GameRule<?>> ruleCache = new HashMap<>();

    public GameRuleModule(ESUPlugin plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        if (!config.getBoolean("allow-world-gamerule.enable", true)) return;
        ConfigurationSection worldsSection = config.getConfigurationSection("allow-world-gamerule.worlds");
        if (worldsSection == null) return;

        ruleCache.clear();

        for (String worldName : worldsSection.getKeys(false)) {
            if (worldName.isEmpty()) continue;
            ConfigurationSection rules = worldsSection.getConfigurationSection(worldName);
            if (rules == null) continue;

            if ("*".equals(worldName)) {
                for (World world : Bukkit.getWorlds()) {
                    applyWorldRules(world, rules);
                }
            } else {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    applyWorldRules(world, rules);
                }
            }
        }
    }

    private void applyWorldRules(World world, ConfigurationSection section) {
        for (String rawKey : section.getKeys(false)) {
            GameRule<?> rule = resolveGameRule(rawKey);
            if (rule != null) {
                setGameRule(world, rule, section.get(rawKey));
            }
        }
    }

    private GameRule<?> resolveGameRule(String rawKey) {
        return ruleCache.computeIfAbsent(rawKey, key -> GameRule.getByName(key.replace("-", "_").toLowerCase()));
    }

    @SuppressWarnings("unchecked")
    private <T> boolean setGameRule(World world, GameRule<T> rule, Object value) {
        T typed = null;
        if (rule.getType() == Boolean.class && value instanceof Boolean b) {
            typed = (T) b;
        } else if (rule.getType() == Integer.class && value instanceof Number n) {
            typed = (T) Integer.valueOf(n.intValue());
        }
        return typed != null && world.setGameRule(rule, typed);
    }
}
