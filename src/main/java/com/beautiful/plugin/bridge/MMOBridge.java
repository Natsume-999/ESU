package com.beautiful.plugin.bridge;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * MMOCore + MMOItems + MythicLib 反射桥接。
 * 全部反射调用，避免编译期依赖（与 NeigeItemsBridge 风格一致）。
 *
 * 用途：读取手持 MMOItems 武器的类型/ID；动态改写 MMOCore 玩家的技能绑定（boundSkills），
 * 实现"换武器即换技能组"。技能释放仍走 MMOCore 原生按键事件。
 */
public final class MMOBridge {

    private static boolean initialized = false;
    private static boolean available = false;

    // MMOCore
    private static Method playerDataGet;          // PlayerData.get(OfflinePlayer)
    private static Method getProfess;             // PlayerData#getProfess()
    private static Method bindSkill;              // PlayerData#bindSkill(int, ClassSkill)
    private static Method unbindSkill;            // PlayerData#unbindSkill(int)
    private static Method getBoundSkill;          // PlayerData#getBoundSkill(int) -> ClassSkill
    private static Method mapBoundSkills;          // PlayerData#mapBoundSkills() -> Map<Integer,String>
    private static Object skillManagerInstance;   // SkillManager.INSTANCE
    private static Method getSkillById;           // SkillManager#getSkill(String) -> RegisteredSkill
    private static Constructor<?> classSkillCtor; // new ClassSkill(RegisteredSkill, int unlockLevel, int maxLevel)
    private static Method classSkillGetSkill;     // ClassSkill#getSkill() -> SkillHandler
    private static Method skillHandlerGetId;      // SkillHandler#getId() -> String

    // MMOItems / MythicLib NBTItem
    private static Method nbtItemGet;             // NBTItem.get(ItemStack) -> NBTItem
    private static Method nbtItemHasType;         // NBTItem#hasType()
    private static Method nbtItemGetType;         // NBTItem#getType()
    private static Method nbtItemGetString;       // NBTItem#getString(String)

    private MMOBridge() {}

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            // ---- MMOCore PlayerData ----
            Class<?> playerData = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            playerDataGet = playerData.getMethod("get", org.bukkit.OfflinePlayer.class);
            getProfess = playerData.getMethod("getProfess");
            getBoundSkill = playerData.getMethod("getBoundSkill", int.class);
            mapBoundSkills = playerData.getMethod("mapBoundSkills");

            Class<?> classSkill = Class.forName("net.Indyuce.mmocore.skill.ClassSkill");
            bindSkill = playerData.getMethod("bindSkill", int.class, classSkill);
            unbindSkill = playerData.getMethod("unbindSkill", int.class);
            classSkillGetSkill = classSkill.getMethod("getSkill");

            // ---- SkillManager（全局技能注册表，按 ID 查 RegisteredSkill）----
            Class<?> skillManager = Class.forName("net.Indyuce.mmocore.manager.SkillManager");
            skillManagerInstance = skillManager.getField("INSTANCE").get(null);
            getSkillById = skillManager.getMethod("getSkill", String.class);

            Class<?> registeredSkill = Class.forName("net.Indyuce.mmocore.skill.RegisteredSkill");
            // new ClassSkill(RegisteredSkill, int unlockLevel, int maxSkillLevel)
            classSkillCtor = classSkill.getConstructor(registeredSkill, int.class, int.class);

            Class<?> skillHandler = Class.forName("io.lumine.mythic.lib.skill.handler.SkillHandler");
            skillHandlerGetId = skillHandler.getMethod("getId");

            // ---- MythicLib NBTItem（读取 MMOItems 武器）----
            Class<?> nbtItem = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
            nbtItemGet = nbtItem.getMethod("get", ItemStack.class);
            nbtItemHasType = nbtItem.getMethod("hasType");
            nbtItemGetType = nbtItem.getMethod("getType");
            nbtItemGetString = nbtItem.getMethod("getString", String.class);

            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    /**
     * 读取物品的 MMOItems 物品 ID（如 EXCALIBUR）。非 MMOItems 物品返回 null。
     */
    public static String getMMOItemId(ItemStack item) {
        if (item == null) return null;
        init();
        if (!available) return null;
        try {
            Object nbt = nbtItemGet.invoke(null, item);
            if (nbt == null) return null;
            boolean hasType = (boolean) nbtItemHasType.invoke(nbt);
            if (!hasType) return null;
            String id = (String) nbtItemGetString.invoke(nbt, "MMOITEMS_ITEM_ID");
            return (id == null || id.isEmpty()) ? null : id;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 读取物品的 MMOItems 类型（如 SWORD、BOW）。非 MMOItems 物品返回 null。
     */
    public static String getMMOItemType(ItemStack item) {
        if (item == null) return null;
        init();
        if (!available) return null;
        try {
            Object nbt = nbtItemGet.invoke(null, item);
            if (nbt == null) return null;
            boolean hasType = (boolean) nbtItemHasType.invoke(nbt);
            if (!hasType) return null;
            String type = (String) nbtItemGetType.invoke(nbt);
            return (type == null || type.isEmpty()) ? null : type;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 把指定技能 ID 绑定到玩家的某个技能槽位。
     * 技能从全局注册表查找（不限职业），构造一个 unlockLevel=0 的 ClassSkill 绑定上去。
     *
     * @param player    玩家
     * @param slot      技能槽位（1 起）
     * @param skillId   MMOCore 技能 ID
     * @param maxLevel  技能最大等级（一般填 1 或技能实际上限）
     * @return 是否绑定成功
     */
    public static boolean bindSkill(Player player, int slot, String skillId, int maxLevel) {
        init();
        if (!available || player == null || skillId == null) return false;
        try {
            Object registered = getSkillById.invoke(skillManagerInstance, skillId);
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

    /**
     * 解绑玩家某个技能槽位。
     */
    public static void unbindSkill(Player player, int slot) {
        init();
        if (!available || player == null) return;
        try {
            Object data = playerDataGet.invoke(null, player);
            if (data == null) return;
            // 仅当该槽位有绑定时才解绑，避免无谓异常
            Object bound = getBoundSkill.invoke(data, slot);
            if (bound != null) {
                unbindSkill.invoke(data, slot);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * 查询某槽位当前绑定的技能 ID，无绑定返回 null。
     */
    public static String getBoundSkillId(Player player, int slot) {
        init();
        if (!available || player == null) return null;
        try {
            Object data = playerDataGet.invoke(null, player);
            if (data == null) return null;
            Object classSkill = getBoundSkill.invoke(data, slot);
            if (classSkill == null) return null;
            Object handler = classSkillGetSkill.invoke(classSkill);
            if (handler == null) return null;
            return (String) skillHandlerGetId.invoke(handler);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 玩家是否已加载 MMOCore 数据（未选职业也算已加载）。
     */
    public static boolean hasPlayerData(Player player) {
        init();
        if (!available || player == null) return false;
        try {
            return playerDataGet.invoke(null, player) != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
