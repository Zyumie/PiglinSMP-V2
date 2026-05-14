package fr.zyumie.PiglinSMP.listeners;

import fr.zyumie.PiglinSMP.Main;
import fr.zyumie.PiglinSMP.managers.WeaponManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BarterListener implements Listener {

    private final Main plugin;
    private final WeaponManager weaponManager;

    public BarterListener(Main plugin) {
        this.plugin       = plugin;
        this.weaponManager = plugin.getWeaponManager();
    }

    @EventHandler
    public void onPiglinBarter(PiglinBarterEvent event) {

        for (String weaponId : weaponManager.getWeaponIds()) {
            double dropRate = weaponManager.getDropRate(weaponId);
            if (dropRate <= 0) continue;

            double roll = ThreadLocalRandom.current().nextDouble();
            if (roll < dropRate) {
                ItemStack weapon = weaponManager.getWeapon(weaponId);
                if (weapon == null) continue;

                boolean replaceDrops = plugin.getConfig()
                        .getBoolean("settings.replace-normal-drops", true);

                List<ItemStack> outcome = new ArrayList<>(event.getOutcome());
                if (replaceDrops) outcome.clear();
                outcome.add(weapon);
                event.getOutcome().clear();
                event.getOutcome().addAll(outcome);

                plugin.getLogger().fine("Piglin Bartering : arme droppée → " + weaponId);
                return; // Une seule arme par échange
            }
        }
    }
}