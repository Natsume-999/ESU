package com.beautiful.plugin.bridge;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import top.mrxiaom.pluginbase.utils.ColorHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public final class NeigeItemsBridge {

    private static Method getItemStackMethod;
    private static Method getItemMethod;
    private static Method getOriginConfigMethod;
    private static Method getItemIdMethod;
    private static Object itemManagerInstance;
    private static Object actionManagerInstance;
    private static Method runActionMethod;
    private static Class<?> contextClazz;
    private static Constructor<?> contextCtor;
    private static Constructor<?> stringActionCtor;
    private static boolean initialized = false;

    private static Plugin esuPlugin;

    public static void setPlugin(Plugin plugin) {
        esuPlugin = plugin;
    }

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> clazz = Class.forName("pers.neige.neigeitems.manager.ItemManager");
            getItemStackMethod = clazz.getMethod("getItemStack", String.class);
            getItemMethod = clazz.getMethod("getItem", String.class);
            getOriginConfigMethod = clazz.getMethod("getOriginConfig", String.class);
            itemManagerInstance = clazz.getField("INSTANCE").get(null);
        } catch (Exception ignored) {}

        try {
            // ItemUtils.getItemId(ItemStack) — 反查物品的 NeigeItems 内部 ID，非 NI 物品返回 null
            Class<?> itemUtilsClazz = Class.forName("pers.neige.neigeitems.utils.ItemUtils");
            getItemIdMethod = itemUtilsClazz.getMethod("getItemId", ItemStack.class);
        } catch (Exception ignored) {}

        try {
            Class<?> actionManagerClazz = Class.forName("pers.neige.neigeitems.manager.ActionManager");
            actionManagerInstance = actionManagerClazz.getField("INSTANCE").get(null);
            runActionMethod = actionManagerClazz.getMethod("runAction",
                    Class.forName("pers.neige.neigeitems.action.impl.StringAction"),
                    Class.forName("pers.neige.neigeitems.action.ActionContext"));
            contextClazz = Class.forName("pers.neige.neigeitems.action.ActionContext");
            contextCtor = contextClazz.getConstructor(Player.class);
            Class<?> stringActionClazz = Class.forName("pers.neige.neigeitems.action.impl.StringAction");
            for (Constructor<?> c : stringActionClazz.getDeclaredConstructors()) {
                if (c.getParameterCount() == 2
                        && c.getParameterTypes()[1] == String.class
                        && c.getParameterTypes()[0].isAssignableFrom(actionManagerInstance.getClass())) {
                    stringActionCtor = c;
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    public static ItemStack getItem(String id) {
        try {
            init();
            if (getItemStackMethod == null || itemManagerInstance == null) return null;
            return (ItemStack) getItemStackMethod.invoke(itemManagerInstance, id);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasItem(String id) {
        try {
            init();
            if (getItemMethod == null || itemManagerInstance == null) return false;
            return getItemMethod.invoke(itemManagerInstance, id) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 反查物品的 NeigeItems 内部 ID，非 NI 物品返回 null
     */
    public static String getItemId(ItemStack itemStack) {
        if (itemStack == null) return null;
        try {
            init();
            if (getItemIdMethod == null) return null;
            return (String) getItemIdMethod.invoke(null, itemStack);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 掉落 NI 物品，通过 Bukkit entity metadata 设置 owner（NeigeItems 的 EntityPickupItemListener 自动拦截非 owner 拾取）
     */
    public static void dropItem(Location location, ItemStack itemStack, int amount, Player owner) {
        if (location == null || location.getWorld() == null || itemStack == null) return;
        for (int i = 0; i < amount; i++) {
            Item item = location.getWorld().dropItem(location, itemStack.clone());
            if (owner != null && esuPlugin != null) {
                // NeigeItems 的 EntityPickupItemListener 读取 NI-Owner metadata 做 owner 检查
                item.setMetadata("NI-Owner", new FixedMetadataValue(esuPlugin, owner.getName()));
                item.addScoreboardTag("NeigeItems");
            }
        }
    }

    /**
     * 执行单个 NeigeItems 动作字符串
     */
    public static void runAction(String actionStr, Player player) {
        try {
            init();
            if (actionManagerInstance == null || runActionMethod == null || stringActionCtor == null) return;
            Object context = contextCtor.newInstance(player);
            Object action = stringActionCtor.newInstance(actionManagerInstance, actionStr);
            runActionMethod.invoke(actionManagerInstance, action, context);
        } catch (Exception ignored) {}
    }

    public static void runRecipeAction(String itemId, Player player) {
        try {
            init();
            if (actionManagerInstance == null || runActionMethod == null) {
                ColorHelper.parseAndSend(player, "&c[ESU] ActionManager 未初始化");
                return;
            }
            if (getOriginConfigMethod == null || itemManagerInstance == null) {
                ColorHelper.parseAndSend(player, "&c[ESU] ItemManager 未初始化");
                return;
            }
            if (stringActionCtor == null) {
                ColorHelper.parseAndSend(player, "&c[ESU] 未找到 StringAction 构造函数");
                return;
            }

            Object configObj = getOriginConfigMethod.invoke(itemManagerInstance, itemId);
            if (!(configObj instanceof ConfigurationSection config)) {
                ColorHelper.parseAndSend(player, "&c[ESU] 配置不是 ConfigurationSection");
                return;
            }

            Object actionValue = config.get("recipe.action");
            if (actionValue == null) return;

            List<String> actions;
            if (actionValue instanceof List<?> list) {
                actions = list.stream().map(Object::toString).toList();
            } else {
                actions = List.of(actionValue.toString());
            }
            if (actions.isEmpty()) return;

            Object context = contextCtor.newInstance(player);
            for (String actionStr : actions) {
                Object action = stringActionCtor.newInstance(actionManagerInstance, actionStr);
                runActionMethod.invoke(actionManagerInstance, action, context);
            }
        } catch (Exception e) {
            ColorHelper.parseAndSend(player, "&c[ESU] 动作执行异常: " + e.getMessage());
        }
    }
}
