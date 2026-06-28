package com.beautiful.plugin.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public final class BetterHudBridge {

    private static Object popupManagerImpl;
    private static Method methodGetPopup;
    private static Method methodToHud;
    private static Method methodGetVariableMap;

    private static Method methodShow;
    private static Method methodHide;

    private static Constructor<?> customPopupEventCtor;
    private static Method customPopupEventGetVars;
    private static Constructor<?> bukkitEventUpdateEventCtor;

    private static boolean available = false;
    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            // PopupManagerImpl.INSTANCE.getPopup(name)
            Class<?> popupManagerClass = Class.forName("kr.toxicity.hud.manager.PopupManagerImpl");
            popupManagerImpl = popupManagerClass.getField("INSTANCE").get(null);
            methodGetPopup = popupManagerClass.getMethod("getPopup", String.class);

            // PlayersKt.toHud(player)
            Class<?> playersKt = Class.forName("kr.toxicity.hud.bootstrap.bukkit.util.PlayersKt");
            methodToHud = playersKt.getMethod("toHud", Player.class);

            // Popup.show / HudPlayer.getVariableMap
            Class<?> popupClass = Class.forName("kr.toxicity.hud.api.popup.Popup");
            Class<?> updateEventClass = Class.forName("kr.toxicity.hud.api.update.UpdateEvent");
            Class<?> hudPlayerClass = Class.forName("kr.toxicity.hud.api.player.HudPlayer");
            methodShow = popupClass.getMethod("show", updateEventClass, hudPlayerClass);
            methodHide = popupClass.getMethod("hide", hudPlayerClass);
            methodGetVariableMap = hudPlayerClass.getMethod("getVariableMap");

            // BukkitEventUpdateEvent(Event, Object)
            Class<?> bukkitUpdateClass = Class.forName("kr.toxicity.hud.api.bukkit.update.BukkitEventUpdateEvent");
            bukkitEventUpdateEventCtor = bukkitUpdateClass.getConstructor(Event.class, Object.class);

            // CustomPopupEvent(Player, String)
            Class<?> customEventClass = Class.forName("kr.toxicity.hud.api.bukkit.event.CustomPopupEvent");
            customPopupEventCtor = customEventClass.getConstructor(Player.class, String.class);
            customPopupEventGetVars = customEventClass.getMethod("getVariables");

            available = true;
        } catch (Exception ignored) {}
    }

    public static void showPopup(String popupName, UUID playerUUID) {
        showPopup(popupName, playerUUID, Map.of());
    }

    @SuppressWarnings("unchecked")
    public static void showPopup(String popupName, UUID playerUUID, Map<String, String> variables) {
        try {
            init();
            if (!available) return;
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) return;
            Object popup = methodGetPopup.invoke(popupManagerImpl, popupName);
            if (popup == null) return;
            Object hudPlayer = methodToHud.invoke(null, player);
            if (hudPlayer == null) return;

            // 严格对齐 Skript EffShowPopup：
            // [string:xxx] 由 BetterHud 的 strings 解析器从 CustomPopupEvent.getVariables() 读取，
            // 前提是 popup.show 传入的 UpdateEvent 包裹着这个 CustomPopupEvent。
            Object customEvent = customPopupEventCtor.newInstance(player, popupName);
            if (!variables.isEmpty()) {
                ((Map<String, String>) customPopupEventGetVars.invoke(customEvent)).putAll(variables);
            }

            // 触发事件
            Bukkit.getPluginManager().callEvent((Event) customEvent);

            // 用包裹 customEvent 的 UpdateEvent 触发 show，变量随之传入
            Object updateEvent = bukkitEventUpdateEventCtor.newInstance((Event) customEvent, UUID.randomUUID());
            methodShow.invoke(popup, updateEvent, hudPlayer);
        } catch (Exception ignored) {}
    }

    public static void hidePopup(String popupName, UUID playerUUID) {
        try {
            init();
            if (!available) return;
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) return;
            Object popup = methodGetPopup.invoke(popupManagerImpl, popupName);
            if (popup == null) return;
            Object hudPlayer = methodToHud.invoke(null, player);
            if (hudPlayer == null) return;
            methodHide.invoke(popup, hudPlayer);
        } catch (Exception ignored) {}
    }
}
