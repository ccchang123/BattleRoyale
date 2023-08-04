package cc.battleroyale.commands.ApexSkills;

import cc.battleroyale.BattleRoyale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Ash implements CommandExecutor, Listener {
    private final Plugin plugin = BattleRoyale.getProvidingPlugin(this.getClass());
    public static Map<Location, Location> Portal = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            Player player = Bukkit.getPlayer(args[0]);
            Location now = player.getLocation();
            Block looking = player.getTargetBlockExact(200);
            if (looking != null) {
                Location lookingAt = looking.getLocation();
                lookingAt = new Location(lookingAt.getWorld(), lookingAt.getX(), lookingAt.getY() + 1, lookingAt.getZ(), now.getYaw(), now.getPitch());
                player.teleport(lookingAt);
                Portal.put(now, lookingAt);
                new BukkitRunnable() {
                    int count = 0;

                    @Override
                    public void run() {
                        count++;
                        if (count > 200) {
                            Portal.remove(now);
                            cancel();
                            return;
                        }
                        now.getWorld().spawnParticle(Particle.PORTAL, now.getX(), now.getY(), now.getZ(), 50, 0.25, 1, 0.25, 0.4);
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!Portal.isEmpty()) {
            Player player = event.getPlayer();
            Portal.forEach((key, value) -> {
                if (player.getWorld().equals(key.getWorld()) && player.getLocation().distance(key) <= 1) {
                    player.playSound(player, "portalteleport", 1.0f, 1.0f);
                    player.teleport(value);
                }
            });
        }
    }
}
