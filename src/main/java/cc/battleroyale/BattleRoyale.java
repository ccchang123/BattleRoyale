package cc.battleroyale;

import cc.battleroyale.commands.ApexSkills.Revenant;
import cc.battleroyale.commands.ApexSkills.Vantage;
import cc.battleroyale.commands.BattleRoyaleCommand;
import cc.battleroyale.commands.RankCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public final class BattleRoyale extends JavaPlugin implements Listener {
    private static BattleRoyale plugin = null;
    private Map<Player, BukkitTask> OutSideRingSound = new HashMap<>();
    private Map<Player, BukkitTask> NearRingSound = new HashMap<>();
    @Override
    public void onEnable() {
        plugin = this;
        plugin.getLogger().info("大逃殺 >> 已啟用大逃殺插件");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Vantage(), this);
        getServer().getPluginManager().registerEvents(new Revenant(), this);
        getServer().getPluginManager().registerEvents(new RankCommand(), this);
        Bukkit.getScheduler().runTaskTimer(plugin, OutsideRing, 0, 30L);
        getCommand("battleroyale").setExecutor(new BattleRoyaleCommand());
        getCommand("vantage").setExecutor(new Vantage());
        getCommand("revenant").setExecutor(new Revenant());
        getCommand("rank").setExecutor(new RankCommand());
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("大逃殺 >> 已停用大逃殺插件");
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("BattleRoyale.admin")) {
            player.sendMessage("§3§l大逃殺 §7>> §a已啟用大逃殺插件");
        }
    }

    @EventHandler
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        Player player = (Player) event.getEntity();
        String world = player.getWorld().getName();
        if (world.startsWith("C") || world.startsWith("Rank")) {
            try {
                Material Item = player.getInventory().getItem(38).getType();
                if (Item != Material.ELYTRA && !player.isOnGround()) {
                    event.setCancelled(true);
                }
            } catch (NullPointerException e) {
                if (!player.isOnGround()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        WorldBorder worldBorder = world.getWorldBorder();
        if (!world.getName().startsWith("world")) {
            double distance = GetPlayerDistanceToBorder(player);
            if (worldBorder.isInside(player.getLocation())) {
                player.stopSound("outsidering");
                StopSoundPeriodically(player, OutSideRingSound);
            }
            else {
                if (!OutSideRingSound.containsKey(player)) {
                    PlaySoundPeriodically(player, "outsidering", 1060L, OutSideRingSound);
                }
            }
            if (distance <= 0.3) {
                List<String> SoundList = new ArrayList<>();
                Random random = new Random();
                SoundList.add("passring1");
                SoundList.add("passring2");
                SoundList.add("passring3");
                SoundList.add("passring4");
                int index = random.nextInt(SoundList.size());
                player.playSound(player.getLocation(), SoundList.get(index), 1.0f, 1.0f);
            }
            else if (distance <= 10) {
                if (!NearRingSound.containsKey(player) && worldBorder.isInside(player.getLocation())) {
                    PlaySoundPeriodically(player, "closering", 900L, NearRingSound);
                }
            }
            else {
                player.stopSound("closering");
                StopSoundPeriodically(player, NearRingSound);
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = event.getFrom();
        if (!world.getName().startsWith("world")) {
            StopSoundPeriodically(player, OutSideRingSound);
            StopSoundPeriodically(player, NearRingSound);
        }
    }

    Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(250));
    Runnable OutsideRing = () -> getServer().getOnlinePlayers().forEach((e) -> {
        // 2000 750 375 185 90 25 1
        String world = e.getWorld().getName();
        if (world.startsWith("C") || world.startsWith("Rank")) {
            if (e.isFlying() && e.getGameMode() == GameMode.SURVIVAL) {
                Title title = Title.title(Component.text("§c不要飛行!"), Component.text(""), times);
                e.showTitle(title);
                RemoveHealth(e, 2);
                e.playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
            }
            if (!e.getWorld().getWorldBorder().isInside(e.getLocation()) && e.getGameMode() == GameMode.SURVIVAL) {
                Collection<ArmorStand> entities = e.getLocation().getNearbyEntitiesByType(ArmorStand.class, 5);
                boolean InHeatShield = false;
                double HeatShieldHealth = 0;
                if (!entities.isEmpty()) {
                    for (ArmorStand entity : entities) {
                        if (!entity.isVisible()) {
                            InHeatShield = true;
                            HeatShieldHealth = entity.getHealth();
                            break;
                        }
                    }
                }
                if (InHeatShield) {
                    Title title = Title.title(Component.text("§c回到邊界中!"), Component.text("§c" + (int) HeatShieldHealth + " %"), times);
                    e.showTitle(title);
                }
                else {
                    if (e.getGameMode() == GameMode.SURVIVAL) {
                        Title title = Title.title(Component.text("§c回到邊界中!"), Component.text(""), times);
                        e.showTitle(title);
                        double WorldBorderSize = e.getWorld().getWorldBorder().getSize();
                        if (WorldBorderSize >= 375) {
                            RemoveHealth(e, 0.05);
                        }
                        else if (WorldBorderSize >= 185) {
                            RemoveHealth(e, 0.1);
                        }
                        else if (WorldBorderSize >= 25) {
                            RemoveHealth(e, 0.2);
                        }
                        else if (WorldBorderSize < 25) {
                            RemoveHealth(e, 0.25);
                        }
                        e.playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                    }
                }
            }
        }
    });

    public void RemoveHealth(Player player, double amount) {
        double PlayerHealth = player.getHealth();
        double PlayerMaxHealth = player.getMaxHealth();
        amount = amount * PlayerMaxHealth;
        if (PlayerHealth > amount) {
            player.setHealth(PlayerHealth - amount);
        }
        else {
            player.setHealth(0.0);
        }
    }

    public double GetPlayerDistanceToBorder(Player player) {
        World world = player.getWorld();
        WorldBorder worldBorder = world.getWorldBorder();

        double playerX = player.getLocation().getX();
        double playerZ = player.getLocation().getZ();
        double centerX = worldBorder.getCenter().getX();
        double centerZ = worldBorder.getCenter().getZ();

        double borderSize = worldBorder.getSize() / 2.0;

        double distanceX = Math.abs(Math.abs(centerX - playerX) - borderSize);
        double distanceZ = Math.abs(Math.abs(centerZ - playerZ) - borderSize);

        if (worldBorder.isInside(player.getLocation())) {
            return Math.min(distanceX, distanceZ);
        }
        else {
            return Math.max(distanceX, distanceZ);
        }
    }

    public void PlaySoundPeriodically(Player player, String sound, long intervalTicks, Map<Player, BukkitTask> soundMap) {
        BukkitTask SoundPlayer = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                }
                else {
                    StopSoundPeriodically(player, soundMap);
                }
            }
        }.runTaskTimer(this, 0, intervalTicks);
        soundMap.put(player, SoundPlayer);
    }

    public void StopSoundPeriodically(Player player, Map<Player, BukkitTask> soundMap) {
        BukkitTask Task = soundMap.get(player);
        if (Task != null) {
            Task.cancel();
            soundMap.remove(player);
        }
    }
}
