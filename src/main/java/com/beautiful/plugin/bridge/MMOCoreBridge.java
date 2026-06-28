package com.beautiful.plugin.bridge;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * MMOCore 反射桥接：动态改写玩家技能栏绑定（boundSkills），实现"换武器即换技能组"。
 * 技能从全局注册表按 ID 查找（不限职业），技能释放仍走 MMOCore 原生按键。
 * 全反射调用，避免编译期依赖。
 */
public final class MMOCoreBridge {

    private static boolean initialized = false;
    private static boolean available = false;

    private static Method playerDataGet;      // PlayerData.get(OfflinePlayer)
    private static Method bindSkill;          // PlayerData#bindSkill(int, ClassSkill)
    private static Method unbindSkill;        // PlayerData#unbindSkill(int)
    private static Method getBoundSkill;      // PlayerData#getBoundSkill(int)
    private static Object skillManager;       // SkillManager.INSTANCE
    private static Method getSkillById;       // SkillManager#getSkill(String) -> RegisteredSkill
    private static Constructor<?> classSkillCtor; // new ClassSkill(RegisteredSkill, int, int)
    private static Method classSkillGetSkill; // ClassSkill#getSkill() -> SkillHandler
    private static Method skillHandlerGetId;  // SkillHandler#getId()

    private MMOCoreBridge() {}

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> playerData = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            playerDataGet = playerData.getMethod("get", org.bukkit.OfflinePlayer.class);
            getBoundSkill = playerData.getMethod("getBoundSkill", int.class);

            Class<?> classSkill = Class.forName("net.Indyuce.mmocore.skill.ClassSkill");
            bindSkill = playerData.getMethod("bindSkill", int.class, classSkill);
            unbindSkill = playerData.getMethod("unbindSkill", int.class);
            classSkillGetSkill = classSkill.getMethod("getSkill");

            Class<?> skillManagerClass = Class.forName("net.Indyuce.mmocore.manager.SkillManager");
            skillManager = skillManagerClass.getField("INSTANCE").get(null);
            getSkillById = skillManagerClass.getMethod("getSkill", String.class);

            Class<?> registeredSkill = Class.forName("net.Indyuce.mmocore.skill.RegisteredSkill");
            classSkillCtor = classSkill.getConstructor(registeredSkill, int.class, int.class);

            Class<?> skillHandler = Class.forName("io.lumine.mythic.lib.skill.handler.SkillHandler");
            skillHandlerGetId = skillHandler.getMethod("getId");

            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    /** 玩家是否已加载 MMOCore 数据。 */
    public static boolean hasPlayerData(Player player) {
        init();
        if (!available || player == null) return false;
        try {
            return playerDataGet.invoke(null, player) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 把指定技能 ID 绑定到玩家的某个技能槽位。
     * 技能从全局注册表查找（不限职业），构造 unlockLevel=0 的 ClassSkill 绑定。
     *
     * @param player   玩家
     * @param slot     技能槽位（1 起）
     * @param skillId  MMOCore 技能 ID
     * @param maxLevel 技能最大等级（一般填 1，可升级技能可调高）
     * @return 是否绑定成功
     */
    public static boolean bindSkill(Player player, int slot, String skillId, int maxLevel) {
        init();
        if (!available || player == null || skillId == null) return false;
        try {
            Object registered = getSkillById.invoke(skillManager, skillId);
            if (registered == null) return false; // 技能 ID 不存在
            Object classSkill = classSkillCtor.newInstance(registered, 0, Math.max(1, maxLevel));
            Object data = playerDataGet.invoke(null, player);
            if (data == null) return false;
            bindSkill.invoke(data, slot, classSkill);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 解绑玩家某个技能槽位（仅当该槽位有绑定时）。 */
    public static void unbindSkill(Player player, int slot) {
        init();
        if (!available || player == null) return;
        try {
            Object data = playerDataGet.invoke(null, player);
            if (data == null) return;
            if (getBoundSkill.invoke(data, slot) != null) {
                unbindSkill.invoke(data, slot);
            }
        } catch (Throwable ignored) {}
    }

    /** 查询某槽位当前绑定的技能 ID，无绑定返回 null。 */
    public static String getBoundSkillId(Player player, int slot) {
        init();
        if (!available || player == null) return null;
        try {
            Object data = playerDataGet.invoke(null, player);
            if (data == null) return null;
            Object classSkill = getBoundSkill.invoke(data, slot);
            if (classSkill == null) return null;
            Object handler = classSkillGetSkill.invoke(classSkill);
            return (handler == null) ? null : (String) skillHandlerGetId.invoke(handler);
        } catch (Throwable t) {
            return null;
        }
    }
}
