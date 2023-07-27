package cc.battleroyale.commands;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

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
                        e.getInventory().setItem(38, new ItemStack(Material.ELYTRA, 1));
                        int player_x = (int) (Math.random() * 700) - 350;
                        int player_z = (int) (Math.random() * 780) - 390;
                        e.teleport(new Location(world, player_x, 238, player_z));
                        e.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 500,4, false, false, false));
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
            }
        }
        return true;
    }

    public void removePermission(Player player, String permission) {
        User user = LuckPermsProvider.get().getPlayerAdapter(Player.class).getUser(player);
        user.data().remove(Node.builder(permission).build());
        LuckPermsProvider.get().getUserManager().saveUser(user);
    }
}
