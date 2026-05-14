package fr.zyumie.PiglinSMP.listeners;

import fr.zyumie.PiglinSMP.Main;
import fr.zyumie.PiglinSMP.managers.CooldownManager;
import fr.zyumie.PiglinSMP.managers.WeaponManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CombatListener implements Listener {

    private final Main plugin;
    private final WeaponManager       wm;
    private final CooldownManager     cm;

    // Berserker : clé NamespacedKey pour identifier le modifier (API 1.21+)
    private final NamespacedKey BERSERKER_ATK_KEY;

    public CombatListener(Main plugin) {
        this.plugin = plugin;
        this.wm     = plugin.getWeaponManager();
        this.cm     = plugin.getCooldownManager();
        this.BERSERKER_ATK_KEY = new NamespacedKey(plugin, "berserker_rage");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (event.isCancelled()) return;

        ItemStack hand = attacker.getInventory().getItemInMainHand();
        String weaponId = wm.getWeaponId(hand);
        if (weaponId == null) return;

        LivingEntity target = (event.getEntity() instanceof LivingEntity le) ? le : null;

        switch (weaponId) {
            case "poison_blade"          -> handlePoisonBlade(attacker, target);
            case "thief_dagger"          -> handleThiefDagger(attacker, target);
            case "time_sword"            -> handleTimeSword(attacker, target);
            case "berserker_sword"       -> handleBerserker(attacker, target, event);
            case "piglin_judgment_sword" -> handleJudgment(attacker, target, event);
            case "trickster_staff"       -> handleTrickster(attacker, target);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // #2 — Lame du Poison
    // ══════════════════════════════════════════════════════════════
    private void handlePoisonBlade(Player attacker, LivingEntity target) {
        if (target == null) return;
        UUID targetId = target.getUniqueId();

        int stacks = wm.getEffectInt("poison_blade", "stack-reset-ticks", 200);
        int current = cm.getStack("poison_blade", targetId);
        int next    = Math.min(current + 1, 3);
        cm.stacks().put("poison_blade:" + targetId, next);   // incrémente manuellement

        int duration, amplifier;
        if (next == 1) {
            duration  = wm.getEffectInt("poison_blade", "hit1-duration-ticks", 100);
            amplifier = wm.getEffectInt("poison_blade", "hit1-amplifier", 0);
        } else if (next == 2) {
            duration  = wm.getEffectInt("poison_blade", "hit2-duration-ticks", 100);
            amplifier = wm.getEffectInt("poison_blade", "hit2-amplifier", 1);
        } else {
            duration  = wm.getEffectInt("poison_blade", "hit3-duration-ticks", 140);
            amplifier = wm.getEffectInt("poison_blade", "hit3-amplifier", 2);
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, duration, amplifier, false, true), true);

        // Réinitialisation après timeout
        cm.scheduleStackReset("poison_blade", targetId, stacks);
    }

    // ══════════════════════════════════════════════════════════════
    // #3 — Dague du Voleur
    // ══════════════════════════════════════════════════════════════
    private void handleThiefDagger(Player attacker, LivingEntity target) {
        if (target == null) return;
        UUID targetId = target.getUniqueId();

        int cooldownTicks = wm.getEffectInt("thief_dagger", "cooldown-ticks", 40);
        if (cm.isOnCooldown("thief_dagger", targetId)) return;

        int stealChance = wm.getEffectInt("thief_dagger", "steal-chance", 35);
        if (ThreadLocalRandom.current().nextInt(100) >= stealChance) return;

        // Chercher un item dans l'inventaire de la cible
        if (!(target instanceof InventoryHolder holder)) return;

        ItemStack[] contents = holder.getInventory().getStorageContents();
        List<Integer> nonEmpty = new ArrayList<>();
        for (int i = 0; i < contents.length; i++)
            if (contents[i] != null && contents[i].getType() != Material.AIR) nonEmpty.add(i);

        if (nonEmpty.isEmpty()) return;

        int slot = nonEmpty.get(ThreadLocalRandom.current().nextInt(nonEmpty.size()));
        ItemStack stolen = contents[slot].clone();
        holder.getInventory().setItem(slot, null);

        int dropChance = wm.getEffectInt("thief_dagger", "drop-chance", 50);
        if (ThreadLocalRandom.current().nextInt(100) < dropChance) {
            target.getWorld().dropItemNaturally(target.getLocation(), stolen);
        } else {
            Map<Integer, ItemStack> leftover = attacker.getInventory().addItem(stolen);
            leftover.values().forEach(l -> attacker.getWorld().dropItemNaturally(attacker.getLocation(), l));
        }

        String prefix = plugin.getConfig().getString("settings.prefix", "&8[&6NetherWeapons&8] &r");
        attacker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                prefix + "&5Vol réussi ! &7(" + stolen.getType().name() + " x" + stolen.getAmount() + ")"));

        cm.setCooldown("thief_dagger", targetId, cooldownTicks);
    }

    // ══════════════════════════════════════════════════════════════
    // #4 — Épée du Temps
    // ══════════════════════════════════════════════════════════════
    private void handleTimeSword(Player attacker, LivingEntity target) {
        if (target == null) return;
        UUID attackerId = attacker.getUniqueId();

        int cooldown = wm.getEffectInt("time_sword", "cooldown-ticks", 60);
        if (cm.isOnCooldown("time_sword", attackerId)) return;

        int slowDur  = wm.getEffectInt("time_sword", "slowness-duration-ticks", 60);
        int slowAmp  = wm.getEffectInt("time_sword", "slowness-amplifier", 2);
        int fatDur   = wm.getEffectInt("time_sword", "fatigue-duration-ticks", 60);
        int fatAmp   = wm.getEffectInt("time_sword", "fatigue-amplifier", 1);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,      slowDur, slowAmp));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, fatDur,  fatAmp));

        int freezeChance = wm.getEffectInt("time_sword", "freeze-chance", 15);
        if (ThreadLocalRandom.current().nextInt(100) < freezeChance) {
            int freezeDur = wm.getEffectInt("time_sword", "freeze-duration-ticks", 20);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, freezeDur, 254), true);

            // Particules freeze
            int range = plugin.getConfig().getInt("settings.particle-range", 16);
            target.getWorld().getNearbyPlayers(target.getLocation(), range)
                    .forEach(p -> p.spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0));
        }

        cm.setCooldown("time_sword", attackerId, cooldown);
    }

    // ══════════════════════════════════════════════════════════════
    // #5 — Épée du Berserker
    // ══════════════════════════════════════════════════════════════
    private void handleBerserker(Player attacker, LivingEntity target, EntityDamageByEntityEvent event) {
        UUID id = attacker.getUniqueId();
        int maxStacks   = wm.getEffectInt("berserker_sword", "max-stacks", 10);
        int resetTicks  = wm.getEffectInt("berserker_sword", "stack-reset-ticks", 160);

        int current = cm.getStack("berserker_sword", id);
        current = Math.min(current + 1, maxStacks);
        cm.stacks().put("berserker_sword:" + id, current);
        cm.scheduleStackReset("berserker_sword", id, resetTicks);

        // Déterminer le tier
        int tier1 = wm.getEffectInt("berserker_sword", "tier1-stacks", 1);
        int tier2 = wm.getEffectInt("berserker_sword", "tier2-stacks", 5);
        int tier3 = wm.getEffectInt("berserker_sword", "tier3-stacks", 10);

        int speedAmp, speedDur;
        double atkBonus;

        if (current >= tier3) {
            speedAmp  = wm.getEffectInt("berserker_sword", "tier3-speed-amplifier", 2);
            speedDur  = wm.getEffectInt("berserker_sword", "tier3-speed-duration-ticks", 100);
            atkBonus  = wm.getEffectDouble("berserker_sword", "tier3-atk-bonus", 0.30);

            int weakChance = wm.getEffectInt("berserker_sword", "tier3-weakness-chance", 20);
            if (target != null && ThreadLocalRandom.current().nextInt(100) < weakChance)
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));

            spawnBerserkerParticles(attacker, 3);

        } else if (current >= tier2) {
            speedAmp  = wm.getEffectInt("berserker_sword", "tier2-speed-amplifier", 1);
            speedDur  = wm.getEffectInt("berserker_sword", "tier2-speed-duration-ticks", 100);
            atkBonus  = wm.getEffectDouble("berserker_sword", "tier2-atk-bonus", 0.15);
            spawnBerserkerParticles(attacker, 2);

        } else if (current >= tier1) {
            speedAmp  = wm.getEffectInt("berserker_sword", "tier1-speed-amplifier", 0);
            speedDur  = wm.getEffectInt("berserker_sword", "tier1-speed-duration-ticks", 100);
            atkBonus  = wm.getEffectDouble("berserker_sword", "tier1-atk-bonus", 0.05);
            spawnBerserkerParticles(attacker, 1);
        } else return;

        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDur, speedAmp), true);

        // Modifier l'attribut ATK dynamiquement (API 1.21+)
        AttributeInstance atk = attacker.getAttribute(Attribute.ATTACK_DAMAGE);
        if (atk != null) {
            // Retirer l'ancien modifier s'il existe
            atk.getModifiers().stream()
                    .filter(m -> BERSERKER_ATK_KEY.equals(m.getKey()))
                    .findFirst()
                    .ifPresent(atk::removeModifier);

            double base = atk.getBaseValue();
            AttributeModifier modifier = new AttributeModifier(
                    BERSERKER_ATK_KEY,
                    base * atkBonus,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
            );
            atk.addModifier(modifier);
        }

        // Action bar : afficher le niveau de rage
        String bar = ChatColor.RED + "⚔ Rage : " + current + "/" + maxStacks;
        attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar));
    }

    private void spawnBerserkerParticles(Player p, int level) {
        int count = level * 5;
        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), count, 0.3, 0.5, 0.3, 0.01);
    }

    // ══════════════════════════════════════════════════════════════
    // #6 — Épée du Jugement Piglin
    // ══════════════════════════════════════════════════════════════
    private void handleJudgment(Player attacker, LivingEntity target, EntityDamageByEntityEvent event) {
        if (target == null) return;
        if (!(target instanceof InventoryHolder holder)) return;

        double goldCount = 0;
        ItemStack[] contents = holder.getInventory().getStorageContents();
        for (ItemStack item : contents) {
            if (item == null) continue;
            goldCount += switch (item.getType()) {
                case GOLD_INGOT      -> item.getAmount();
                case GOLD_NUGGET     -> item.getAmount() / 9.0;
                case GOLDEN_APPLE    -> item.getAmount();
                case ENCHANTED_GOLDEN_APPLE -> item.getAmount() * 2;
                case GOLD_BLOCK      -> item.getAmount() * 9;
                default -> 0;
            };
        }

        double perGold  = wm.getEffectDouble("piglin_judgment_sword", "damage-per-gold", 0.3);
        double maxBonus = wm.getEffectDouble("piglin_judgment_sword", "max-bonus-damage", 15.0);
        double bonus    = Math.min(goldCount * perGold, maxBonus);

        // Bonus ender chest
        boolean hasEnderChest = false;
        for (ItemStack item : contents)
            if (item != null && item.getType() == Material.ENDER_CHEST) { hasEnderChest = true; break; }

        if (hasEnderChest)
            bonus += wm.getEffectDouble("piglin_judgment_sword", "ender-chest-bonus", 2.0);

        event.setDamage(event.getDamage() + bonus);

        // Particules or
        if (bonus > 0) {
            int range = plugin.getConfig().getInt("settings.particle-range", 16);
            int particleCount = (int) Math.min(bonus * 2, 30);
            target.getWorld().getNearbyPlayers(target.getLocation(), range)
                    .forEach(p -> p.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0),
                            particleCount, 0.3, 0.5, 0.3, Material.GOLD_BLOCK.createBlockData()));
        }

        // Son tonnerre si gros bonus
        double thunderThreshold = wm.getEffectDouble("piglin_judgment_sword", "thunder-threshold", 10.0);
        if (bonus >= thunderThreshold)
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);

        // Message attaquant
        String prefix = plugin.getConfig().getString("settings.prefix", "&8[&6NetherWeapons&8] &r");
        int displayGold = (int) goldCount;
        attacker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                prefix + "&6[Jugement] &7Ta cible possède &e" + displayGold + " &7lingots d'or ! (+&c"
                        + String.format("%.1f", bonus) + " dmg&7)"));
    }

    // ══════════════════════════════════════════════════════════════
    // #7 — Bâton du Trickster
    // ══════════════════════════════════════════════════════════════
    private void handleTrickster(Player attacker, LivingEntity target) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        String prefix = plugin.getConfig().getString("settings.prefix", "&8[&6NetherWeapons&8] &r");

        int invisChance  = wm.getEffectInt("trickster_staff", "invisibility-chance", 20);
        int poisonChance = wm.getEffectInt("trickster_staff", "poison-chance", 20);
        int blindChance  = wm.getEffectInt("trickster_staff", "blindness-chance", 15);
        int levChance    = wm.getEffectInt("trickster_staff", "levitation-chance", 15);
        int confChance   = wm.getEffectInt("trickster_staff", "confusion-chance", 10);
        int speedChance  = wm.getEffectInt("trickster_staff", "speed-chance", 10);

        // Seuils cumulatifs
        int t1 = invisChance;
        int t2 = t1 + poisonChance;
        int t3 = t2 + blindChance;
        int t4 = t3 + levChance;
        int t5 = t4 + confChance;
        int t6 = t5 + speedChance;

        String actionMsg;

        if (roll < t1) {
            int dur = wm.getEffectInt("trickster_staff", "invisibility-duration-ticks", 40);
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, dur, 0));
            actionMsg = "&7> &dInvisibilité !";

        } else if (roll < t2 && target != null) {
            int dur = wm.getEffectInt("trickster_staff", "poison-duration-ticks", 80);
            int amp = wm.getEffectInt("trickster_staff", "poison-amplifier", 1);
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, dur, amp));
            actionMsg = "&7> &2Poison sur la cible !";

        } else if (roll < t3 && target != null) {
            int dur = wm.getEffectInt("trickster_staff", "blindness-duration-ticks", 40);
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, dur, 0));
            actionMsg = "&7> &8Cécité sur la cible !";

        } else if (roll < t4 && target != null) {
            int dur = wm.getEffectInt("trickster_staff", "levitation-duration-ticks", 20);
            target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, dur, 0));
            actionMsg = "&7> &fLévitation sur la cible !";

        } else if (roll < t5 && target != null) {
            int dur = wm.getEffectInt("trickster_staff", "confusion-duration-ticks", 60);
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, dur, 2));
            actionMsg = "&7> &5Confusion sur la cible !";

        } else if (roll < t6) {
            int dur = wm.getEffectInt("trickster_staff", "speed-duration-ticks", 60);
            int amp = wm.getEffectInt("trickster_staff", "speed-amplifier", 1);
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, amp));
            actionMsg = "&7> &aVitesse !";

        } else {
            actionMsg = "&7... Raté !";
        }

        attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.translateAlternateColorCodes('&', "&d[Trickster] " + actionMsg)));
    }

    // ══════════════════════════════════════════════════════════════
    // Nettoyage à la déconnexion
    // ══════════════════════════════════════════════════════════════
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cm.cleanup(event.getPlayer().getUniqueId());
    }
}