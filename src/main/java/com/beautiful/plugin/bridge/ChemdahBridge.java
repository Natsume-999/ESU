package com.beautiful.plugin.bridge;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class ChemdahBridge {

    private static Method methodIsInConversation;
    private static Method methodGetQuestPlayer;
    private static boolean available = false;
    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> chemdahClass = Class.forName("ink.ptms.chemdah.api.ChemdahAPI");
            methodGetQuestPlayer = chemdahClass.getMethod("getQuestPlayer", Player.class);
            Class<?> questPlayerClass = Class.forName("ink.ptms.chemdah.core.quest.QuestPlayer");
            methodIsInConversation = questPlayerClass.getMethod("isConversation");
            available = true;
        } catch (Exception ignored) {}
    }

    public static boolean isInConversation(Player player) {
        try {
            init();
            if (!available) return false;
            Object questPlayer = methodGetQuestPlayer.invoke(null, player);
            if (questPlayer == null) return false;
            return (boolean) methodIsInConversation.invoke(questPlayer);
        } catch (Exception e) {
            return false;
        }
    }
}
