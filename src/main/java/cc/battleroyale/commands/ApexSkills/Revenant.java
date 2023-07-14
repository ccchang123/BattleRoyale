package cc.battleroyale.commands.ApexSkills;

import cc.battleroyale.BattleRoyale;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Revenant implements CommandExecutor, Listener {
    private final Plugin plugin = BattleRoyale.getProvidingPlugin(this.getClass());
    public static Map<Player, BukkitTask> DeathTotem = new HashMap<>();
    public static Map<Player, Location> TotemLocation = new HashMap<>();
    public static BossBar bossbar = BossBar.bossBar(Component.text("§c已進入死亡保護"), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            Player player = Bukkit.getPlayer(args[0]);
            if (player != null) {
                BukkitTask timer = new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.teleport(TotemLocation.get(player));
                        player.playSound(player.getLocation(), "returntototem", 1.0f, 1.0f);
                        DeathTotem.remove(player);
                        TotemLocation.remove(player);
                        player.hideBossBar(bossbar);
                    }
                }.runTaskLater(plugin, 600L);
                DeathTotem.put(player, timer);
                TotemLocation.put(player, player.getLocation());
                player.showBossBar(bossbar);
            }
        }
        return true;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (TotemLocation.containsKey(player)) {
            event.setCancelled(true);
            player.teleport(TotemLocation.get(player));
            player.playSound(player.getLocation(), "returntototem", 1.0f, 1.0f);
            TotemLocation.remove(player);
            DeathTotem.remove(player);
            player.hideBossBar(bossbar);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (TotemLocation.containsKey(player)) {
            TotemLocation.remove(player);
            DeathTotem.remove(player);
            player.hideBossBar(bossbar);
        }
    }
}
