package cc.battleroyale.commands;

import cc.battleroyale.BattleRoyale;
import de.jeff_media.chestsort.api.ChestSortEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RankCommand implements CommandExecutor, Listener {
    private final Plugin plugin = BattleRoyale.getProvidingPlugin(this.getClass());
    public static Map<Player, Double> PlayerDamage = new HashMap<>();
    public static Map<Player, Integer> PlayerKilled = new HashMap<>();
    public static Map<World, Long> RankStartTime = new HashMap<>();
    public static Map<Player, Long> SurviveTime = new HashMap<>();
    public static Set<UUID> LeftPlayer = new HashSet<>();
    public static Map<World, Player> Champion = new HashMap<>();
    public static Map<Player, Integer> PlayerKilledChampion = new HashMap<>();
    public static Map<Player, BukkitTask> UsingSurvivalItem = new HashMap<>();
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            if (args[0].equals("result")) {
                World world = sender.getServer().getWorld(args[1]);
                world.getPlayers().forEach((e) -> GetResult(e, world));
            }
            else if (args[0].equals("start")) {
                World world = sender.getServer().getWorld(args[1]);
                long time = System.currentTimeMillis();
                RankStartTime.put(world, time);
            }
            else if (args[0].equals("stop")) {
                World world = sender.getServer().getWorld(args[1]);
                world.getPlayers().forEach((e) -> {
                    if (!SurviveTime.containsKey(e)) {
                        SurviveTime.put(e, System.currentTimeMillis() - RankStartTime.get(world));
                    }
                });
                RankStartTime.remove(world);
            }
            else if (args[0].equals("champion")) {
                World world = sender.getServer().getWorld(args[1]);
                List<Player> players = world.getPlayers();
                Random random = new Random();
                int index = random.nextInt(players.size());
                Player HighestPermissionPlayer = players.get(index);
                for (Player e : players) {
                    if (e.hasPermission("apex.champion")) {
                        plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "lp user " + e.getName() + " permission unset apex.champion");
                        HighestPermissionPlayer = e;
                        break;
                    }
                }
                for (Player e : players) {
                    e.sendMessage("§3§l向你介紹你的冠軍: §6§l" + HighestPermissionPlayer.getName());
                }
                Champion.put(world, HighestPermissionPlayer);
            }
            else if (args[0].equals("init")) {
                World world = sender.getServer().getWorld(args[1]);
                ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.displayName(Component.text("§c該槽位已被鎖定"));
                itemMeta.setCustomModelData(30);
                itemStack.setItemMeta(itemMeta);
                world.getPlayers().forEach((e) -> {
                    e.setAbsorptionAmount(0);
                    PlayerDamage.remove(e);
                    PlayerKilled.remove(e);
                    SurviveTime.remove(e);
                    PlayerKilledChampion.remove(e);
                    for (int i=9; i<=17; i++) {
                        e.getInventory().setItem(i, itemStack);
                    }
                });
                RankStartTime.remove(world);
                Champion.remove(world);
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
                damage += PlayerDamage.get(player);
            }
            PlayerDamage.put(player, damage);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity().getWorld().getName().startsWith("Rank")) {
            Player dead = event.getEntity();
            World world = dead.getWorld();
            SurviveTime.put(dead, System.currentTimeMillis() - RankStartTime.get(world));

            Player killer = event.getEntity().getKiller();
            int kills = 1;
            if (PlayerKilled.containsKey(killer)) {
                kills += PlayerKilled.get(killer);
            }
            PlayerKilled.put(killer, kills);

            if (Champion.get(world).equals(dead)) {
                List<String> SoundList = new ArrayList<>();
                Random random = new Random();
                SoundList.add("championeliminated1");
                SoundList.add("championeliminated2");
                int index = random.nextInt(SoundList.size());
                world.getPlayers().forEach((e) -> {
                    e.sendMessage("§c§l注意 §3§l冠軍已被消滅");
                    e.playSound(e.getLocation(), SoundList.get(index), 1.0f, 1.0f);
                });
                PlayerKilledChampion.put(killer, 1);
            }

            HaveChampion(world);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = event.getFrom();
        if (RankStartTime.containsKey(world)) {
            player.sendMessage("§c§l你中途退出了排位賽, 受到了中離懲罰");
            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "aleague removepoints " + player.getName() + " 100");
            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "md setcooldown Rank_Christmas " + player.getName() + " 10m");
        }

        if (world.getName().startsWith("Rank")) {
            HaveChampion(world);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world.getName().startsWith("Rank") && RankStartTime.containsKey(world)) {
            HaveChampion(world);
            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "aleague removepoints " + player.getName() + " 100");
            LeftPlayer.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (LeftPlayer.contains(player.getUniqueId())) {
            player.sendMessage("§c§l你中途退出了排位賽, 受到了中離懲罰");
            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "md setcooldown Rank_Christmas " + player.getName() + " 10m");
            LeftPlayer.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        World world = player.getWorld();
        int slot = event.getSlot();
        if (world.getName().startsWith("Rank") && player.getGameMode().equals(GameMode.SURVIVAL)) {
            if ((slot >= 9 && slot <= 17) || UsingSurvivalItem.containsKey(player)) {
                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }

    @EventHandler
    public void onChestSort(ChestSortEvent event) {
        Player player = (Player) event.getPlayer();
        try {
            World world = player.getWorld();
            if (world.getName().startsWith("Rank") && player.getGameMode().equals(GameMode.SURVIVAL)) {
                event.setCancelled(true);
            }
        }
        catch (Exception ignored) { }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        ItemStack item = event.getItem();
        if (item != null && (world.startsWith("C") || world.startsWith("Rank")) && player.getGameMode().equals(GameMode.SURVIVAL)) {
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta.hasDisplayName()) {
                String itemName = LegacyComponentSerializer.legacyAmpersand().serialize(itemMeta.displayName());
                switch (itemName) {
                    case "&b小型防護罩":
                        PutUsingSurvivalItem(player, 60L);
                        break;
                    case "&b大型防護罩":
                    case "&b小型治療包":
                        PutUsingSurvivalItem(player, 100L);
                        break;
                    case "&b大型治療包":
                        PutUsingSurvivalItem(player, 160L);
                        break;
                    case "&5鳳凰治療包":
                        PutUsingSurvivalItem(player, 200L);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (UsingSurvivalItem.containsKey(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            World world = player.getWorld();
            Location location = event.getEntity().getLocation();
            if ((world.getName().startsWith("Ch") || world.getName().startsWith("Rank")) && event.getEntity().getName().equals("Snowball")) {
                Snowball snowball = world.spawn(location, Snowball.class);
                ItemStack snowballItem = new ItemStack(Material.SNOWBALL);
                ItemMeta itemMeta = snowballItem.getItemMeta();
                itemMeta.setCustomModelData(1);
                snowballItem.setItemMeta(itemMeta);
                snowball.setItem(snowballItem);
                snowball.setGravity(false);
                snowball.setVelocity(new Vector(0, 0, 0));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Collection<Player> entities = location.getNearbyEntitiesByType(Player.class, 5);
                        world.spawnParticle(Particle.EXPLOSION_LARGE, location, 20, 2, 2, 2, 0);
                        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                        for (Player entity : entities) {
                            entity.damage(10);
                            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30,2, false, false, false));
                        }
                        snowball.remove();
                    }
                }.runTaskLater(plugin, 80L);
            }
        }
    }

    public void PutUsingSurvivalItem(Player player, long intervalTicks) {
        BukkitTask timer = new BukkitRunnable() {
            @Override
            public void run() {
                UsingSurvivalItem.remove(player);
            }
        }.runTaskLater(plugin, intervalTicks);
        UsingSurvivalItem.put(player, timer);
    }

    public void HaveChampion(World world) {
        int SurviveAmount = 0;
        for (Player e : world.getPlayers()) {
            if (e.getGameMode().equals(GameMode.SURVIVAL)) {
                SurviveAmount++;
            }
        }
        if (SurviveAmount <= 1) {
            world.getPlayers().forEach((e) -> {
                e.playSound(e.getLocation(), "winner", 1.0f, 1.0f);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        e.playSound(e.getLocation(), "wehavechampion", 1.0f, 1.0f);
                    }
                }.runTaskLater(plugin, 310L);
                e.sendMessage("§3§l冠軍已出爐, 正在結算排位分數");
                GetResult(e, world);
                e.sendMessage("§3§l排位結算完成! 可使用 §b§l[§e§l/leave§b§l] §3§l離開");
            });
            RankStartTime.remove(world);
        }
    }

    public void GetResult(Player e, World world) {
        if (RankStartTime.containsKey(world)) {
            int RP = 0;
            e.sendMessage("§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            e.sendMessage("§3§l排位結算");
            e.sendMessage("");
            if (PlayerDamage.containsKey(e)) {
                int damage = (int) Math.round(PlayerDamage.get(e));
                int score = (damage < 625) ? damage / 25 : (int) Math.sqrt(damage);
                RP += score;
                e.sendMessage("§3§l造成的傷害: §6§l" + damage + " (" + score + " RP)");
                PlayerDamage.remove(e);
            }
            if (PlayerKilled.containsKey(e)) {
                int kills = PlayerKilled.get(e);
                RP += kills * 10;
                e.sendMessage("§3§l擊殺數: §6§l" + kills + " (" + kills * 10 + " RP)");
                PlayerKilled.remove(e);
            }
            if (PlayerKilledChampion.containsKey(e)) {
                int kills = PlayerKilledChampion.get(e);
                RP += kills * 15;
                e.sendMessage("§3§l擊殺冠軍: §6§l" + kills + " (" + kills * 15 + " RP)");
                PlayerKilledChampion.remove(e);
            }
            if (SurviveTime.containsKey(e)) {
                int seconds = (int) (SurviveTime.get(e) / 1000);
                RP += seconds / 45;
                e.sendMessage("§3§l存活時間: §6§l" + seconds + " (" + seconds / 45 + " RP)");
                SurviveTime.remove(e);
            }
            e.sendMessage("");
            e.sendMessage("§3§l總計: §6§l" + RP + "§3§l RP");
            if (RP > 0) {
                plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "aleague addpoints " + e.getName() + " " + RP);
            }
            e.sendMessage("§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }
    }
}
