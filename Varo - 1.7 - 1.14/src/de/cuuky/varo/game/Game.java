package de.cuuky.varo.game;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.apache.commons.lang.time.DateUtils;

import de.cuuky.varo.Main;
import de.cuuky.varo.api.VaroAPI;
import de.cuuky.varo.api.event.events.game.VaroStartEvent;
import de.cuuky.varo.bot.BotLauncher;
import de.cuuky.varo.bot.discord.VaroDiscordBot;
import de.cuuky.varo.command.varo.RandomTeamCommand;
import de.cuuky.varo.config.config.ConfigEntry;
import de.cuuky.varo.config.messages.ConfigMessages;
import de.cuuky.varo.entity.player.VaroPlayer;
import de.cuuky.varo.entity.player.event.BukkitEventType;
import de.cuuky.varo.entity.player.stats.stat.PlayerState;
import de.cuuky.varo.event.VaroEvent;
import de.cuuky.varo.game.end.WinnerCheck;
import de.cuuky.varo.game.start.AutoStart;
import de.cuuky.varo.game.start.ProtectionTime;
import de.cuuky.varo.game.state.GameState;
import de.cuuky.varo.list.ListHandler;
import de.cuuky.varo.logger.logger.EventLogger;
import de.cuuky.varo.logger.logger.EventLogger.LogType;
import de.cuuky.varo.scoreboard.ScoreboardHandler;
import de.cuuky.varo.serialize.identifier.VaroSerializeField;
import de.cuuky.varo.serialize.identifier.VaroSerializeable;
import de.cuuky.varo.threads.OutSideTimeChecker;
import de.cuuky.varo.utils.JavaUtils;
import de.cuuky.varo.utils.VaroUtils;
import de.cuuky.varo.version.VersionUtils;
import de.cuuky.varo.version.types.Sounds;
import de.cuuky.varo.world.border.BorderDecreaseDayTimer;
import de.cuuky.varo.world.border.BorderDecreaseMinuteTimer;
import de.cuuky.varo.world.border.VaroBorder;

public class Game implements VaroSerializeable {

	/*
	 * Partly OLD
	 */

	public static Game instance;

	@VaroSerializeField(path = "gamestate")
	private GameState gamestate;
	@VaroSerializeField(path = "autostart")
	private AutoStart autostart;
	@VaroSerializeField(path = "borderDecrease")
	private BorderDecreaseDayTimer borderDecrease;
	@VaroSerializeField(path = "lobby")
	private Location lobby;
	@VaroSerializeField(path = "lastDayTimer")
	private Date lastDayTimer;
	@VaroSerializeField(path = "lastCoordsPost")
	private Date lastCoordsPost;

	private boolean showDistanceToBorder, showTimeInActionBar, finaleJoinStart = false, firstTime = false;
	private int protectionTime, noKickDistance, playTime, startCountdown, startScheduler;
	private ProtectionTime protection;
	private BorderDecreaseMinuteTimer minuteTimer;

	public Game() { // Für Deserializer
		instance = this;
	}

	public static Game getInstance() {
		return instance;
	}

	public static void initialise() {
		instance = new Game();

		instance.startRefreshTimer();
		instance.loadVariables();

		instance.gamestate = GameState.LOBBY;
		instance.borderDecrease = new BorderDecreaseDayTimer(true);
	}

	private void startRefreshTimer() {

		Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), new Runnable() {

			int seconds = 0;

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				seconds++;
				if (gamestate == GameState.STARTED) {
					if (seconds == 60) {
						seconds = 0;
						if (ConfigEntry.KICK_AT_SERVER_CLOSE.getValueAsBoolean()) {
							double minutesToClose = (int) (((OutSideTimeChecker.getInstance().getDate2().getTime().getTime() - new Date().getTime()) / 1000) / 60);

							if (minutesToClose == 10 || minutesToClose == 5 || minutesToClose == 3 || minutesToClose == 2 || minutesToClose == 1)
								Bukkit.broadcastMessage(ConfigMessages.KICK_SERVER_CLOSE_SOON.getValue().replace("%minutes%", String.valueOf(minutesToClose)));

							if (!OutSideTimeChecker.getInstance().canJoin())
								for (VaroPlayer vp : (ArrayList<VaroPlayer>) VaroPlayer.getOnlinePlayer().clone()) {
									vp.getStats().setCountdown(0);
									vp.getPlayer().kickPlayer("§cDie Spielzeit ist nun vorüber!\n§7Versuche es morgen erneut");
								}
						}
					}

					if (ConfigEntry.PLAY_TIME.isIntActivated()) {
						for (VaroPlayer vp : (ArrayList<VaroPlayer>) VaroPlayer.getOnlinePlayer().clone()) {
							if (vp.getStats().isSpectator() || vp.isAdminIgnore())
								continue;

							int countdown = vp.getStats().getCountdown() - 1;
							Player p = vp.getPlayer();

							if (showTimeInActionBar || vp.getStats().isShowActionbarTime())
								vp.getNetworkManager().sendActionbar(Main.getColorCode() + vp.getStats().getCountdownMin(countdown) + "§8:" + Main.getColorCode() + vp.getStats().getCountdownSec(countdown));
							else if (showDistanceToBorder) {
								int distance = (int) VaroBorder.getInstance().getBorderDistanceTo(p);
								if (!ConfigEntry.DISTANCE_TO_BORDER_REQUIRED.isIntActivated() || distance <= ConfigEntry.DISTANCE_TO_BORDER_REQUIRED.getValueAsInt())
									vp.getNetworkManager().sendActionbar("§7Distanz zur Border: " + Main.getColorCode() + distance);
							}

							if (countdown == playTime - protectionTime - 1 && !firstTime && !VaroEvent.getMassRecEvent().isEnabled())
								Bukkit.broadcastMessage(ConfigMessages.JOIN_PROTECTION_OVER.getValue(vp));

							if (countdown == 30 || countdown == 10 || countdown == 5 || countdown == 4 || countdown == 3 || countdown == 2 || countdown == 1 || countdown == 0) {
								if (countdown == 0 && !VaroEvent.getMassRecEvent().isEnabled()) {
									Bukkit.broadcastMessage(ConfigMessages.KICK_BROADCAST.getValue(vp));
									vp.onEvent(BukkitEventType.KICKED);
									p.kickPlayer(ConfigMessages.KICK_MESSAGE.getValue(vp));
									continue;
								} else {
									if (countdown == 1)
										if (!vp.canBeKicked(noKickDistance)) {
											vp.sendMessage(ConfigMessages.KICK_PLAYER_NEARBY.getValue().replace("%distance%", String.valueOf(ConfigEntry.NO_KICK_DISTANCE.getValueAsInt())));
											countdown += 1;
										}

									Bukkit.broadcastMessage(ConfigMessages.KICK_IN_SECONDS.getValue().replace("%player%", vp.getName()).replace("%countdown%", countdown == 1 ? "einer" : String.valueOf(countdown)));
								}
							}

							vp.getStats().setCountdown(countdown);
						}
					}
				}

				for (VaroPlayer vp : VaroPlayer.getOnlinePlayer()) {
					if (gamestate == GameState.LOBBY) {
						vp.getStats().setCountdown(playTime);
						if (vp.getStats().getState() == PlayerState.DEAD)
							vp.getStats().setState(PlayerState.ALIVE);
					}

					ScoreboardHandler.getInstance().update(vp);
					vp.getNetworkManager().sendTablist();
				}

				if (ConfigEntry.SESSIONS_PER_DAY.getValueAsInt() <= 0) {
					for (VaroPlayer vp : VaroPlayer.getVaroPlayer()) {
						if (vp.getStats().getTimeUntilAddSession() == null) {
							continue;
						}
						if (new Date().after(vp.getStats().getTimeUntilAddSession())) {
							vp.getStats().setSessions(vp.getStats().getSessions() + 1);
							if (vp.getStats().getSessions() < ConfigEntry.PRE_PRODUCE_SESSIONS.getValueAsInt() + 1) {
								vp.getStats().setTimeUntilAddSession(DateUtils.addHours(new Date(), ConfigEntry.JOIN_AFTER_HOURS.getValueAsInt()));
							} else {
								vp.getStats().setTimeUntilAddSession(null);
							}
						}
					}
				}

			}
		}, 0, 20);
	}

	public void start() {
		if (hasStarted() || isStarting())
			return;

		if (ConfigEntry.DO_RANDOMTEAM_AT_START.getValueAsInt() > 0) {
			new RandomTeamCommand().doRandomTeam(ConfigEntry.DO_RANDOMTEAM_AT_START.getValueAsInt());
		}

		if (ConfigEntry.DO_SORT_AT_START.getValueAsBoolean())
			VaroUtils.sortPlayers();

		removeArentAtStart();
		if (minuteTimer != null)
			minuteTimer.remove();

		minuteTimer = new BorderDecreaseMinuteTimer();
		startScheduler = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), new Runnable() {

			@Override
			public void run() {
				if (VersionUtils.getOnlinePlayer().size() != 0)
					((Player) VersionUtils.getOnlinePlayer().toArray()[0]).getWorld().setTime(1000);

				if (startCountdown != 0)
					Bukkit.broadcastMessage(ConfigMessages.GAME_START_COUNTDOWN.getValue().replace("%countdown%", startCountdown == 1 ? "einer" : String.valueOf(startCountdown)));

				if (startCountdown == ConfigEntry.STARTCOUNTDOWN.getValueAsInt() || startCountdown == 1) {
					for (VaroPlayer pl1 : VaroPlayer.getOnlinePlayer()) {
						if (pl1.getStats().isSpectator())
							continue;

						Player pl = pl1.getPlayer();
						pl.setGameMode(GameMode.ADVENTURE);
						pl1.cleanUpPlayer();
					}
				}

				if (startCountdown == 5 || startCountdown == 4 || startCountdown == 3 || startCountdown == 2 || startCountdown == 1) {
					for (VaroPlayer vp : VaroPlayer.getOnlinePlayer()) {
						if (vp.getStats().isSpectator())
							continue;

						Player pl = vp.getPlayer();
						pl.playSound(pl.getLocation(), Sounds.NOTE_BASS_DRUM.bukkitSound(), 1, 1);

						String[] title = ConfigMessages.GAME_VARO_START_TITLE.getValue().replace("%countdown%", String.valueOf(startCountdown)).split("\n");
						if (title.length != 0)
							vp.getNetworkManager().sendTitle(title[0], title.length == 2 ? title[1] : "");
					}
				}

				if (startCountdown == 0) {
					for (VaroPlayer pl1 : VaroPlayer.getOnlinePlayer()) {
						if (pl1.getStats().isSpectator())
							continue;

						Player pl = pl1.getPlayer();
						pl.playSound(pl.getLocation(), Sounds.NOTE_PLING.bukkitSound(), 1, 1);
						pl.setGameMode(GameMode.SURVIVAL);
						pl1.cleanUpPlayer();
						pl1.getStats().loadStartDefaults();
					}

					if (VaroAPI.getEventManager().executeEvent(new VaroStartEvent(Game.this))) {
						startCountdown = ConfigEntry.STARTCOUNTDOWN.getValueAsInt();
						Bukkit.getScheduler().cancelTask(startScheduler);
						return;
					}

					setGamestate(GameState.STARTED);
					fillChests();
					VaroUtils.getMainWorld().strikeLightningEffect(VaroUtils.getMainWorld().getSpawnLocation());
					firstTime = true;
					Bukkit.broadcastMessage(ConfigMessages.GAME_VARO_START.getValue());
					EventLogger.getInstance().println(LogType.ALERT, ConfigMessages.ALERT_GAME_STARTED.getValue());
					startCountdown = ConfigEntry.STARTCOUNTDOWN.getValueAsInt();
					Bukkit.getScheduler().cancelTask(startScheduler);

					Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {

						@Override
						public void run() {
							firstTime = false;
						}
					}, ConfigEntry.PLAY_TIME.getValueAsInt() * 60 * 20);

					ListHandler.getInstance().getStartItems().giveToAll();
					if (ConfigEntry.STARTPERIOD_PROTECTIONTIME.getValueAsInt() > 0) {
						Bukkit.broadcastMessage(ConfigMessages.PROTECTION_START.getValue().replace("%seconds%", String.valueOf(ConfigEntry.STARTPERIOD_PROTECTIONTIME.getValueAsInt())));
						protection = new ProtectionTime();
					}

					return;
				}

				startCountdown--;
			}
		}, 0, 20);
	}

	public void end(WinnerCheck check) {
		this.gamestate = GameState.END;

		for (VaroPlayer vp : check.getPlaces().get(1)) {
			if (!vp.isOnline())
				continue;

			Player p = vp.getPlayer();
			p.getWorld().spawnEntity(p.getLocation().clone().add(1, 0, 0), EntityType.FIREWORK);
			p.getWorld().spawnEntity(p.getLocation().clone().add(-1, 0, 0), EntityType.FIREWORK);
			p.getWorld().spawnEntity(p.getLocation().clone().add(0, 0, 1), EntityType.FIREWORK);
			p.getWorld().spawnEntity(p.getLocation().clone().add(0, 0, -1), EntityType.FIREWORK);
		}

		String first = "";
		String second = "";
		String third = "";
		for (int i = 1; i <= 3; i++) {
			ArrayList<VaroPlayer> won;
			won = check.getPlaces().get(i);

			if (won == null)
				break;

			String names = "";
			for (VaroPlayer vp : won)
				names = names + (!won.toArray()[won.size() - 1].equals(vp) ? vp.getName() + (won.size() > 2 ? (won.toArray()[won.size() - 2].equals(vp) ? "" : ", ") : "") : ((won.size() == 1 ? "" : " & ") + vp.getName()));
			names = names + (won.get(0).getTeam() != null ? " (#" + won.get(0).getTeam().getName() + ")" : "");

			switch (i) {
				case 1:
					first = names;
					break;
				case 2:
					second = names;
					break;
				case 3:
					third = names;
					break;
			}
		}

		Bukkit.broadcastMessage(Main.getColorCode() + first + " §7" + (first.contains("&") ? "haben" : "hat") + " das Projekt für sich entschieden! §5Herzlichen Glückwunsch!");
		EventLogger.getInstance().println(LogType.WIN, first + " " + (first.contains("&") ? "haben" : "hat") + " das Projekt für sich entschieden! Herzlichen Glückwunsch!");
		VaroDiscordBot db = BotLauncher.getDiscordBot();
		if (db != null && db.isEnabled()) {
			if (db.getResultChannel() != null && db.isEnabled())
				db.sendMessage((":first_place: " + first + (second != null ? "\n" + ":second_place: " + second : "") + (third != null ? "\n" + ":third_place: " + third : "")) + "\n\nHerzlichen Glückwunsch!", "Das Projekt ist nun vorbei!", Color.MAGENTA, BotLauncher.getDiscordBot().getResultChannel());

			File file = new File("plugins/Varo/logs", "logs.yml");
			if (file.exists())
				db.sendFile("Die Logs des Projektes", file, BotLauncher.getDiscordBot().getResultChannel());
		}
	}

	@SuppressWarnings("unchecked")
	private void removeArentAtStart() {
		if (!ConfigEntry.REMOVE_PLAYERS_ARENT_AT_START.getValueAsBoolean())
			return;

		for (VaroPlayer varoplayer : (ArrayList<VaroPlayer>) VaroPlayer.getVaroPlayer().clone())
			if (!varoplayer.isOnline())
				varoplayer.delete();
	}

	private void fillChests() {
		if (!ConfigEntry.RANDOM_CHEST_FILL_RADIUS.isIntActivated())
			return;

		int radius = ConfigEntry.RANDOM_CHEST_FILL_RADIUS.getValueAsInt();
		Location loc = VaroUtils.getMainWorld().getSpawnLocation().clone().add(radius, radius, radius);
		Location loc2 = VaroUtils.getMainWorld().getSpawnLocation().clone().add(-radius, -radius, -radius);

		int itemsPerChest = ConfigEntry.RANDOM_CHEST_MAX_ITEMS_PER_CHEST.getValueAsInt();
		ArrayList<ItemStack> chestItems = ListHandler.getInstance().getChestItems().getItems();
		for (Block block : getBlocksBetweenPoints(loc, loc2)) {
			if (!(block.getState() instanceof Chest))
				continue;

			Chest chest = (Chest) block.getState();
			chest.getBlockInventory().clear();
			for (int i = 0; i < itemsPerChest; i++) {
				int random = JavaUtils.randomInt(0, chest.getBlockInventory().getSize() - 1);
				while (chest.getBlockInventory().getContents().length != chest.getBlockInventory().getSize())
					random = JavaUtils.randomInt(0, chest.getBlockInventory().getSize() - 1);

				chest.getBlockInventory().setItem(random, chestItems.get(JavaUtils.randomInt(0, chestItems.size() - 1)));
			}
		}

		Bukkit.broadcastMessage("§7Alle Kisten um den " + Main.getColorCode() + "Spawn §7wurden " + Main.getColorCode() + "aufgefüllt§7!");
	}

	private List<Block> getBlocksBetweenPoints(Location l1, Location l2) {
		List<Block> blocks = new ArrayList<>();
		int topBlockX = (Math.max(l1.getBlockX(), l2.getBlockX()));
		int bottomBlockX = (Math.min(l1.getBlockX(), l2.getBlockX()));
		int topBlockY = (Math.max(l1.getBlockY(), l2.getBlockY()));
		int bottomBlockY = (Math.min(l1.getBlockY(), l2.getBlockY()));
		int topBlockZ = (Math.max(l1.getBlockZ(), l2.getBlockZ()));
		int bottomBlockZ = (Math.min(l1.getBlockZ(), l2.getBlockZ()));

		for (int x = bottomBlockX; x <= topBlockX; x++) {
			for (int y = bottomBlockY; y <= topBlockY; y++) {
				for (int z = bottomBlockZ; z <= topBlockZ; z++) {
					blocks.add(l1.getWorld().getBlockAt(x, y, z));
				}
			}
		}
		return blocks;
	}

	private void loadVariables() {
		showDistanceToBorder = ConfigEntry.SHOW_DISTANCE_TO_BORDER.getValueAsBoolean();
		showTimeInActionBar = ConfigEntry.SHOW_TIME_IN_ACTIONBAR.getValueAsBoolean();
		protectionTime = ConfigEntry.JOIN_PROTECTIONTIME.getValueAsInt();
		noKickDistance = ConfigEntry.NO_KICK_DISTANCE.getValueAsInt();
		playTime = ConfigEntry.PLAY_TIME.getValueAsInt() * 60;
		startCountdown = ConfigEntry.STARTCOUNTDOWN.getValueAsInt();
	}

	public void abort() {
		Bukkit.getScheduler().cancelTask(startScheduler);
		Bukkit.broadcastMessage("§7Der Start wurde §cabgebrochen§7!");
		startCountdown = ConfigEntry.STARTCOUNTDOWN.getValueAsInt();
	}

	public void setBorderDecrease(BorderDecreaseDayTimer borderDecrease) {
		this.borderDecrease = borderDecrease;
	}

	public Date getLastCoordsPost() {
		return lastCoordsPost;
	}

	public void setLastCoordsPost(Date lastCoordsPost) {
		this.lastCoordsPost = lastCoordsPost;
	}

	public Date getLastDayTimer() {
		return lastDayTimer;
	}

	public void setLastDayTimer(Date lastDayTimer) {
		this.lastDayTimer = lastDayTimer;
	}

	public ProtectionTime getProtection() {
		return protection;
	}

	public void setProtection(ProtectionTime protection) {
		this.protection = protection;
	}

	public boolean isStarting() {
		return startCountdown != ConfigEntry.STARTCOUNTDOWN.getValueAsInt();
	}

	public boolean isFirstTime() {
		return firstTime;
	}

	public boolean hasStarted() {
		return gamestate != GameState.LOBBY;
	}

	public boolean isRunning() {
		return gamestate == GameState.STARTED;
	}

	public void setGamestate(GameState gamestate) {
		this.gamestate = gamestate;
	}

	public int getStartCountdown() {
		return startCountdown;
	}

	public GameState getGameState() {
		return gamestate;
	}

	public AutoStart getAutoStart() {
		return autostart;
	}

	public void setAutoStart(AutoStart autoStart) {
		this.autostart = autoStart;
	}

	public Location getLobby() {
		return lobby;
	}

	public void setLobby(Location lobby) {
		this.lobby = lobby;
	}

	public boolean getFinaleJoinStart() {
		return finaleJoinStart;
	}

	public void setFinaleJoinStart(boolean finaleJoinStart) {
		this.finaleJoinStart = finaleJoinStart;
	}

	@Override
	public void onDeserializeEnd() {
		startRefreshTimer();

		loadVariables();

		if (gamestate == GameState.STARTED)
			minuteTimer = new BorderDecreaseMinuteTimer();
	}

	@Override
	public void onSerializeStart() {
	}
}