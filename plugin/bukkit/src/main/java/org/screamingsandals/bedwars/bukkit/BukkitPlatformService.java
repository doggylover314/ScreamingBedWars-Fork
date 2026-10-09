/*
 * Copyright (C) 2025 ScreamingSandals
 *
 * This file is part of Screaming BedWars.
 *
 * Screaming BedWars is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Screaming BedWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Screaming BedWars. If not, see <https://www.gnu.org/licenses/>.
 */

package org.screamingsandals.bedwars.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.BedWarsPlugin;
import org.screamingsandals.bedwars.PlatformService;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.bukkit.hooks.BukkitBStatsMetrics;
import org.screamingsandals.bedwars.bukkit.hooks.PerWorldInventoryCompatibilityFix;
import org.screamingsandals.bedwars.bukkit.listener.LegacyWaterListener;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.lib.Server;
import org.screamingsandals.lib.block.BlockPlacement;
import org.screamingsandals.lib.block.snapshot.BlockSnapshot;
import org.screamingsandals.lib.event.player.PlayerBlockBreakEvent;
import org.screamingsandals.lib.event.player.PlayerBlockPlaceEvent;
import org.screamingsandals.lib.impl.bukkit.event.player.BukkitPlayerBlockBreakEvent;
import org.screamingsandals.lib.impl.bukkit.event.player.BukkitPlayerBlockPlaceEvent;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.tasker.DefaultThreads;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.tasker.task.TaskBase;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.ServiceDependencies;
import org.screamingsandals.lib.utils.annotations.methods.OnEnable;
import org.screamingsandals.lib.utils.reflect.Reflect;

import java.util.Locale;
import java.util.function.Consumer;

@Service
@ServiceDependencies(initAnother = {
        PerWorldInventoryCompatibilityFix.class,
        BukkitBStatsMetrics.class
})
public class BukkitPlatformService extends PlatformService {
    @OnEnable
    public void onEnable(@NotNull Plugin plugin) {
        if (!Server.isVersion(1, 12, 2)) {
            plugin.getServer().getPluginManager().registerEvents(new LegacyWaterListener(), plugin);
        }
    }

    @Override
    public void reloadPlugin(@NotNull CommandSender sender) {
        sender.sendMessage(Message.of(LangKeys.SAFE_RELOAD).defaultPrefix());

        GameManagerImpl.getInstance().getLocalGames().forEach(GameImpl::stop);

        var logger = BedWarsPlugin.getInstance().getLogger();
        var plugin = BedWarsPlugin.getInstance().getPluginDescription().as(JavaPlugin.class);

        Tasker.runRepeatedly(DefaultThreads.GLOBAL_THREAD, new Consumer<>() {
            public int timer = 60;

            @Override
            public void accept(@NotNull TaskBase taskBase) {
                boolean gameRuns = false;
                for (var game : GameManagerImpl.getInstance().getLocalGames()) {
                    if (game.getStatus() != GameStatus.DISABLED) {
                        gameRuns = true;
                        break;
                    }
                }

                if (gameRuns && timer == 0) {
                    sender.sendMessage(Message.of(LangKeys.SAFE_RELOAD_FAILED_TO_STOP_GAME).defaultPrefix());
                }

                if (!gameRuns || timer == 0) {
                    taskBase.cancel();
                    try {
                        logger.info(String.format("Disabling %s", plugin.getDescription().getFullName()));
                        Bukkit.getPluginManager().callEvent(new PluginDisableEvent(plugin));
                        Reflect.getMethod(plugin, "setEnabled", boolean.class).invoke(false);
                    } catch (Throwable ex) {
                        logger.trace("Error occurred (in the plugin loader) while disabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
                    }

                    try {
                        Bukkit.getScheduler().cancelTasks(plugin);
                    } catch (Throwable ex) {
                        logger.trace("Error occurred (in the plugin loader) while cancelling tasks for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
                    }

                    try {
                        Bukkit.getServicesManager().unregisterAll(plugin);
                    } catch (Throwable ex) {
                        logger.trace("Error occurred (in the plugin loader) while unregistering services for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
                    }

                    try {
                        HandlerList.unregisterAll(plugin);
                    } catch (Throwable ex) {
                        logger.trace("Error occurred (in the plugin loader) while unregistering events for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
                    }

                    try {
                        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin);
                        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin);
                    } catch (Throwable ex) {
                        logger.trace("Error occurred (in the plugin loader) while unregistering plugin channels for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
                    }

                    try {
                        for (var world : Bukkit.getWorlds()) {
                            world.removePluginChunkTickets(plugin);
                        }
                    } catch (Throwable ex) {
                        logger.trace("Error occurred (in the plugin loader) while removing chunk tickets for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
                    }
                    Bukkit.getServer().getPluginManager().enablePlugin(plugin);
                    sender.sendMessage(Component.text("Plugin reloaded! Keep in mind that restarting the server is safer!"));
                    return;
                }
                timer--;
            }
        }, 20, TaskerTime.TICKS);
    }

    // TODO: slib?
    @Override
    public void spawnEffect(@NotNull org.screamingsandals.lib.world.Location location, @NotNull String value) {
        var particle = Effect.valueOf(value.toUpperCase(Locale.ROOT));
        var bukkitLoc =  location.as(Location.class);
        bukkitLoc.getWorld().playEffect(bukkitLoc, particle, 1);
    }

    @Override
    @NotNull
    public PlayerBlockPlaceEvent fireFakeBlockPlaceEvent(@NotNull BlockPlacement block, @NotNull BlockSnapshot originalState, @NotNull BlockPlacement clickedBlock, @NotNull org.screamingsandals.lib.item.ItemStack item, @NotNull org.screamingsandals.lib.player.Player player, boolean canBuild) {
        var event = new BlockPlaceEvent(block.as(Block.class), originalState.as(BlockState.class),
                clickedBlock.as(Block.class), item.as(ItemStack.class), player.as(Player.class), canBuild);
        Bukkit.getPluginManager().callEvent(event);

        return new BukkitPlayerBlockPlaceEvent(event);
    }

    @Override
    @NotNull
    public PlayerBlockBreakEvent fireFakeBlockBreakEvent(@NotNull BlockPlacement block, @NotNull org.screamingsandals.lib.player.Player player) {
        var event = new BlockBreakEvent(block.as(Block.class), player.as(Player.class));
        Bukkit.getPluginManager().callEvent(event);

        return new BukkitPlayerBlockBreakEvent(event);
    }

    @Override
    public @Nullable Object savePlatformScoreboard(@NotNull org.screamingsandals.lib.player.Player player) {
        return player.as(Player.class).getScoreboard();
    }

    @Override
    public void restorePlatformScoreboard(@NotNull org.screamingsandals.lib.player.Player player, @NotNull Object scoreboard) {
        if (scoreboard instanceof Scoreboard) {
            player.as(Player.class).setScoreboard((Scoreboard) scoreboard);
        }
    }

    @Override
    public void prepareSuddenDeathDragon(@NotNull org.screamingsandals.lib.entity.Entity dragon) {
        var bukkitEntity = dragon.as(org.bukkit.entity.Entity.class);
        if (!(bukkitEntity instanceof org.bukkit.entity.EnderDragon)) {
            return;
        }
        var enderDragon = (org.bukkit.entity.EnderDragon) bukkitEntity;
        try {
            enderDragon.setPhase(org.bukkit.entity.EnderDragon.Phase.HOVER);   // 1.9+; fires EnderDragonChangePhaseEvent (HOVER is allowed)
        } catch (Throwable ignored) {
            // 1.8: no phases; the per-tick teleport still controls the dragon
        }
        try {
            var bossBar = enderDragon.getBossBar();                            // only non-null when a DragonBattle owns it (End worlds)
            if (bossBar != null) {
                bossBar.setVisible(false);
            }
        } catch (Throwable ignored) {
        }
    }

    // The compile API (1.16.5) has neither BlockState#copy(Location) nor Entity#copy(Location) (both are Paper API), so reflection is used.
    private static final java.lang.reflect.@Nullable Method BLOCK_STATE_COPY = findMethod(BlockState.class, "copy", Location.class);
    private static final java.lang.reflect.@Nullable Method ENTITY_COPY = findMethod(org.bukkit.entity.Entity.class, "copy", Location.class);

    private static java.lang.reflect.@Nullable Method findMethod(Class<?> owner, String name, Class<?>... params) {
        try {
            return owner.getMethod(name, params);
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    @Override
    public boolean isBlockEntityCopySupported() {
        return BLOCK_STATE_COPY != null;
    }

    @Override
    public boolean copyBlockEntity(@NotNull BlockPlacement source, @NotNull BlockPlacement target) {
        if (BLOCK_STATE_COPY == null) {
            return false;
        }
        try {
            var state = source.as(Block.class).getState();
            if (!(state instanceof org.bukkit.block.TileState)) {
                return false;
            }
            var copy = (BlockState) BLOCK_STATE_COPY.invoke(state, target.location().as(Location.class));
            return copy.update(true, false);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isEntityCopySupported() {
        return ENTITY_COPY != null;
    }

    @Override
    public boolean copyEntity(@NotNull org.screamingsandals.lib.entity.Entity source, @NotNull org.screamingsandals.lib.world.Location target) {
        if (ENTITY_COPY == null) {
            return false;
        }
        try {
            var entity = source.as(org.bukkit.entity.Entity.class);
            if (entity instanceof Player) {
                return false;
            }
            if (entity instanceof org.bukkit.entity.Hanging) {
                // Entity#copy(Location) keeps the attachment block of a hanging entity, so the copy is discarded at the
                // new place ("Block-attached entity at invalid position"): spawn a fresh one and copy its properties.
                var hanging = (org.bukkit.entity.Hanging) entity;
                return source.getEntityType().spawn(target, spawned -> copyHangingData(hanging, spawned.as(org.bukkit.entity.Hanging.class))) != null;
            }
            return ENTITY_COPY.invoke(entity, target.as(Location.class)) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void copyHangingData(org.bukkit.entity.Hanging from, org.bukkit.entity.Hanging to) {
        to.setFacingDirection(from.getFacing(), true);
        if (from instanceof org.bukkit.entity.ItemFrame && to instanceof org.bukkit.entity.ItemFrame) {
            var src = (org.bukkit.entity.ItemFrame) from;
            var dst = (org.bukkit.entity.ItemFrame) to;
            dst.setItem(src.getItem(), false);
            dst.setRotation(src.getRotation());
            dst.setVisible(src.isVisible());
            dst.setFixed(src.isFixed());
            dst.setItemDropChance(src.getItemDropChance());
        } else if (from instanceof org.bukkit.entity.Painting && to instanceof org.bukkit.entity.Painting) {
            ((org.bukkit.entity.Painting) to).setArt(((org.bukkit.entity.Painting) from).getArt(), true);
        }
    }
}
