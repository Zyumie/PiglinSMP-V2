package fr.zyumie.PiglinSMP.managers;

import fr.zyumie.PiglinSMP.Main;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Main plugin;

    // cooldown simple : "weaponId:uuid" -> timestamp ms
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    // stacks (poison, berserker) : "weaponId:uuid" -> valeur
    private final Map<String, Integer> stacks = new ConcurrentHashMap<>();

    // tâches de reset programmées : clé -> BukkitTask
    private final Map<String, BukkitTask> resetTasks = new ConcurrentHashMap<>();

    public CooldownManager(Main plugin) {
        this.plugin = plugin;
    }

    // ── Cooldowns ──────────────────────────────────────────────

    public boolean isOnCooldown(String weaponId, UUID uuid) {
        String key = weaponId + ":" + uuid;
        Long last = cooldowns.get(key);
        if (last == null) return false;
        // le cooldown est stocké en ms, on compare à now
        return System.currentTimeMillis() < last;
    }

    /**
     * @param ticks durée du cooldown en ticks (20 ticks = 1s)
     */
    public void setCooldown(String weaponId, UUID uuid, int ticks) {
        String key = weaponId + ":" + uuid;
        cooldowns.put(key, System.currentTimeMillis() + (ticks * 50L));
    }

    // ── Stacks ─────────────────────────────────────────────────

    public int getStack(String weaponId, UUID uuid) {
        return stacks.getOrDefault(weaponId + ":" + uuid, 0);
    }

    public int incrementStack(String weaponId, UUID uuid, int max) {
        String key = weaponId + ":" + uuid;
        int val = Math.min(stacks.getOrDefault(key, 0) + 1, max);
        stacks.put(key, val);
        return val;
    }

    public void resetStack(String weaponId, UUID uuid) {
        stacks.remove(weaponId + ":" + uuid);
    }

    /**
     * Planifie (ou replanifie) un reset de stack après `ticks` ticks.
     */
    public void scheduleStackReset(String weaponId, UUID uuid, int ticks) {
        String key = weaponId + ":" + uuid;

        // Annuler le reset précédent s'il existe
        BukkitTask old = resetTasks.remove(key);
        if (old != null) old.cancel();

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            stacks.remove(key);
            resetTasks.remove(key);
        }, ticks);

        resetTasks.put(key, task);
    }

    // ── Nettoyage ──────────────────────────────────────────────

    /**
     * Nettoie toutes les données d'un joueur (appelé sur PlayerQuitEvent).
     */
    public void cleanup(UUID uuid) {
        String suffix = ":" + uuid;
        cooldowns.keySet().removeIf(k -> k.endsWith(suffix));
        stacks.keySet().removeIf(k -> k.endsWith(suffix));

        resetTasks.entrySet().removeIf(e -> {
            if (e.getKey().endsWith(suffix)) {
                e.getValue().cancel();
                return true;
            }
            return false;
        });
    }

    /**
     * Annule toutes les tâches (appelé à l'onDisable).
     */
    public void cancelAll() {
        resetTasks.values().forEach(BukkitTask::cancel);
        resetTasks.clear();
    }

    /**
     * Accès direct à la map des stacks (pour incréments personnalisés).
     */
    public Map<String, Integer> stacks() { return stacks; }
}