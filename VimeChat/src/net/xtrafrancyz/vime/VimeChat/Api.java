package net.xtrafrancyz.vime.VimeChat;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Created by TheLegion on 30.12.2017 (December) 1:26
 */
public class Api {

    private static MuteManager manager;

    Api(MuteManager manager) {
        Api.manager = manager;
    }

    /**
     * @param player имя игрока. которого вы ищите
     * @return MuteManager с игроком
     * @apiNote возвращает null, если игрок не замучен
     * @see net.xtrafrancyz.vime.VimeChat.MuteManager.MuteInfo
     */
    MuteManager.MuteInfo getMute(String player) {
        return getMutes().get(player);
    }

    /**
     * @param player игрок, которого хотите замутить
     * @param reason причина мута
     * @param time   время в минутах
     * @return Результат
     * @throws IllegalArgumentException если player или reason blank (StringUtils.isBlank(String s)). Если minutes меньше либо равны 0
     */
    public boolean mute(String player, String reason, int time) throws IllegalArgumentException {
        if (StringUtils.isBlank(player))
            throw new IllegalArgumentException("Player could be nonNull");
        if (StringUtils.isBlank(reason))
            throw new IllegalArgumentException("Reason could be nonNull");
        if (time <= 0)
            throw new IllegalArgumentException("Time could be greater than 0");

        return manager.mute("VimeWorld", player, time, reason);
    }

    /**
     * @param player игрок, которого хотите замутить
     * @return Результат
     */
    public boolean unmute(String player) throws IllegalArgumentException {
        return manager.unMute(player, "VimeWorld");
    }

    /**
     * @param player  тот, которому нужно поменять мут
     * @param newTime новое время
     * @return Результат
     */
    public boolean editmute(String player, int newTime) throws IllegalArgumentException {
        return manager.editMute("VimeWorld", player, newTime);
    }

    /**
     * @return список всех мутов
     */
    private Map<String, MuteManager.MuteInfo> getMutes() {
        return manager.getMutes();
    }
}
