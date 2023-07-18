package cc.battleroyale.commands;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BattleRoyaleCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("battleroyale")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args[0].equals("start")) {
                    player.getWorld().getPlayers().forEach((e) -> e.getInventory().setItem(38, new ItemStack(Material.ELYTRA, 1)));
                }
            }
            else if (sender instanceof ConsoleCommandSender) {
                if (args[0].equals("start")) {
                    World world = sender.getServer().getWorld(args[1]);
                    world.getPlayers().forEach((e) -> {
                        e.getInventory().setItem(38, new ItemStack(Material.ELYTRA, 1));
                        int player_x = (int) (Math.random() * 700) - 350;
                        int player_z = (int) (Math.random() * 780) - 390;
                        e.teleport(new Location(world, player_x, 238, player_z));
                        e.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 500,4, false, false, false));
                        e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200,0, false, false, false));
                        e.playSound(e.getLocation(), "jumpmaster", 1.0f, 1.0f);
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
                else if (args[0].equals("ring")) {
                    World world = sender.getServer().getWorld(args[1]);
                    List<String> SoundList = new ArrayList<>();
                    Random random = new Random();
                    if (args[2].equals("start")) {
                        SoundList.add("ringstart1");
                        SoundList.add("ringstart2");
                        SoundList.add("ringstart3");
                        SoundList.add("ringstart4");

                    }
                    else if (args[2].equals("stop")) {
                        SoundList.add("ringstop1");
                        SoundList.add("ringstop2");
                        SoundList.add("ringstop3");
                        SoundList.add("ringstop4");
                    }
                    int index = random.nextInt(SoundList.size());
                    world.getPlayers().forEach((e) -> {
                        if (GetPlayerDistanceToBorder(e) <= 20 && world.getWorldBorder().isInside(e.getLocation())) {
                            e.playSound(e.getLocation(), SoundList.get(index), 1.0f, 1.0f);
                        }
                    });
                }
            }
        }
        return true;
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
}
