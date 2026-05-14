package fr.zyumie.PiglinSMP.commands;

import fr.zyumie.PiglinSMP.Main;
import fr.zyumie.PiglinSMP.managers.WeaponManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PWeaponsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final WeaponManager       wm;

    private static final List<String> SUB_COMMANDS = Arrays.asList("give", "reload", "list");

    public PWeaponsCommand(Main plugin) {
        this.plugin = plugin;
        this.wm     = plugin.getWeaponManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("settings.prefix", "&8[&6PiglinWeapons&8] &r"));

        if (!sender.hasPermission("piglinweapons.admin")) {
            sender.sendMessage(prefix + ChatColor.RED + "Tu n'as pas la permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, prefix);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── /nweapons give <weaponId> [player] ─────────────────
            case "give" -> {
                if (args.length < 2) {
                    sender.sendMessage(prefix + ChatColor.RED + "Usage : /pweapons give <weaponId> [joueur]");
                    return true;
                }

                String weaponId = args[1];
                ItemStack weapon = wm.getWeapon(weaponId);
                if (weapon == null) {
                    sender.sendMessage(prefix + ChatColor.RED + "Arme inconnue : &e" + weaponId);
                    sender.sendMessage(prefix + ChatColor.GRAY + "IDs valides : " +
                            String.join(", ", wm.getWeaponIds()));
                    return true;
                }

                // Cible : argument optionnel ou sender
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(prefix + ChatColor.RED + "Joueur introuvable : " + args[2]);
                        return true;
                    }
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(prefix + ChatColor.RED + "Spécifie un joueur : /nweapons give <id> <joueur>");
                        return true;
                    }
                    target = (Player) sender;
                }

                target.getInventory().addItem(weapon);
                target.sendMessage(prefix + ChatColor.GREEN + "Tu as reçu : " +
                        ChatColor.translateAlternateColorCodes('&',
                                plugin.getConfig().getString("weapons." + weaponId + ".display-name", weaponId)));

                if (!target.equals(sender))
                    sender.sendMessage(prefix + ChatColor.GREEN + "Arme &e" + weaponId +
                            " &adonnée à &e" + target.getName());
            }

            // ── /pweapons reload ────────────────────────────────────
            case "reload" -> {
                plugin.reloadConfig();
                wm.reload();
                sender.sendMessage(prefix + ChatColor.GREEN + "Configuration rechargée ! (" +
                        wm.getWeaponCount() + " armes)");
            }

            // ── /pweapons list ──────────────────────────────────────
            case "list" -> {
                sender.sendMessage(prefix + ChatColor.YELLOW + "Armes disponibles (" + wm.getWeaponCount() + ") :");
                for (String id : wm.getWeaponIds()) {
                    String name = ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("weapons." + id + ".display-name", id));
                    String rarity = ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("weapons." + id + ".rarity", "?"));
                    double rate = wm.getDropRate(id) * 100;
                    sender.sendMessage(ChatColor.GRAY + "  • " + name +
                            ChatColor.DARK_GRAY + " [" + rarity + ChatColor.DARK_GRAY + "] " +
                            ChatColor.GRAY + String.format("%.2f%%", rate));
                }
            }

            default -> sendHelp(sender, prefix);
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String prefix) {
        sender.sendMessage(prefix + ChatColor.YELLOW + "Commandes :");
        sender.sendMessage(ChatColor.GRAY + "  /pweapons give <id> [joueur]");
        sender.sendMessage(ChatColor.GRAY + "  /pweapons list");
        sender.sendMessage(ChatColor.GRAY + "  /pweapons reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("piglinweapons.admin")) return List.of();

        if (args.length == 1)
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        if (args.length == 2 && args[0].equalsIgnoreCase("give"))
            return wm.getWeaponIds().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

        if (args.length == 3 && args[0].equalsIgnoreCase("give"))
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());

        return List.of();
    }
}