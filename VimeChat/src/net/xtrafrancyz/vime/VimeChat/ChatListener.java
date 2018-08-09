package net.xtrafrancyz.vime.VimeChat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created in: 14.04.2014
 *
 * @author xtrafrancyz
 */
class ChatListener implements Listener {
    private static final Map<String, PlayerInfo> players = new HashMap<>();
    private final Main plugin;

    ChatListener(Main plugin) {
        this.plugin = plugin;
        players.clear();
    }

    private static String implode(String[] arr) {
        if (arr.length == 0)
            return "";
        StringBuilder sb = new StringBuilder(arr[0]);
        for (int i = 1; i < arr.length; i++)
            sb.append(" ").append(arr[i]);
        return sb.toString();
    }

    private static boolean isUpperCase(String str) {
        for (char c : str.toCharArray())
            if (!Character.isUpperCase(c))
                return false;
        return true;
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent event) {
        final Response res = processChat(event.getPlayer(), event.getMessage());
        event.setMessage(res.message);
        
        if (res.messageToPlayer != null)
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', res.messageToPlayer)), 1);
    }

    private Response processChat(final Player player, String message) {
        
        message = message
            //восклицательные и вопросительные знаки не больше одного
            .replaceAll("([!?])\\1+", "$1")
            //Убирание ?!?!?!?!?!?!?
            .replaceAll("(!\\?|\\?!)+", "$1")
            //Убирание большого количества точек
            .replaceAll("\\.{3,}", "...")
            //Замена повторяющихся символов (2 и более)
            .replaceAll("([^.,!?])\\1+", "$1$1");
        
        String[] words = message.split(" ");
        int capsWords = 0;
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 2 && isUpperCase(words[i])) {
                capsWords++;
                words[i] = words[i].toLowerCase();
            }
        }
        message = implode(words);
        
        MessageInfo minfo = new MessageInfo(message, System.currentTimeMillis());
        Response res = new Response(message);
        boolean muted = false;
        
        PlayerInfo pinfo;
        if (!players.containsKey(player.getName())) {
            pinfo = new PlayerInfo();
            players.put(player.getName(), pinfo);
        } else
            pinfo = players.get(player.getName());
        
        if (capsWords > 2) {
            if (System.currentTimeMillis() - pinfo.lastCaps < 60 * 1000) {
                muted = true;
                if (Main.getApi().getMute(player.getName()) == null)
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> plugin.mute.mute("#antiflood", player.getName(), Main.muteTimeCaps, "Капс"), 1);
            } else {
                res.messageToPlayer = "&f[&cАнтиФлуд&f]&6 Отключите Caps Lock или вы будете замучены.";
            }
            pinfo.lastCaps = System.currentTimeMillis();
        }
        if (!muted && !pinfo.messages.isEmpty()) {
            if (pinfo.messages.size() > 3) {
                int maxDelay = (int) (minfo.time - pinfo.messages.getLast().time);
                for (int i = pinfo.messages.size() - 1; i > 1; i--) {
                    int diff = (int) (pinfo.messages.get(i).time - pinfo.messages.get(i - 1).time);
                    if (diff > maxDelay)
                        maxDelay = diff;
                }
                if (maxDelay < 2 * 1000) {
                    muted = true;
                    if (Main.getApi().getMute(player.getName()) == null)
                        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> plugin.mute.mute("#antiflood", player.getName(), Main.muteTimeFlood, "Флуд"), 1);
                }
            }
            if (!muted && minfo.equals(pinfo.messages.getLast())) {
                res.messageToPlayer = "&f[&cАнтиФлуд&f]&6 Ваше сообщение идентично предыдущему. В следующий раз вы будете замучены.";
                if (pinfo.messages.size() > 1) {
                    MessageInfo prelast = pinfo.messages.get(pinfo.messages.size() - 2);
                    if (System.currentTimeMillis() - prelast.time < 20 * 1000 && prelast.equals(minfo)) {
                        res.messageToPlayer = null;
                        if (Main.getApi().getMute(player.getName()) == null)
                            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> plugin.mute.mute("#antiflood", player.getName(), Main.muteTimeFlood, "Флуд"), 1);
                    }
                }
            }
            
        }
        
        pinfo.messages.addLast(minfo);
        pinfo.limitMessages();
        return res;
    }

    static class Response {
        String message;
        String messageToPlayer = null;

        Response(String message) {
            this.message = message;
        }
    }

    static class PlayerInfo {
        LinkedList<MessageInfo> messages = new LinkedList<>();
        long lastCaps = 0;

        void limitMessages() {
            while (messages.size() > 9)
                messages.removeFirst();
        }
    }

    static class MessageInfo {
        String message;
        long time;

        MessageInfo(String message, long time) {
            this.message = message.replaceAll("[^\\wа-яА-Я\\- ]", "");
            this.time = time;
        }

        boolean equals(MessageInfo i) {
            return message.equals(i.message);
        }
    }
}
