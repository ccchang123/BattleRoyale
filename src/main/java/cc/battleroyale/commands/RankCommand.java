package cc.battleroyale.commands;

import cc.battleroyale.BattleRoyale;
import de.jeff_media.chestsort.api.ChestSortEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.ClickType;
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

import java.time.Duration;
import java.util.*;

public class RankCommand implements CommandExecutor, Listener {
    private final Plugin plugin = BattleRoyale.getProvidingPlugin(this.getClass());
    public static Map<Player, Double> PlayerDamage = new HashMap<>();
    public static Map<Player, Integer> PlayerKilled = new HashMap<>();
    public static Map<World, Long> RankStartTime = new HashMap<>();
    public static Map<Player, Long> SurviveTime = new HashMap<>();
    public static Set<UUID> LeftPlayer = new HashSet<>();
    public static Map<World, Player> Champion = new HashMap<>();
    public static Map<World, Player> KillLeader = new HashMap<>();
    public static Map<World, Double> MaxDamage = new HashMap<>();
    public static Map<Player, Integer> PlayerKilledChampion = new HashMap<>();
    public static Map<Player, Integer> PlayerKilledKillLeader = new HashMap<>();
    public static Map<Player, BukkitTask> UsingSurvivalItem = new HashMap<>();
    Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(250));

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            if (args[0].equals("result")) {
                World world = sender.getServer().getWorld(args[1]);
                world.getPlayers().forEach((e) -> GetResult(e, world));
                RankStartTime.remove(world);
            }
            else if (args[0].equals("start")) {
                World world = sender.getServer().getWorld(args[1]);
                long time = System.currentTimeMillis();
                RankStartTime.put(world, time);
            }
            else if (args[0].equals("stop")) {
                World world = sender.getServer().getWorld(args[1]);
                world.getPlayers().forEach((e) -> {
                    if (!SurviveTime.containsKey(e) && e.getGameMode().equals(GameMode.SURVIVAL)) {
                        SurviveTime.put(e, System.currentTimeMillis() - RankStartTime.get(world));
                    }
                });
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
                    e.sendMessage("§3§l擊殺冠軍獎勵: §6§l20 RP");
                }
                Champion.put(world, HighestPermissionPlayer);
            }
            else if (args[0].equals("init")) {
                World world = sender.getServer().getWorld(args[1]);
                world.getPlayers().forEach((e) -> {
                    PlayerDamage.remove(e);
                    PlayerKilled.remove(e);
                    SurviveTime.remove(e);
                    PlayerKilledChampion.remove(e);
                    PlayerKilledKillLeader.remove(e);
                });
                RankStartTime.remove(world);
                Champion.remove(world);
                KillLeader.remove(world);
                MaxDamage.remove(world);
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        World world = event.getDamager().getWorld();
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if ((world.getName().startsWith("Ch") || world.getName().startsWith("Rank"))) {
                String UsingSkill = PlaceholderAPI.setPlaceholders(player, "%mmocore_is_casting%");
                if (UsingSkill.equals("true")) {
                    event.setCancelled(true);
                }
            }
            if (world.getName().startsWith("Rank")) {
                if (!RankStartTime.containsKey(world)) {
                    event.setCancelled(true);
                }
                else {
                    double damage = event.getDamage();
                    if (PlayerDamage.containsKey(player)) {
                        damage += PlayerDamage.get(player);
                    }
                    PlayerDamage.put(player, damage);

                    if (damage >= 300) {
                        double maxDamage;
                        if (MaxDamage.containsKey(world)) {
                            maxDamage = MaxDamage.get(world);
                            if (damage >= maxDamage + 50) {
                                NewKillLeader(world, player, damage);
                            }
                        }
                        else {
                            NewKillLeader(world, player, damage);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = event.getEntity().getKiller();
        World world = dead.getWorld();
        if (world.getName().startsWith("Rank")) {
            world.playSound(dead.getLocation(), "playereliminated", 1.0f, 1.0f);
            if (RankStartTime.containsKey(world)) {
                SurviveTime.put(dead, System.currentTimeMillis() - RankStartTime.get(world));
            }
            if (killer != null) {
                int kills = 1;
                if (PlayerKilled.containsKey(killer)) {
                    kills += PlayerKilled.get(killer);
                }
                PlayerKilled.put(killer, kills);

                List<String> SoundList = new ArrayList<>();
                Random random = new Random();
                if (Champion.get(world).equals(dead)) {
                    SoundList.add("championeliminated1");
                    SoundList.add("championeliminated2");
                    int index = random.nextInt(SoundList.size());
                    world.getPlayers().forEach((e) -> {
                        e.sendMessage("§c§l注意 §3§l冠軍已被消滅");
                        e.playSound(e, SoundList.get(index), 1.0f, 1.0f);
                    });
                    PlayerKilledChampion.put(killer, 1);
                    Title title = Title.title(Component.text("§3§l你擊殺了冠軍"), Component.text(""), times);
                    killer.showTitle(title);
                }
                else if (KillLeader.get(world).equals(dead)) {
                    SoundList.add("killleadereliminated1");
                    SoundList.add("killleadereliminated2");
                    int index = random.nextInt(SoundList.size());
                    world.getPlayers().forEach((e) -> {
                        e.sendMessage("§c§l注意 §3§l擊殺首領已被消滅");
                        e.playSound(e, SoundList.get(index), 1.0f, 1.0f);
                    });
                    int killkillleader = 1;
                    if (PlayerKilledKillLeader.containsKey(killer)) {
                        killkillleader += PlayerKilledKillLeader.get(killer);
                    }
                    KillLeader.remove(world);
                    MaxDamage.remove(world);
                    PlayerKilledKillLeader.put(killer, killkillleader);
                    Title title = Title.title(Component.text("§3§l你擊殺了擊殺首領"), Component.text(""), times);
                    killer.showTitle(title);
                }
            }
            HaveChampion(world, 2);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = event.getFrom();
        if (RankStartTime.containsKey(world) && !LeftPlayer.contains(player.getUniqueId())) {
            player.sendMessage("§c§l你中途退出了排位賽, 受到了中離懲罰");
            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "aleague removepoints " + player.getName() + " 50");
            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "md setcooldown Rank_Christmas " + player.getName() + " 10m");
        }

        if (world.getName().startsWith("Rank")) {
            HaveChampion(world, 1);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world.getName().startsWith("Rank") && RankStartTime.containsKey(world)) {
            HaveChampion(world, 1);
            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "aleague removepoints " + player.getName() + " 50");
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
        if ((world.getName().startsWith("Ch") || world.getName().startsWith("Rank")) && player.getGameMode().equals(GameMode.SURVIVAL)) {
            if (UsingSurvivalItem.containsKey(player) || (slot >= 9 && slot <= 17) || slot == 40 || event.getClick().equals(ClickType.DOUBLE_CLICK)) {
                event.setCancelled(true);
                player.updateInventory();
            }
            else if (slot >= 18 && slot <= 26) {
                if (!((slot <= 20 && player.hasPermission("battleroyale.backpack.1")) ||
                      (slot <= 23 && player.hasPermission("battleroyale.backpack.2")) ||
                    player.hasPermission("battleroyale.backpack.3"))) {
                    event.setCancelled(true);
                    player.updateInventory();
                }
            }
        }
    }

    @EventHandler
    public void onChestSort(ChestSortEvent event) {
        Player player = (Player) event.getPlayer();
        try {
            World world = player.getWorld();
            if ((world.getName().startsWith("Ch") || world.getName().startsWith("Rank")) && player.getGameMode().equals(GameMode.SURVIVAL)) {
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
        Block block = event.getClickedBlock();
        if (block != null && block.getType().name().endsWith("_SIGN") && event.getAction().isRightClick() && world.startsWith("world")) {
            event.setCancelled(true);
        }
        if (item != null && (world.startsWith("C") || world.startsWith("Rank")) && player.getGameMode().equals(GameMode.SURVIVAL)) {
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta != null && itemMeta.hasDisplayName()) {
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
                        Collection<Entity> entities = location.getNearbyEntitiesByType(Entity.class, 5);
                        world.spawnParticle(Particle.EXPLOSION_LARGE, location, 20, 2, 2, 2, 0);
                        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                        for (Entity entity : entities) {
                            if (entity instanceof LivingEntity) {
                                LivingEntity livingEntity = (LivingEntity) entity;
                                livingEntity.damage(10);
                                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30,2, false, false, false));
                            }
                        }
                        snowball.remove();
                    }
                }.runTaskLater(plugin, 60L);
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

    public void NewKillLeader(World world, Player player, double damage) {
        MaxDamage.put(world, damage);
        if (!KillLeader.get(world).equals(player)) {
            KillLeader.put(world, player);
            List<String> SoundList = new ArrayList<>();
            Random random = new Random();
            SoundList.add("newkillleader1");
            SoundList.add("newkillleader2");
            int index = random.nextInt(SoundList.size());
            world.getPlayers().forEach((e) -> {
                e.playSound(e, SoundList.get(index), 1.0f, 1.0f);
                e.sendMessage("§c§l注意 §6§l" + player.getName() + " §3§l是新的擊殺首領 他造成了 §6§l" + (int) damage + " §3§l點傷害");
            });
            player.playSound(player, "youarenewkillleader", 1.0f, 1.0f);
            Title title = Title.title(Component.text("§3§l你是新的擊殺首領"), Component.text(""), times);
            player.showTitle(title);
        }
    }

    public void HaveChampion(World world, int amount) {
        int SurviveAmount = 0;
        for (Player e : world.getPlayers()) {
            if (e.getGameMode().equals(GameMode.SURVIVAL)) {
                SurviveAmount++;
            }
        }
        if (SurviveAmount <= amount && RankStartTime.containsKey(world)) {
            world.getPlayers().forEach((e) -> {
                if (!SurviveTime.containsKey(e) && e.getGameMode().equals(GameMode.SURVIVAL)) {
                    SurviveTime.put(e, System.currentTimeMillis() - RankStartTime.get(world));
                }
                e.playSound(e, "winner", 1.0f, 1.0f);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        e.playSound(e, "wehavechampion", 1.0f, 1.0f);
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
                e.sendMessage("§3§l造成的傷害: §6§l" + damage + " 點 (" + score + " RP)");
                PlayerDamage.remove(e);
            }
            if (PlayerKilled.containsKey(e)) {
                int kills = PlayerKilled.get(e);
                RP += kills * 15;
                e.sendMessage("§3§l擊殺玩家: §6§l" + kills + " 次 (" + kills * 15 + " RP)");
                PlayerKilled.remove(e);
            }
            if (PlayerKilledChampion.containsKey(e)) {
                int kills = PlayerKilledChampion.get(e);
                RP += kills * 20;
                e.sendMessage("§3§l擊殺冠軍: §6§l" + kills + " 次 (" + kills * 20 + " RP)");
                PlayerKilledChampion.remove(e);
            }
            if (PlayerKilledKillLeader.containsKey(e)) {
                int kills = PlayerKilledKillLeader.get(e);
                RP += kills * 20;
                e.sendMessage("§3§l擊殺擊殺首領: §6§l" + kills + " 次 (" + kills * 20 + " RP)");
                PlayerKilledKillLeader.remove(e);
            }
            if (SurviveTime.containsKey(e)) {
                int seconds = (int) (SurviveTime.get(e) / 1000);
                RP += seconds / 30;
                e.sendMessage("§3§l存活時間: §6§l" + seconds + " 秒 (" + seconds / 30 + " RP)");
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
