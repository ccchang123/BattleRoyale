package cc.battleroyale.commands;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public class BattleRoyaleCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("battleroyale")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                PlayerData playerData = PlayerData.get(player.getUniqueId());
                playerData.leaveSkillCasting(true);
            }
            else if (sender instanceof ConsoleCommandSender) {
                if (args[0].equals("start")) {
                    World world = sender.getServer().getWorld(args[1]);
                    ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.displayName(Component.text("§c該槽位已被鎖定"));
                    itemMeta.setCustomModelData(30);
                    itemStack.setItemMeta(itemMeta);
                    world.getPlayers().forEach((e) -> {
                        for (int i=1; i<=3; i++) {
                            removePermission(e, "battleroyale.backpack." + i);
                        }
                        for (int i=9; i<=26; i++) {
                            e.getInventory().setItem(i, itemStack);
                        }
                        e.setAbsorptionAmount(0);
                        e.setFoodLevel(20);
                        e.getInventory().setItem(38, new ItemStack(Material.ELYTRA, 1));
                        int player_x = (int) (Math.random() * 700) - 350;
                        int player_z = (int) (Math.random() * 780) - 390;
                        e.teleport(new Location(world, player_x, 238, player_z));
                        e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200,0, false, false, false));
                        e.playSound(e, "jumpmaster", 1.0f, 1.0f);
                    });
                }
                else if (args[0].equals("shield")) {
                    Player player = Bukkit.getPlayer(args[1]);
                    if (player != null) {
                        double PlayerShield = player.getAbsorptionAmount();
                        int amount = 0;
                        try {
                            amount = Integer.parseInt(args[2]);
                        }
                        catch (NumberFormatException ignored){ }
                        if (PlayerShield + amount >= 20) {
                            player.setAbsorptionAmount(20);
                        }
                        else {
                            player.setAbsorptionAmount(PlayerShield + amount);
                        }
                    }
                }
                else if (args[0].equals("glide")) {
                    try {
                        Player player = Bukkit.getPlayer(args[1]);
                        if (player != null) {
                            player.setGliding(true);
                        }
                    }
                    catch (Exception ignored) { }
                }
                else if (args[0].equals("heatshield")) {
                    MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("world_boss_3_heatshield").orElse(null);
                    Player player = Bukkit.getPlayer(args[1]);
                    if (mob != null && player != null) {
                        mob.spawn(BukkitAdapter.adapt(player.getLocation()),1);
                    }
                }
                else if (args[0].equals("map")) {
                    World world = sender.getServer().getWorld(args[1]);
                    ItemStack itemStack = new ItemStack(Material.FILLED_MAP, 1);
                    MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
                    MapView mapView = Bukkit.createMap(world);
                    mapView.setCenterX(0);
                    mapView.setCenterZ(0);
                    mapView.setScale(MapView.Scale.FAR);
                    mapView.setTrackingPosition(true);
                    double centerX = world.getWorldBorder().getCenter().getX();
                    double centerZ = world.getWorldBorder().getCenter().getZ();
                    // 2000, 750, 375, 180, 90, 25, 1
                    mapView.addRenderer(new MapRenderer() {
                        @Override
                        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
                            double WorldBorderSize = world.getWorldBorder().getSize();
                            setPixel(canvas, centerX * 127 / 1024 + 63.5, centerZ * 127 / 1024 + 63.5, WorldBorderSize / 16);
                        }
                    });
                    mapMeta.setMapView(mapView);
                    itemStack.setItemMeta(mapMeta);
                    world.getPlayers().forEach((e) -> {
                        e.getInventory().setItem(40, itemStack);
                    });
                }
            }
        }
        return true;
    }

    public void setPixel(MapCanvas canvas, double X, double Z, double Border) {
        for (int x = 0; x <= 127; x++) {
            for (int z = 0; z <= 127; z++) {
                if (x < X - Border ||
                    x > X + Border ||
                    z < Z - Border ||
                    z > Z + Border ) {
                    canvas.setPixelColor(x, z, Color.RED);
                }
            }
        }
    }

    public void removePermission(Player player, String permission) {
        User user = LuckPermsProvider.get().getPlayerAdapter(Player.class).getUser(player);
        user.data().remove(Node.builder(permission).build());
        LuckPermsProvider.get().getUserManager().saveUser(user);
    }
}
