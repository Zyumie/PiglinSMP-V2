package fr.zyumie.PiglinSMP;

import fr.zyumie.PiglinSMP.commands.PWeaponsCommand;
import fr.zyumie.PiglinSMP.listeners.BarterListener;
import fr.zyumie.PiglinSMP.listeners.CombatListener;
import fr.zyumie.PiglinSMP.listeners.ProjectileListener;
import fr.zyumie.PiglinSMP.managers.CooldownManager;
import fr.zyumie.PiglinSMP.managers.WeaponManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private WeaponManager weaponManager;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;

        // Sauvegarde la config par défaut si absente
        saveDefaultConfig();

        // Initialisation des managers
        this.cooldownManager = new CooldownManager(this);
        this.weaponManager   = new WeaponManager(this);

        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(new BarterListener(this),     this);
        getServer().getPluginManager().registerEvents(new CombatListener(this),     this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(this), this);

        // Commande admin
        PWeaponsCommand cmd = new PWeaponsCommand(this);
        getCommand("pweapons").setExecutor(cmd);
        getCommand("pweapons").setTabCompleter(cmd);

        getLogger().info("NetherWeapons activé ! " + weaponManager.getWeaponCount() + " armes chargées.");
    }

    @Override
    public void onDisable() {
        cooldownManager.cancelAll();
        getLogger().info("NetherWeapons désactivé.");
    }

    public static Main getInstance() { return instance; }
    public WeaponManager getWeaponManager()         { return weaponManager; }
    public CooldownManager getCooldownManager()     { return cooldownManager; }
}