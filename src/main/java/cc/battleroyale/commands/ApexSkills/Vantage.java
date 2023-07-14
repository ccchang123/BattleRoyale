package cc.battleroyale.commands.ApexSkills;

import cc.battleroyale.BattleRoyale;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.List;

public class Vantage implements CommandExecutor, Listener {
    private final Plugin plugin = BattleRoyale.getProvidingPlugin(this.getClass());
    public static Set<Player> TaggedByVantage = new HashSet<>();
    public static Map<Player, BukkitTask> Timers = new HashMap<>();
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            Player player = Bukkit.getPlayer(args[0]);
            if (player != null) {
                if (TaggedByVantage.contains(player)) {
                    BukkitTask timer = Timers.get(player);
                    if (timer != null) {
                        timer.cancel();
                    }
                }
                TaggedByVantage.add(player);
                BossBar bossbar = BossBar.bossBar(Component.text("§c已被標記"), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
                BukkitTask timer = new BukkitRunnable() {
                    @Override
                    public void run() {
                        TaggedByVantage.remove(player);
                        Timers.remove(player);
                        player.hideBossBar(bossbar);
                    }
                }.runTaskLater(plugin, 200L);
                Timers.put(player, timer);
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200,0, false, false, false));
                player.sendMessage("§c§l已被標記, 所受傷害變為 1.5 倍");
                List<String> SoundList = new ArrayList<>();
                Random random = new Random();
                SoundList.add("tagged1");
                SoundList.add("tagged2");
                int index = random.nextInt(SoundList.size());
                player.playSound(player.getLocation(), SoundList.get(index), 1.0f, 1.0f);
                player.showBossBar(bossbar);
            }
        }
        return true;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (TaggedByVantage.contains(player)) {
            event.setDamage(event.getDamage() * 1.5);
        }
    }
}
