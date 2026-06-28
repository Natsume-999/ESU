package com.beautiful.plugin.modules.recipe;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.bridge.NeigeItemsBridge;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoRegister(requirePlugins = {"NeigeItems"})
public class RecipeModule extends AbstractESUModule implements Listener {

    private boolean disableDiscovery = true;
    private boolean clearVanillaRecipe = true;
    private String materialId = "";
    private List<String> recipeList = List.of();
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();
    private final Map<String, String> recipeKeyToItemId = new HashMap<>();

    public RecipeModule(ESUPlugin plugin) {
        super(plugin);
        registerEvents();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        if (!config.getBoolean("cover-vanilla-recipe.enable", true)) return;

        disableDiscovery = config.getBoolean("cover-vanilla-recipe.disable-recipe-discovery", true);
        clearVanillaRecipe = config.getBoolean("cover-vanilla-recipe.clear-vanilla-recipe", true);
        materialId = config.getString("cover-vanilla-recipe.material", "");
        recipeList = config.getStringList("cover-vanilla-recipe.recipe");

        plugin.getScheduler().runTaskLater(this::registerRecipes, 100L);
    }

    private void registerRecipes() {
        if (materialId.isEmpty() || recipeList.isEmpty()) return;

        // 等待 NeigeItems 加载完成
        ItemStack materialItem = NeigeItemsBridge.getItem(materialId);
        if (materialItem == null) {
            plugin.getScheduler().runTaskLater(this::registerRecipes, 100L);
            return;
        }

        if (clearVanillaRecipe) {
            Bukkit.clearRecipes();
        }

        registeredRecipes.clear();
        recipeKeyToItemId.clear();

        for (String recipeId : recipeList) {
            if (!recipeId.isEmpty()) {
                registerRecipe(recipeId, materialItem);
            }
        }

        if (registeredRecipes.isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            discoverRecipesForPlayer(player);
        }
    }

    private void registerRecipe(String resultId, ItemStack materialItem) {
        ItemStack resultItem = NeigeItemsBridge.getItem(resultId);
        if (resultItem == null) return;

        NamespacedKey key = new NamespacedKey("esu", resultId.toLowerCase());
        ShapelessRecipe recipe = new ShapelessRecipe(key, resultItem);
        // 使用 ExactChoice 精确匹配 NI 材料物品（含 NBT），配方书展示真实材料；
        // 配合 onCraftItem 取消 esu 配方，仅作图鉴展示+点击触发，不实际合成
        ItemStack ingredient = materialItem.clone();
        ingredient.setAmount(1);
        recipe.addIngredient(new RecipeChoice.ExactChoice(ingredient));

        if (Bukkit.addRecipe(recipe)) {
            registeredRecipes.add(key);
            recipeKeyToItemId.put(key.toString(), resultId);
        }
    }

    public void discoverRecipesForPlayer(Player player) {
        for (NamespacedKey key : registeredRecipes) {
            player.discoverRecipe(key);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 配方可能尚未注册完成（依赖 NI 加载），重试直至注册完成或玩家离线
        tryDiscoverOnJoin(player, 0);
    }

    private void tryDiscoverOnJoin(Player player, int attempts) {
        plugin.getScheduler().runTaskLater(() -> {
            if (!player.isOnline()) return;
            if (!registeredRecipes.isEmpty()) {
                discoverRecipesForPlayer(player);
                return;
            }
            // 最多重试 10 次（每次 20 tick = 1 秒，共约 10 秒）
            if (attempts < 10) {
                tryDiscoverOnJoin(player, attempts + 1);
            }
        }, 20L);
    }

    @EventHandler
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        if (!disableDiscovery) return;
        // 只拦截原版配方的自动解锁弹窗，放行本插件 esu 命名空间的配方发现
        NamespacedKey key = event.getRecipe();
        if (key != null && "esu".equals(key.getNamespace())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRecipeBookClick(PlayerRecipeBookClickEvent event) {
        NamespacedKey key = event.getRecipe();
        if (key == null || !"esu".equals(key.getNamespace())) return;

        event.setCancelled(true);

        String itemId = recipeKeyToItemId.get(key.toString());
        if (itemId != null) {
            NeigeItemsBridge.runRecipeAction(itemId, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        // esu 配方禁止实际合成（仅作图鉴展示）
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
