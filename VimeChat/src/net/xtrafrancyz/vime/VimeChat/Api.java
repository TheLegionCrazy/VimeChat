package net.xtrafrancyz.vime.VimeChat;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Created by TheLegion on 30.12.2017 (December) 1:26
 */
public class Api {
    
    public static MuteManager manager;
    
    public Api(MuteManager manager) {
        this.manager = manager;
    }
    
    /**
     * @param player имя игрока. которого вы ищите
     * @return MuteManager с игроком
     * @apiNote возвращает null, если игрок не замучен
     * @see net.xtrafrancyz.vime.VimeChat.MuteManager.MuteInfo
     */
    public MuteManager.MuteInfo getMute(String player) {
        return getMutes().get(player);
    }
    
    /**
     * @param player игрок, которого хотите замутить
     * @param reason причина мута
     * @param time   время в минутах
     * @throws IllegalArgumentException если player или reason blank (StringUtils.isBlank(String s)). Если minutes меньше либо равны 0
     */
    public void mute(String player, String reason, int time) throws IllegalArgumentException {
        if (StringUtils.isBlank(player))
            throw new IllegalArgumentException("Player could be nonNull");
        if (StringUtils.isBlank(reason))
            throw new IllegalArgumentException("Reason could be nonNull");
        if (time <= 0)
            throw new IllegalArgumentException("Time could be greater than 0");
        
        manager.mute("VimeWorld", player, time, reason);
    }
    
    /**
     * @param player игрок, которого хотите замутить
     * @throws IllegalArgumentException если игрок не в муте
     */
    public void unmute(String player) throws IllegalArgumentException {
        if (!manager.unMute(player))
            throw new IllegalArgumentException(player + " not muted");
    }
    
    /**
     * @return список всех мутов
     */
    public Map<String, MuteManager.MuteInfo> getMutes() {
        return manager.getMutes();
    }
}
