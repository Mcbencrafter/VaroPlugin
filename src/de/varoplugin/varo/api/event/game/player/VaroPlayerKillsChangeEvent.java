package de.varoplugin.varo.api.event.game.player;

import de.varoplugin.varo.game.entity.player.VaroPlayer;

public class VaroPlayerKillsChangeEvent extends VaroPlayerCancelableEvent {

    private int kills;

    public VaroPlayerKillsChangeEvent(VaroPlayer player, int kills) {
        super(player);

        this.kills = kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getKills() {
        return this.kills;
    }
}