package cc.battleroyale;

import cc.battleroyale.commands.ApexSkills.Revenant;
import cc.battleroyale.commands.ApexSkills.Vantage;
import cc.battleroyale.commands.BattleRoyaleCommand;
import cc.battleroyale.commands.RankCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
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
    private static Map<Player, BukkitTask> OutSideRingSound = new HashMap<>();
    private static Map<Player, BukkitTask> NearRingSound = new HashMap<>();
    private static Map<Player, BukkitTask> FlyingPlayer = new HashMap<>();
    private static Map<Player, BukkitTask> OutsideRingPlayer = new HashMap<>();
    private static List<String> PassRingSoundList = new ArrayList<>();
    private static final Random random = new Random();
    Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(250));
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

        PassRingSoundList.add("passring1");
        PassRingSoundList.add("passring2");
        PassRingSoundList.add("passring3");
        PassRingSoundList.add("passring4");
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
        if (world.getName().startsWith("C") || world.getName().startsWith("Rank")) {
            if (player.isFlying() && player.getGameMode().equals(GameMode.SURVIVAL)) {
                if (!FlyingPlayer.containsKey(player)) {
                    BukkitTask DamageToFlyingPlayer = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                Title title = Title.title(Component.text("§c不要飛行!"), Component.text(""), times);
                                player.showTitle(title);
                                RemoveHealth(player, 0.1);
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                            }
                            else {
                                StopPeriodically(player, FlyingPlayer);
                            }
                        }
                    }.runTaskTimer(this, 0, 30);
                    FlyingPlayer.put(player, DamageToFlyingPlayer);
                }
            }
            else {
                StopPeriodically(player, FlyingPlayer);
            }


            if (worldBorder.isInside(player.getLocation())) {
                player.stopSound("outsidering");
                StopPeriodically(player, OutSideRingSound);
                double distance = GetPlayerDistanceToBorder(player);
                if (distance <= 0.3) {
                    int index = random.nextInt(PassRingSoundList.size());
                    player.playSound(player.getLocation(), PassRingSoundList.get(index), 1.0f, 1.0f);
                }
                else if (distance <= 15) {
                    if (!NearRingSound.containsKey(player)) {
                        PlaySoundPeriodically(player, "closering", 900L, NearRingSound);
                    }
                }
                else {
                    player.stopSound("closering");
                    StopPeriodically(player, NearRingSound);
                }
            }
            else {
                player.stopSound("closering");
                StopPeriodically(player, NearRingSound);
                if (!OutSideRingSound.containsKey(player)) {
                    PlaySoundPeriodically(player, "outsidering", 1060L, OutSideRingSound);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = event.getFrom();
        if (!world.getName().startsWith("world")) {
            StopPeriodically(player, OutSideRingSound);
            StopPeriodically(player, NearRingSound);
        }
    }

    Runnable OutsideRing = () -> getServer().getOnlinePlayers().forEach((e) -> {
        // 2000 750 375 185 90 25 1
        String world = e.getWorld().getName();
        if (world.startsWith("C") || world.startsWith("Rank")) {
            if (!e.getWorld().getWorldBorder().isInside(e.getLocation()) && e.getGameMode() == GameMode.SURVIVAL) {
                Collection<ArmorStand> entities = e.getLocation().getNearbyEntitiesByType(ArmorStand.class, 5);
                boolean InHeatShield = false;
                if (!entities.isEmpty()) {
                    for (ArmorStand entity : entities) {
                        if (!entity.isVisible()) {
                            InHeatShield = true;
                            double HeatShieldHealth = entity.getHealth();
                            Title title = Title.title(Component.text("§c回到邊界中!"), Component.text("§c" + (int) HeatShieldHealth + " %"), times);
                            e.showTitle(title);
                            break;
                        }
                    }
                }
                if (!InHeatShield && e.getGameMode() == GameMode.SURVIVAL) {
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
        Location PlayerLocation = player.getLocation();
        WorldBorder worldBorder = player.getWorld().getWorldBorder();
        Location WorldBorderCenter = worldBorder.getCenter();

        double borderSize = worldBorder.getSize() / 2.0;

        double distanceX = borderSize - Math.abs(WorldBorderCenter.getX() - PlayerLocation.getX());
        double distanceZ = borderSize - Math.abs(WorldBorderCenter.getZ() - PlayerLocation.getZ());

        return Math.min(distanceX, distanceZ);
    }

    public void PlaySoundPeriodically(Player player, String sound, long intervalTicks, Map<Player, BukkitTask> soundMap) {
        BukkitTask SoundPlayer = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                }
                else {
                    StopPeriodically(player, soundMap);
                }
            }
        }.runTaskTimer(this, 0, intervalTicks);
        soundMap.put(player, SoundPlayer);
    }

    public void StopPeriodically(Player player, Map<Player, BukkitTask> Map) {
        BukkitTask Task = Map.get(player);
        if (Task != null) {
            Task.cancel();
            Map.remove(player);
        }
    }
}
