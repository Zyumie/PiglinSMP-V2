package fr.zyumie.PiglinSMP.managers;

import fr.zyumie.PiglinSMP.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class WeaponManager {

    private final Main plugin;

    // Map des armes disponibles : id -> ItemStack template
    private final Map<String, ItemStack> weapons = new LinkedHashMap<>();

    // Matériaux de base pour chaque arme
    private static final Map<String, Material> BASE_MATERIALS = Map.of(
            "nether_unstable_bow",   Material.BOW,
            "poison_blade",          Material.IRON_SWORD,
            "thief_dagger",          Material.STONE_SWORD,
            "time_sword",            Material.GOLDEN_SWORD,
            "berserker_sword",       Material.DIAMOND_SWORD,
            "piglin_judgment_sword", Material.NETHERITE_SWORD,
            "trickster_staff",       Material.BLAZE_ROD
    );

    // Enchantements de base pour chaque arme
    private static final Map<String, Map<Enchantment, Integer>> BASE_ENCHANTS = new HashMap<>();

    static {
        BASE_ENCHANTS.put("nether_unstable_bow", Map.of(
                Enchantment.INFINITY, 1,
                Enchantment.UNBREAKING, 3
        ));
        BASE_ENCHANTS.put("poison_blade", Map.of(
                Enchantment.UNBREAKING, 2
        ));
        BASE_ENCHANTS.put("thief_dagger", Map.of(
                Enchantment.SHARPNESS, 1,
                Enchantment.UNBREAKING, 2
        ));
        BASE_ENCHANTS.put("time_sword", Map.of(
                Enchantment.SHARPNESS, 2,
                Enchantment.UNBREAKING, 3
        ));
        BASE_ENCHANTS.put("berserker_sword", Map.of(
                Enchantment.UNBREAKING, 3
        ));
        BASE_ENCHANTS.put("piglin_judgment_sword", Map.of(
                Enchantment.SHARPNESS, 3,
                Enchantment.UNBREAKING, 3,
                Enchantment.FIRE_ASPECT, 1
        ));
        BASE_ENCHANTS.put("trickster_staff", Map.of(
                Enchantment.UNBREAKING, 2
        ));
    }

    public WeaponManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * (Re)charge toutes les armes depuis la config.
     */
    public void reload() {
        weapons.clear();
        ConfigurationSection weaponsSection = plugin.getConfig().getConfigurationSection("weapons");
        if (weaponsSection == null) return;

        for (String id : weaponsSection.getKeys(false)) {
            ConfigurationSection sec = weaponsSection.getConfigurationSection(id);
            if (sec == null || !sec.getBoolean("enabled", true)) continue;

            ItemStack item = buildWeapon(id, sec);
            if (item != null) weapons.put(id, item);
        }
    }

    /**
     * Construit un ItemStack pour l'arme donnée depuis sa section config.
     */
    private ItemStack buildWeapon(String id, ConfigurationSection sec) {
        Material mat = BASE_MATERIALS.get(id);
        if (mat == null) {
            plugin.getLogger().warning("Arme inconnue dans la config : " + id);
            return null;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        // ── Nom affiché
        String displayName = ChatColor.translateAlternateColorCodes('&',
                sec.getString("display-name", "&f" + id));
        meta.setDisplayName(displayName);

        // ── Lore : description + rareté depuis config
        List<String> lore = new ArrayList<>();
        String description = sec.getString("description", "");
        String rarity      = sec.getString("rarity", "");

        if (!description.isEmpty())
            lore.add(ChatColor.translateAlternateColorCodes('&', description));

        lore.add(""); // ligne vide séparatrice

        if (!rarity.isEmpty())
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Rareté : " + rarity));

        lore.add(ChatColor.translateAlternateColorCodes('&', "&cPiglin Bartering Drop"));

        meta.setLore(lore);

        // ── Enchantements
        Map<Enchantment, Integer> enchants = BASE_ENCHANTS.getOrDefault(id, Collections.emptyMap());
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            meta.addEnchant(e.getKey(), e.getValue(), true);
        }

        // ── Masquer les attributs/flags parasites
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // ── Tag PDC pour identifier l'arme en jeu
        NamespacedKey key = new NamespacedKey(plugin, id);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Retourne un clone de l'ItemStack template pour l'arme donnée.
     */
    public ItemStack getWeapon(String id) {
        ItemStack base = weapons.get(id);
        return base != null ? base.clone() : null;
    }

    /**
     * Vérifie si un ItemStack est une arme custom et retourne son id, ou null.
     */
    public String getWeaponId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        for (String id : weapons.keySet()) {
            NamespacedKey key = new NamespacedKey(plugin, id);
            if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
                return id;
        }
        return null;
    }

    public Set<String> getWeaponIds()  { return weapons.keySet(); }
    public int getWeaponCount()        { return weapons.size(); }

    /**
     * Drop rate configuré pour une arme (0.0 si absente).
     */
    public double getDropRate(String id) {
        return plugin.getConfig().getDouble("weapons." + id + ".drop-rate", 0.0);
    }

    /**
     * Raccourci pour lire un int d'effet depuis la config.
     */
    public int getEffectInt(String weaponId, String effectKey, int def) {
        return plugin.getConfig().getInt("weapons." + weaponId + ".effects." + effectKey, def);
    }

    /**
     * Raccourci pour lire un double d'effet depuis la config.
     */
    public double getEffectDouble(String weaponId, String effectKey, double def) {
        return plugin.getConfig().getDouble("weapons." + weaponId + ".effects." + effectKey, def);
    }
}