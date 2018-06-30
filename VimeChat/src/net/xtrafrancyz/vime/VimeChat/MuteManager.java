package net.xtrafrancyz.vime.VimeChat;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Created in: 14.04.2014
 *
 * @author xtrafrancyz
 * @author TheLegion
 */
public final class MuteManager implements Listener {

    private final Main plugin;
    private final File saveFile;
    private Map<String, MuteInfo> mutedPlayers;
    private int loop = -1;

    public MuteManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        saveFile = new File(plugin.getDataFolder(), "mutes");

        startLoop();
    }

    /**
     * @param n     число
     * @param form1 1 письмо, минута
     * @param form2 2 письма, минуты
     * @param form3 90 писем, минут
     * @return правильная множественная форма
     */
    public static String plurals(int n, String form1, String form2, String form3) {
        if (n == 0) {
            return form3;
        }

        n = Math.abs(n) % 100;

        if (n > 10 && n < 20) {
            return form3;
        }

        n = n % 10;

        if (n > 1 && n < 5) {
            return form2;
        }

        if (n == 1) {
            return form1;
        }

        return form3;
    }

    private String getPrefix(String admin) {
        String prefix;

        if (admin.equals("#antiflood")) {
            prefix = "&f[&cАнтиФлуд&f] ";
        } else if (admin.equals("#console")) {
            prefix = "&f[&aСервер&f] ";
        } else if (Main.usePex) {
            prefix = "&f[&3" + admin + "&f] ";
        } else {
            prefix = "&f[&a" + admin + "&f] ";
        }

        return prefix;
    }

    private String getPrefix(CommandSender sender) {
        String prefix;

        if (sender instanceof ConsoleCommandSender) {
            prefix = "&f[&aСервер&f] ";
        } else {
            if (Main.usePex) {
                prefix = "&f[&3" + sender.getName() + "&f] ";
            } else {
                prefix = "&f[&a" + sender.getName() + "&f] ";
            }
        }

        return prefix;
    }

    private String getTime(int time) {
        String timeMsg;

        if (time == 0) {
            timeMsg = "всегда.";
        } else {
            timeMsg = time + "&e " + plurals(time, "минуту", "минуты", "минут");
        }

        return timeMsg;
    }


    public boolean mute(String admin, String player, int time, String reason) {
        String printableReason = "";
        if (reason.length() > 0)
            printableReason = " &eПричина: &a" + reason + "&e.";

        String prefix = getPrefix(admin);

        if (reason.isEmpty()) {
            reason = null;
        }

        mutedPlayers.put(player, new MuteInfo(reason, time == 0 ? time : System.currentTimeMillis() + time * 60000, admin));

        plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                prefix + "&eИгроку &a" + player + "&e запрещено писать в чат на &a" + getTime(time) + printableReason));
        return true;
    }

    public boolean unMute(String player, String sender) {
        if (isMuted(player)) {
            mutedPlayers.remove(player);
            plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    getPrefix(sender) + "&eИгрок &a" + player + "&e снова может писать в чат"));
            return true;
        }

        return false;
    }

    public boolean editMute(String admin, String player, int newTime) {
        if (isMuted(player)) {
            MuteInfo mute = mutedPlayers.remove(player);
            mute.admin = admin;
            mute.muteto = System.currentTimeMillis() + newTime * 60000;
            mutedPlayers.put(player, mute);

            plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    getPrefix(admin) + "&eИгрок &a" + player + "&e был перемучен на " + getTime(newTime)));
            return true;
        }

        return false;
    }

    public boolean isMuted(String player) {
        return mutedPlayers.containsKey(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        if (isMuted(event.getPlayer().getName())) {
            MuteInfo mi = mutedPlayers.get(event.getPlayer().getName());
            int minutes = (int) Math.ceil((mi.muteto - System.currentTimeMillis()) / 60000);
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Вам запрещено писать в чат" + (mi.reason != null ? ". Причина: " + ChatColor.YELLOW + mi.reason : "") + ChatColor.RED + ". Осталось: " + ChatColor.YELLOW + minutes + " " + plurals(minutes, "минута", "минуты", "минут"));
        }
    }

    public void startLoop() {
        loop = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Entry<String, MuteInfo> entry : mutedPlayers.entrySet())
                if (entry.getValue().muteto > 0 && entry.getValue().muteto < System.currentTimeMillis()) {
                    mutedPlayers.remove(entry.getKey());
                    Player pl = plugin.getServer().getPlayerExact(entry.getKey());
                    if (pl != null)
                        pl.sendMessage(ChatColor.GREEN + "Вы снова можете писать в чат");
                }
        }, 200, 50);
    }

    public void stopLoop() {
        plugin.getServer().getScheduler().cancelTask(loop);
    }

    public void save() {
        try {
            if (!saveFile.exists())
                saveFile.createNewFile();
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(saveFile)));
            oos.writeObject(mutedPlayers);
            oos.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, null, e);
        }
    }

    public void load() {
        if (!saveFile.exists()) {
            mutedPlayers = new ConcurrentHashMap<>();
        } else {
            try {
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(saveFile)));
                mutedPlayers = (Map) ois.readObject();
                if (mutedPlayers instanceof HashMap) {
                    ConcurrentHashMap<String, MuteInfo> newmap = new ConcurrentHashMap<>();
                    for (Map.Entry<String, MuteInfo> e : mutedPlayers.entrySet())
                        newmap.put(e.getKey(), e.getValue());
                    mutedPlayers = newmap;
                }
                ois.close();
            } catch (IOException | ClassNotFoundException ex) {
                plugin.getLogger().log(Level.SEVERE, null, ex);
                mutedPlayers = new ConcurrentHashMap<>();
            }
        }
    }

    public Map<String, MuteInfo> getMutes() {
        return mutedPlayers;
    }

    public static class MuteInfo implements Serializable {
        public String reason;
        public String admin;
        public long muteto;

        public MuteInfo(String reason, long muteto, String admin) {
            this.reason = reason;
            this.muteto = muteto;
            this.admin = admin;
        }
    }
}
