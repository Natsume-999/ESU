package com.beautiful.plugin.modules.command;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.modules.AbstractESUModule;
import com.beautiful.plugin.modules.recipe.RecipeModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ColorHelper;

import java.util.Collections;
import java.util.List;

@AutoRegister
public class CommandModule extends AbstractESUModule implements CommandExecutor, TabCompleter {

    public CommandModule(ESUPlugin plugin) {
        super(plugin);
        registerCommand("esu", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            ColorHelper.parseAndSend(sender, "&eESU &fv" + plugin.getDescription().getVersion());
            ColorHelper.parseAndSend(sender, "&7/esu reload &f- 重载配置文件");
            ColorHelper.parseAndSend(sender, "&7/esu recipe &f- 发现所有配方");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("esu.admin")) {
                    ColorHelper.parseAndSend(sender, "&c你没有权限执行此命令");
                    return true;
                }
                try {
                    plugin.reloadConfig();
                    ColorHelper.parseAndSend(sender, "&a[ESU] 配置文件重载成功！");
                } catch (Exception e) {
                    ColorHelper.parseAndSend(sender, "&c[ESU] 重载失败: " + e.getMessage());
                }
            }
            case "recipe" -> {
                if (!sender.hasPermission("esu.admin")) {
                    ColorHelper.parseAndSend(sender, "&c你没有权限执行此命令");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    ColorHelper.parseAndSend(sender, "&c[ESU] 仅玩家可用");
                    return true;
                }
                RecipeModule recipeModule = RecipeModule.inst();
                if (recipeModule != null) {
                    recipeModule.discoverRecipesForPlayer(player);
                    ColorHelper.parseAndSend(sender, "&a[ESU] 已发现所有 ESU 配方");
                } else {
                    ColorHelper.parseAndSend(sender, "&c[ESU] 配方模块未启用");
                }
            }
            default -> ColorHelper.parseAndSend(sender, "&c未知子命令: " + args[0]);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("reload", "recipe");
        return Collections.emptyList();
    }
}
