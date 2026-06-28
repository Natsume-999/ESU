package com.beautiful.plugin.modules.recipe;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.bridge.MMOItemsBridge;
import com.beautiful.plugin.modules.AbstractESUModule;
import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 配方管理模块（MMOItems 版）。
 * 清除原版配方，注册 MMOItems 物品的"图鉴配方"——仅作配方书展示，不可实际合成、点击不触发动作。
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

    // 配方成品标识（类型:ID）列表
    private final List<String> recipeDefs = new ArrayList<>();
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    public RecipeModule(ESUPlugin plugin) {
        super(plugin);
        registerEvents();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        enable = config.getBoolean("cover-vanilla-recipe.enable", true);
        recipeDefs.clear();
        if (!enable) return;

        disableDiscovery = config.getBoolean("cover-vanilla-recipe.disable-recipe-discovery", true);
        clearVanillaRecipe = config.getBoolean("cover-vanilla-recipe.clear-vanilla-recipe", true);

        // material 格式：类型:ID（如 MATERIAL:CRAFTING_TOKEN）
        String[] mat = splitTypeId(config.getString("cover-vanilla-recipe.material", ""));
        materialType = mat[0];
        materialId = mat[1];

        // recipe 列表：每项是成品的 类型:ID
        recipeDefs.addAll(config.getStringList("cover-vanilla-recipe.recipe"));

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

        for (String resultKey : recipeDefs) {
            if (!resultKey.isEmpty()) {
                registerRecipe(resultKey, materialItem);
            }
        }

        if (registeredRecipes.isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            discoverRecipesForPlayer(player);
        }
    }

    private void registerRecipe(String resultKey, ItemStack materialItem) {
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
        }
    }

    public void discoverRecipesForPlayer(Player player) {
        for (NamespacedKey key : registeredRecipes) {
            player.discoverRecipe(key);
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
        // esu 配方仅作图鉴展示，点击不做任何事（仅取消默认填充行为）
        NamespacedKey key = event.getRecipe();
        if (key == null || !"esu".equals(key.getNamespace())) return;
        event.setCancelled(true);
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

