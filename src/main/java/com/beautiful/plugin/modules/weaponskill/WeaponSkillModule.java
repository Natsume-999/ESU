package com.beautiful.plugin.modules.weaponskill;

import com.beautiful.plugin.ESUPlugin;
import com.beautiful.plugin.bridge.MMOCoreBridge;
import com.beautiful.plugin.bridge.MMOItemsBridge;
import com.beautiful.plugin.modules.AbstractESUModule;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ColorHelper;

import java.util.*;

/**
 * 武器技能模块：手持的 MMOItems 武器决定 MMOCore 技能栏绑定的技能。
 * 换武器即换技能组，释放仍走 MMOCore 原生按键。
 *
 * 依赖：MMOCore + MMOItems（软依赖，缺失则模块静默禁用）
 */
@AutoRegister(requirePlugins = {"MMOCore", "MMOItems"})
public class WeaponSkillModule extends AbstractESUModule implements Listener {

    private boolean enable = false;
    private int maxSkillLevel = 1;
    private boolean clearWhenUnequip = true;

    // 切换提示
    private boolean messageEnable = true;
    private boolean messageActionBar = true;     // true=动作栏(物品栏上方)，false=聊天框
    private String messageEquip = "&a已切换为 &e{weapon} &a技能组";
    private String messageUnequip = "&7已收起武器技能";

    // 武器物品ID -> 槽位技能映射（slot 从 1 起，按配置顺序分配）
    private final Map<String, List<String>> weaponSkills = new HashMap<>();
    // 武器物品ID -> 显示名（用于提示 {weapon}），缺省用物品ID
    private final Map<String, String> weaponNames = new HashMap<>();
    // 记录每个玩家当前生效的武器ID，避免重复刷新
    private final Map<UUID, String> activeWeapon = new HashMap<>();

    public WeaponSkillModule(ESUPlugin plugin) {
        super(plugin);
        registerEvents();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        weaponSkills.clear();
        weaponNames.clear();
        activeWeapon.clear();

        enable = config.getBoolean("weapon-skill.enable", false);
        if (!enable) return;

        maxSkillLevel = config.getInt("weapon-skill.max-skill-level", 1);
        clearWhenUnequip = config.getBoolean("weapon-skill.clear-when-unequip", true);

        messageEnable = config.getBoolean("weapon-skill.message.enable", true);
        messageActionBar = config.getBoolean("weapon-skill.message.action-bar", true);
        messageEquip = config.getString("weapon-skill.message.equip", "&a已切换为 &e{weapon} &a技能组");
        messageUnequip = config.getString("weapon-skill.message.unequip", "&7已收起武器技能");

        ConfigurationSection weapons = config.getConfigurationSection("weapon-skill.weapons");
        if (weapons != null) {
            for (String rawId : weapons.getKeys(false)) {
                String weaponId = rawId.toUpperCase(Locale.ROOT);
                // 两种写法：1) 直接是技能列表  2) 带 name + skills 的小节
                if (weapons.isList(rawId)) {
                    List<String> skills = weapons.getStringList(rawId);
                    if (!skills.isEmpty()) {
                        weaponSkills.put(weaponId, skills);
                        weaponNames.put(weaponId, rawId);
                    }
                } else {
                    ConfigurationSection sec = weapons.getConfigurationSection(rawId);
                    if (sec == null) continue;
                    List<String> skills = sec.getStringList("skills");
                    if (skills.isEmpty()) continue;
                    weaponSkills.put(weaponId, skills);
                    weaponNames.put(weaponId, sec.getString("name", rawId));
                }
            }
        }

        if (!MMOCoreBridge.isAvailable() || !MMOItemsBridge.isAvailable()) {
            warn("MMOCore/MMOItems API 未就绪，武器技能模块无法工作");
            enable = false;
            return;
        }

        // 重载后为所有在线玩家立即刷新一次
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
        info("武器技能模块已启用，配置武器 " + weaponSkills.size() + " 把");
    }

    public boolean isEnable() {
        return enable;
    }

    // ============ 事件 ============

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enable) return;
        // MMOCore 数据可能稍晚加载，延迟一拍刷新
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> refresh(player), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeWeapon.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 切换快捷栏：用 newSlot 取目标格子物品，决定新武器。
     * 此事件在主手真正改变前触发，故用事件给出的目标槽位读取。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!enable) return;
        Player player = event.getPlayer();
        ItemStack target = player.getInventory().getItem(event.getNewSlot());
        applyForItem(player, target);
    }

    // ============ 核心逻辑 ============

    /**
     * 根据玩家当前主手物品刷新技能绑定。
     */
    public void refresh(Player player) {
        if (!enable || player == null) return;
        applyForItem(player, player.getInventory().getItemInMainHand());
    }

    /**
     * 根据给定物品应用对应武器的技能组。
     */
    private void applyForItem(Player player, ItemStack item) {
        if (!MMOCoreBridge.hasPlayerData(player)) return;

        String weaponId = MMOItemsBridge.getItemId(item);
        String key = (weaponId == null) ? null : weaponId.toUpperCase(Locale.ROOT);
        UUID uuid = player.getUniqueId();
        String current = activeWeapon.get(uuid);

        // 命中配置的武器
        if (key != null && weaponSkills.containsKey(key)) {
            if (key.equals(current)) return; // 已是该武器，跳过
            bindWeaponSkills(player, weaponSkills.get(key));
            activeWeapon.put(uuid, key);
            sendMessage(player, messageEquip, weaponNames.getOrDefault(key, key));
            return;
        }

        // 非配置武器 / 空手
        if (current != null) {
            // 之前有武器技能，现在清掉
            if (clearWhenUnequip) {
                clearWeaponSkills(player, weaponSkills.get(current));
            }
            activeWeapon.remove(uuid);
            sendMessage(player, messageUnequip, weaponNames.getOrDefault(current, current));
        }
    }

    /** 发送切换提示（动作栏或聊天框）。 */
    private void sendMessage(Player player, String template, String weaponName) {
        if (!messageEnable || template == null || template.isEmpty()) return;
        String text = template.replace("{weapon}", weaponName == null ? "" : weaponName);
        if (messageActionBar) {
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                            ColorHelper.parseColor(text)));
        } else {
            ColorHelper.parseAndSend(player, text);
        }
    }

    /**
     * 把技能列表按顺序绑定到槽位 1,2,3...
     */
    private void bindWeaponSkills(Player player, List<String> skills) {
        // 先清空将要使用的槽位范围，避免残留
        int slot = 1;
        for (String skillId : skills) {
            MMOCoreBridge.bindSkill(player, slot, skillId, maxSkillLevel);
            slot++;
        }
    }

    /**
     * 解绑某武器占用的槽位。
     */
    private void clearWeaponSkills(Player player, List<String> skills) {
        if (skills == null) return;
        for (int slot = 1; slot <= skills.size(); slot++) {
            MMOCoreBridge.unbindSkill(player, slot);
        }
    }
}
