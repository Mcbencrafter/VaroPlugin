package de.varoplugin.varo.game.task.trigger.player;

import de.varoplugin.varo.api.event.game.player.VaroPlayerParticipantStateChangeEvent;
import de.varoplugin.varo.api.task.trigger.Trigger;
import de.varoplugin.varo.game.entity.player.ParticipantState;
import de.varoplugin.varo.game.entity.player.Player;
import org.bukkit.event.EventHandler;

public class VaroParticipantStateTrigger extends AbstractPlayerTrigger {

    private ParticipantState state;

    public VaroParticipantStateTrigger(Player player, ParticipantState state, boolean match) {
        super(player, match);
        this.state = state;
    }

    public VaroParticipantStateTrigger(Player player, ParticipantState state) {
        this(player, state, true);
    }

    @Override
    protected boolean isTriggered() {
        return this.getPlayer().getState().equals(this.state);
    }

    @EventHandler
    public void onPlayerStateChange(VaroPlayerParticipantStateChangeEvent event) {
        if (!this.getPlayer().equals(event.getPlayer())) return;
        this.triggerIf(event.getState().equals(this.state));
    }

    @Override
    public Trigger clone() {
        VaroParticipantStateTrigger trigger = (VaroParticipantStateTrigger) super.clone();
        trigger.state = this.state;
        return trigger;
    }
}
