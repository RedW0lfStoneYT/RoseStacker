package dev.rosewood.rosestacker.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosestacker.manager.ConfigurationManager.Setting;
import dev.rosewood.rosestacker.nms.NMSAdapter;
import dev.rosewood.rosestacker.nms.NMSHandler;
import dev.rosewood.rosestacker.nms.hologram.Hologram;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;


public class HologramManager extends Manager implements Listener {

    private final Map<Location, Hologram> holograms;
    private final NMSHandler nmsHandler;
    private BukkitTask watcherTask;
    private double renderDistanceSqrd;
    private boolean hideThroughWalls;

    public HologramManager(RosePlugin rosePlugin) {
        super(rosePlugin);

        this.holograms = new ConcurrentHashMap<>();
        this.nmsHandler = NMSAdapter.getHandler();

        Bukkit.getPluginManager().registerEvents(this, this.rosePlugin);
    }

    @Override
    public void reload() {
        this.watcherTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this.rosePlugin, this::updateWatchers, 0L, Setting.NAMETAG_UPDATE_FREQUENCY.getLong());
        this.renderDistanceSqrd = Setting.BLOCK_DYNAMIC_TAG_VIEW_RANGE.getDouble() * Setting.BLOCK_DYNAMIC_TAG_VIEW_RANGE.getDouble();
        this.hideThroughWalls = Setting.BLOCK_DYNAMIC_TAG_VIEW_RANGE_WALL_DETECTION_ENABLED.getBoolean();
    }

    @Override
    public void disable() {
        if (this.watcherTask != null) {
            this.watcherTask.cancel();
            this.watcherTask = null;
        }

        this.holograms.values().forEach(Hologram::delete);
        this.holograms.clear();
    }

    private void updateWatchers() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players)
            for (Hologram hologram : this.holograms.values())
                this.updateWatcher(player, hologram);
    }

    private void updateWatcher(Player player, Hologram hologram) {
        if (this.isPlayerInRange(player, hologram.getLocation())) {
            hologram.addWatcher(player);
            if (this.hideThroughWalls)
                hologram.setVisibility(player, this.nmsHandler.hasLineOfSight(player, hologram.getDisplayLocation()));
        } else {
            hologram.removeWatcher(player);
        }
    }

    private boolean isPlayerInRange(Player player, Location location) {
        return player.getWorld().equals(location.getWorld()) && player.getLocation().distanceSquared(location) <= this.renderDistanceSqrd;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.rosePlugin, () -> {
            Player player = event.getPlayer();
            for (Hologram hologram : this.holograms.values())
                this.updateWatcher(player, hologram);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.rosePlugin, () -> {
            Player player = event.getPlayer();
            for (Hologram hologram : this.holograms.values())
                hologram.removeWatcher(player);
        });
    }

    /**
     * Creates or updates a hologram at the given location
     *
     * @param location The location of the hologram
     * @param text The text for the hologram
     */
    public void createOrUpdateHologram(Location location, String text) {
        Hologram hologram = this.holograms.get(location);
        if (hologram == null) {
            hologram = this.nmsHandler.createHologram(location, text);
            this.holograms.put(location, hologram);
            for (Player player : Bukkit.getOnlinePlayers())
                this.updateWatcher(player, hologram);
        } else {
            hologram.setText(text);
        }
    }

    /**
     * Deletes a hologram at a given location if one exists
     *
     * @param location The location of the hologram
     */
    public void deleteHologram(Location location) {
        Hologram hologram = this.holograms.get(location);
        if (hologram != null) {
            hologram.delete();
            this.holograms.remove(location);
        }
    }

}
