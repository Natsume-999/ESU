package com.beautiful.plugin;

import com.beautiful.plugin.bridge.NeigeItemsBridge;
import top.mrxiaom.pluginbase.BukkitPlugin;

public class ESUPlugin extends BukkitPlugin {

    public ESUPlugin() {
        super(options()
                .bungee(false)
                .database(false)
                .scanPackage("com.beautiful.plugin")
        );
    }

    public static ESUPlugin getInstance() {
        return (ESUPlugin) BukkitPlugin.getInstance();
    }

    @Override
    protected void afterEnable() {
        NeigeItemsBridge.setPlugin(this);
        getLogger().info("ESU 插件已启用！ 版本 " + getDescription().getVersion());
    }

    @Override
    protected void afterDisable() {
        getLogger().info("ESU 插件已禁用！");
    }
}
