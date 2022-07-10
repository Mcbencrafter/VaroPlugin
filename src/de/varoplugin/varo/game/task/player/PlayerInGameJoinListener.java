package de.varoplugin.varo.game.task.player;

import de.varoplugin.varo.game.entity.player.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerInGameJoinListener extends AbstractPlayerListener {

    public PlayerInGameJoinListener(Player player) {
        super(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.getPlayer().isPlayer(event.getPlayer())) return;
    }
}
