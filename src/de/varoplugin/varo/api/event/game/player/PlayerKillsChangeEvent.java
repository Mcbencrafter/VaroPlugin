package de.varoplugin.varo.api.event.game.player;

import de.varoplugin.varo.game.entity.player.VaroPlayer;

/**
 * @author CuukyOfficial
 * @version v0.1
 */
public class PlayerKillsChangeEvent extends VaroPlayerEvent {

    private final int kills;

    public PlayerKillsChangeEvent(VaroPlayer player, int kills) {
        super(player);

        this.kills = kills;
    }
}