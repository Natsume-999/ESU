package com.beautiful.plugin.bridge;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * MMOItems 反射桥接：读取/生成 MMOItems 物品。
 * 全反射调用，避免编译期依赖。
 */
public final class MMOItemsBridge {

    private static boolean initialized = false;
    private static boolean available = false;

    private static Object mmoItemsPlugin;     // MMOItems.plugin
    private static Method getItemManager;     // MMOItems#getItems()
    private static Object itemManager;        // ItemManager 实例
    private static Method itemManagerGetItem; // ItemManager#getItem(String type, String id) -> ItemStack

    private static Method nbtItemGet;         // NBTItem.get(ItemStack)
    private static Method nbtItemHasType;     // NBTItem#hasType()
    private static Method nbtItemGetType;     // NBTItem#getType()
    private static Method nbtItemGetString;   // NBTItem#getString(String)

    private MMOItemsBridge() {}

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> mmoItems = Class.forName("net.Indyuce.mmoitems.MMOItems");
            mmoItemsPlugin = mmoItems.getField("plugin").get(null);
            getItemManager = mmoItems.getMethod("getItems");
            itemManager = getItemManager.invoke(mmoItemsPlugin);
            // ItemManager#getItem(String type, String id)
            itemManagerGetItem = itemManager.getClass().getMethod("getItem", String.class, String.class);

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
     * 按 类型 + ID 生成 MMOItems 物品。失败返回 null。
     *
     * @param type MMOItems 物品类型（如 SWORD、BOW、MATERIAL）
     * @param id   MMOItems 物品 ID
     */
    public static ItemStack getItem(String type, String id) {
        init();
        if (!available || type == null || id == null) return null;
        try {
            Object result = itemManagerGetItem.invoke(itemManager, type, id);
            return (result instanceof ItemStack stack) ? stack : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 读取物品的 MMOItems 物品 ID（如 EXCALIBUR）。非 MMOItems 物品返回 null。 */
    public static String getItemId(ItemStack item) {
        return readNbtString(item, "MMOITEMS_ITEM_ID");
    }

    /** 读取物品的 MMOItems 类型（如 SWORD、BOW）。非 MMOItems 物品返回 null。 */
    public static String getItemType(ItemStack item) {
        if (item == null) return null;
        init();
        if (!available) return null;
        try {
            Object nbt = nbtItemGet.invoke(null, item);
            if (nbt == null || !(boolean) nbtItemHasType.invoke(nbt)) return null;
            String type = (String) nbtItemGetType.invoke(nbt);
            return (type == null || type.isEmpty()) ? null : type;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String readNbtString(ItemStack item, String key) {
        if (item == null) return null;
        init();
        if (!available) return null;
        try {
            Object nbt = nbtItemGet.invoke(null, item);
            if (nbt == null || !(boolean) nbtItemHasType.invoke(nbt)) return null;
            String value = (String) nbtItemGetString.invoke(nbt, key);
            return (value == null || value.isEmpty()) ? null : value;
        } catch (Throwable t) {
            return null;
        }
    }
}
