package com.beautiful.plugin.modules.recipe;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.bridge.MMOItemsBridge;
import com.beautiful.plugin.modules.AbstractESUModule;
import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ColorHelper;

import java.util.*;

/**
 * 配方管理模块（MMOItems 版）。
 * 清除原版配方，注册 MMOItems 物品的"图鉴配方"——仅作配方书展示与点击触发，不可实际合成。
 * 点击配方书中的配方时，执行该配方配置的 ESU 自定义动作（message / command / give）。
 *
 * 配方物品用 "类型:ID" 格式标识（如 SWORD:CUTLASS）。
 * 依赖：MMOItems
 */
@AutoRegister(requirePlugins = {"MMOItems"})
public class RecipeModule extends AbstractESUModule implements Listener {

    private boolean enable = true;
    private boolean disableDiscovery = true;
    private boolean clearVanillaRecipe = true;
    private String materialType = "";
    private String materialId = "";

    // 配方 key(esu 命名空间) -> 点击时执行的动作列表
    private final Map<String, List<String>> recipeActions = new HashMap<>();
    // 配方 result 标识（type:id）列表，用于注册
    private final Map<String, List<String>> recipeDefs = new LinkedHashMap<>();
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    public RecipeModule(ESUPlugin plugin) {
        super(plugin);
        registerEvents();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        enable = config.getBoolean("cover-vanilla-recipe.enable", true);
        recipeActions.clear();
        recipeDefs.clear();
        if (!enable) return;

        disableDiscovery = config.getBoolean("cover-vanilla-recipe.disable-recipe-discovery", true);
        clearVanillaRecipe = config.getBoolean("cover-vanilla-recipe.clear-vanilla-recipe", true);

        // material 格式：类型:ID（如 MATERIAL:CRAFTING_TOKEN）
        String[] mat = splitTypeId(config.getString("cover-vanilla-recipe.material", ""));
        materialType = mat[0];
        materialId = mat[1];

        // recipes 段：每个键是 result 的 类型:ID，值是点击动作列表
        ConfigurationSection recipes = config.getConfigurationSection("cover-vanilla-recipe.recipes");
        if (recipes != null) {
            for (String resultKey : recipes.getKeys(false)) {
                recipeDefs.put(resultKey, recipes.getStringList(resultKey));
            }
        }

        if (!MMOItemsBridge.isAvailable()) {
            warn("MMOItems API 未就绪，配方模块无法工作");
            return;
        }

        plugin.getScheduler().runTaskLater(this::registerRecipes, 100L);
    }

    public boolean isEnable() {
        return enable;
    }

    /** 拆分 "类型:ID" 为 [类型, ID]；缺省类型为空。 */
    private static String[] splitTypeId(String raw) {
        if (raw == null || raw.isEmpty()) return new String[]{"", ""};
        int idx = raw.indexOf(':');
        if (idx < 0) return new String[]{"", raw.trim()};
        return new String[]{raw.substring(0, idx).trim(), raw.substring(idx + 1).trim()};
    }

    // ============ 配方注册 ============

    private void registerRecipes() {
        if (materialId.isEmpty() || recipeDefs.isEmpty()) return;

        // 等待 MMOItems 材料物品可用
        ItemStack materialItem = MMOItemsBridge.getItem(materialType, materialId);
        if (materialItem == null) {
            plugin.getScheduler().runTaskLater(this::registerRecipes, 100L);
            return;
        }

        if (clearVanillaRecipe) {
            Bukkit.clearRecipes();
        }
        registeredRecipes.clear();
        recipeActions.clear();

        for (Map.Entry<String, List<String>> entry : recipeDefs.entrySet()) {
            registerRecipe(entry.getKey(), entry.getValue(), materialItem);
        }

        if (registeredRecipes.isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            discoverRecipesForPlayer(player);
        }
    }

    private void registerRecipe(String resultKey, List<String> actions, ItemStack materialItem) {
        String[] r = splitTypeId(resultKey);
        ItemStack resultItem = MMOItemsBridge.getItem(r[0], r[1]);
        if (resultItem == null) {
            warn("配方结果物品不存在: " + resultKey);
            return;
        }

        // 命名空间 key 用 result 的 ID（小写、清洗非法字符）
        String keyId = r[1].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        NamespacedKey key = new NamespacedKey("esu", keyId);

        ShapelessRecipe recipe = new ShapelessRecipe(key, resultItem);
        ItemStack ingredient = materialItem.clone();
        ingredient.setAmount(1);
        recipe.addIngredient(new RecipeChoice.ExactChoice(ingredient));

        if (Bukkit.addRecipe(recipe)) {
            registeredRecipes.add(key);
            recipeActions.put(key.toString(), actions == null ? List.of() : actions);
        }
    }

    public void discoverRecipesForPlayer(Player player) {
        for (NamespacedKey key : registeredRecipes) {
            player.discoverRecipe(key);
        }
    }

    // ============ 点击动作执行 ============

    /**
     * 执行配方点击动作。支持前缀：
     *   message: 文本   —— 给玩家发消息（支持 & 颜色码）
     *   command: 指令   —— 控制台执行（%player% 替换为玩家名）
     *   player: 指令    —— 玩家身份执行（%player% 替换为玩家名）
     *   give: 类型:ID   —— 给予一个 MMOItems 物品
     */
    private void runActions(Player player, List<String> actions) {
        if (actions == null) return;
        for (String raw : actions) {
            if (raw == null || raw.isEmpty()) continue;
            int idx = raw.indexOf(':');
            String prefix = idx < 0 ? "" : raw.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String body = idx < 0 ? raw.trim() : raw.substring(idx + 1).trim();
            switch (prefix) {
                case "message" -> ColorHelper.parseAndSend(player, body);
                case "command" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        body.replace("%player%", player.getName()));
                case "player" -> player.performCommand(body.replace("%player%", player.getName()));
                case "give" -> {
                    String[] ti = splitTypeId(body);
                    ItemStack item = MMOItemsBridge.getItem(ti[0], ti[1]);
                    if (item != null) {
                        player.getInventory().addItem(item);
                    } else {
                        warn("give 动作物品不存在: " + body);
                    }
                }
                default -> ColorHelper.parseAndSend(player, raw); // 无前缀按消息处理
            }
        }
    }

    // ============ 事件 ============

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enable) return;
        tryDiscoverOnJoin(event.getPlayer(), 0);
    }

    private void tryDiscoverOnJoin(Player player, int attempts) {
        plugin.getScheduler().runTaskLater(() -> {
            if (!player.isOnline()) return;
            if (!registeredRecipes.isEmpty()) {
                discoverRecipesForPlayer(player);
                return;
            }
            if (attempts < 10) {
                tryDiscoverOnJoin(player, attempts + 1);
            }
        }, 20L);
    }

    @EventHandler
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        if (!disableDiscovery) return;
        // 放行本插件 esu 命名空间，拦截原版配方自动解锁弹窗
        NamespacedKey key = event.getRecipe();
        if (key != null && "esu".equals(key.getNamespace())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRecipeBookClick(PlayerRecipeBookClickEvent event) {
        NamespacedKey key = event.getRecipe();
        if (key == null || !"esu".equals(key.getNamespace())) return;
        event.setCancelled(true);
        runActions(event.getPlayer(), recipeActions.get(key.toString()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        // esu 配方仅作图鉴展示，禁止实际合成
        if (event.getRecipe() instanceof org.bukkit.Keyed keyed
                && "esu".equals(keyed.getKey().getNamespace())) {
            event.setCancelled(true);
            return;
        }
        if (clearVanillaRecipe) {
            event.setCancelled(true);
        }
    }

    public static RecipeModule inst() {
        return instanceOf(RecipeModule.class);
    }
}

