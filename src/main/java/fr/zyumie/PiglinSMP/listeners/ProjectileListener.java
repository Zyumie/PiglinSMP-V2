package fr.zyumie.PiglinSMP.listeners;

import fr.zyumie.PiglinSMP.Main;
import fr.zyumie.PiglinSMP.managers.CooldownManager;
import fr.zyumie.PiglinSMP.managers.WeaponManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public class ProjectileListener implements Listener {

    private static final String META_KEY        = "nw_bow_type";
    private static final String META_SHOOTER    = "nw_bow_shooter";

    // Types d'effets possibles
    private enum BowEffect { EXPLOSIVE, FIRE, SLOW, NORMAL }

    private final Main plugin;
    private final WeaponManager       wm;
    private final CooldownManager     cm;

    public ProjectileListener(Main plugin) {
        this.plugin = plugin;
        this.wm     = plugin.getWeaponManager();
        this.cm     = plugin.getCooldownManager();
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;

        ItemStack bow = event.getBow();
        if (!"nether_unstable_bow".equals(wm.getWeaponId(bow))) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        // Choisir un effet aléatoire
        BowEffect effect = randomEffect();

        // Stocker l'effet dans les métadonnées de la flèche
        arrow.setMetadata(META_KEY,     new FixedMetadataValue(plugin, effect.name()));
        arrow.setMetadata(META_SHOOTER, new FixedMetadataValue(plugin, shooter.getUniqueId().toString()));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.hasMetadata(META_KEY)) return;

        String effectName = arrow.getMetadata(META_KEY).get(0).asString();
        BowEffect effect;
        try { effect = BowEffect.valueOf(effectName); }
        catch (IllegalArgumentException e) { return; }

        String shooterStr = arrow.hasMetadata(META_SHOOTER)
                ? arrow.getMetadata(META_SHOOTER).get(0).asString() : null;

        // Cooldown : 1 effet spécial par seconde par joueur
        if (shooterStr != null && effect != BowEffect.NORMAL) {
            java.util.UUID shooterId = java.util.UUID.fromString(shooterStr);
            int cdTicks = wm.getEffectInt("nether_unstable_bow", "cooldown-ticks", 20);
            if (cm.isOnCooldown("nether_unstable_bow", shooterId)) {
                arrow.removeMetadata(META_KEY, plugin);
                return;
            }
            cm.setCooldown("nether_unstable_bow", shooterId, cdTicks);
        }

        Entity hitEntity = event.getHitEntity();
        Location loc     = arrow.getLocation();

        switch (effect) {
            case EXPLOSIVE -> {
                float power = (float) wm.getEffectDouble("nether_unstable_bow", "explosion-power", 1.2);
                boolean exp = loc.getWorld().createExplosion(loc, power, false, false);
                // Annuler la destruction de blocs (no-grief)
                // Note : createExplosion avec setBreakBlocks false est géré par les paramètres
                // On utilise setBreakBlocks via l'event EntityExplodeEvent si besoin
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
            }
            case FIRE -> {
                int fireTicks = wm.getEffectInt("nether_unstable_bow", "fire-duration-ticks", 60);
                if (hitEntity instanceof LivingEntity le)
                    le.setFireTicks(fireTicks);
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 20, 0.3, 0.3, 0.3, 0.05);
            }
            case SLOW -> {
                int slowDur = wm.getEffectInt("nether_unstable_bow", "slow-duration-ticks", 60);
                int slowAmp = wm.getEffectInt("nether_unstable_bow", "slow-amplifier", 1);
                if (hitEntity instanceof LivingEntity le)
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDur, slowAmp));
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 15, 0.3, 0.3, 0.3, 0.01);
            }
            case NORMAL -> {
                // Rien — flèche normale
            }
        }

        // Nettoyage des métadonnées
        arrow.removeMetadata(META_KEY,     plugin);
        arrow.removeMetadata(META_SHOOTER, plugin);
    }

    private BowEffect randomEffect() {
        int roll = ThreadLocalRandom.current().nextInt(4);
        return switch (roll) {
            case 0 -> BowEffect.EXPLOSIVE;
            case 1 -> BowEffect.FIRE;
            case 2 -> BowEffect.SLOW;
            default -> BowEffect.NORMAL;
        };
    }
}