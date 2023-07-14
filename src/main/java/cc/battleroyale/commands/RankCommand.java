package cc.battleroyale.commands;

import cc.battleroyale.BattleRoyale;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class RankCommand implements CommandExecutor, Listener {
    private final Plugin plugin = BattleRoyale.getProvidingPlugin(this.getClass());
    public static Map<Player, Double> PlayerDamage = new HashMap<>();
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            if (args[0].equals("result")) {
                if (args[1].equals("damage")) {
                    Player player = Bukkit.getPlayer(args[2]);
                    if (PlayerDamage.containsKey(player)) {
                        int damage = (int) (PlayerDamage.get(player) / 15);
                        plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "aleague addpoints " + player.getName() + " " + damage);
                        PlayerDamage.remove(player);
                    }
                }
            }
        }
        return true;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if ((event.getDamager() instanceof Player) && event.getDamager().getWorld().getName().startsWith("Rank")) {
            Player player = (Player) event.getDamager();
            double damage = event.getDamage();
            if (PlayerDamage.containsKey(player)) {
                double OldDamage = PlayerDamage.get(player);
                PlayerDamage.put(player, damage + OldDamage);
            }
            else {
                PlayerDamage.put(player, damage);
            }
        }
    }
}
