package cc.battleroyale.commands.ApexSkills;

import cc.battleroyale.BattleRoyale;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Revenant implements CommandExecutor, Listener {
    private final Plugin plugin = BattleRoyale.getProvidingPlugin(this.getClass());
    public static Map<Player, BukkitTask> DeathTotem = new HashMap<>();
    public static Map<UUID, Location> TotemLocation = new HashMap<>();
    public static BossBar bossbar = BossBar.bossBar(Component.text("§c已進入死亡保護"), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            Player player = Bukkit.getPlayer(args[0]);
            if (player != null) {
                BukkitTask timer = new BukkitRunnable() {
                    @Override
                    public void run() {
                        UUID uuid = player.getUniqueId();
                        if (TotemLocation.containsKey(uuid)) {
                            player.playSound(player.getLocation(), "returntototem", 1.0f, 1.0f);
                        }
                        DeathTotem.remove(player);
                        TotemLocation.remove(uuid);
                        player.hideBossBar(bossbar);
                    }
                }.runTaskLater(plugin, 900L);
                DeathTotem.put(player, timer);
                TotemLocation.put(player.getUniqueId(), player.getLocation());
                player.showBossBar(bossbar);
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (TotemLocation.containsKey(uuid)) {
            event.setCancelled(true);
            player.teleportAsync(TotemLocation.get(uuid));
            player.playSound(player.getLocation(), "returntototem", 1.0f, 1.0f);
            TotemLocation.remove(uuid);
            DeathTotem.remove(player);
            player.hideBossBar(bossbar);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (TotemLocation.containsKey(uuid)) {
            TotemLocation.remove(uuid);
            DeathTotem.remove(player);
            player.hideBossBar(bossbar);
        }
    }
}
