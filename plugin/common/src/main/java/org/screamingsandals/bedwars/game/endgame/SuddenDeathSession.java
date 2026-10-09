/*
 * Copyright (C) 2026 ScreamingSandals
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

package org.screamingsandals.bedwars.game.endgame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.PlatformService;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.entities.EntitiesManagerImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.endgame.dragon.DragonSteering;
import org.screamingsandals.bedwars.game.endgame.dragon.DragonTargeting;
import org.screamingsandals.bedwars.game.endgame.dragon.FlightBox;
import org.screamingsandals.bedwars.game.endgame.dragon.FlightGeometry;
import org.screamingsandals.bedwars.game.endgame.dragon.Vec3;
import org.screamingsandals.bedwars.lib.debug.Debug;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.lib.Server;
import org.screamingsandals.lib.entity.Entities;
import org.screamingsandals.lib.entity.Entity;
import org.screamingsandals.lib.entity.LivingEntity;
import org.screamingsandals.lib.entity.type.EntityType;
import org.screamingsandals.lib.tasker.DefaultThreads;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.tasker.task.Task;
import org.screamingsandals.lib.tasker.task.TaskBase;
import org.screamingsandals.lib.utils.math.Vector3D;
import org.screamingsandals.lib.world.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runtime state of one running sudden death (one per game). All dragons of a session are spawned, flown by a
 * per-tick task, watched by a one-second watchdog and removed by {@link #stop()}.
 */
final class SuddenDeathSession {
    static final int RETARGET_TICKS = 10;
    static final double TARGET_STICKINESS = 1.5;
    static final double TARGET_Y_OFFSET = 0.5;
    static final int MAX_RESPAWNS = 3;

    final @NotNull GameEndgameService service;
    final @NotNull GameImpl game;
    final @NotNull EndgameSettings settings;
    /**
     * Service-wide map entity uuid to dragon (shared).
     */
    final @NotNull Map<UUID, ManagedDragon> registry;
    final @NotNull FlightBox flightBox;
    final double cruiseY;
    final @NotNull Vec3 spawnCentre;
    final double ringRadius;
    final @NotNull DragonSteering.Params params;
    final @NotNull List<ManagedDragon> dragons = new CopyOnWriteArrayList<>();
    final @NotNull Random random = new Random();
    @Nullable Task watchdog;
    volatile boolean stopped;
    /**
     * Set and cleared by GameEndgameService#acquireMobGriefing / #releaseMobGriefing.
     */
    boolean mobGriefingAcquired;
    /**
     * Set after teleportSync threw UnsupportedOperationException (Folia).
     */
    private boolean asyncTeleport;

    SuddenDeathSession(@NotNull GameEndgameService service, @NotNull GameImpl game, @NotNull EndgameSettings settings,
                       @NotNull Map<UUID, ManagedDragon> registry) {
        this.service = service;
        this.game = game;
        this.settings = settings;
        this.registry = registry;

        var p1 = game.getPos1();
        var p2 = game.getPos2();
        var arena = FlightBox.of(p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ());
        double avgSpawnY = averageTeamSpawnY(game, arena.centre().y());
        this.flightBox = FlightGeometry.flightBox(arena, avgSpawnY, settings.boundsMargin(), settings.cruiseHeight());
        this.cruiseY = FlightGeometry.cruiseY(flightBox, avgSpawnY, settings.cruiseHeight());
        var centre = flightBox.centre();
        this.spawnCentre = new Vec3(centre.x(), cruiseY, centre.z());
        this.ringRadius = FlightGeometry.ringRadius(flightBox);
        this.params = DragonSteering.Params.of(settings.dragonSpeed(), settings.dragonTurnRate());
    }

    /**
     * Average Y of all spawn points of the teams in game; {@code fallback} when there are none.
     */
    static double averageTeamSpawnY(@NotNull GameImpl game, double fallback) {
        double sum = 0;
        int count = 0;
        for (var team : game.getActiveTeams()) {
            for (var location : team.getTeamSpawns()) {
                sum += location.getY();
                count++;
            }
        }
        return count == 0 ? fallback : sum / count;
    }

    /**
     * Spawns a dragon; must be called on the server thread.
     */
    @Nullable ManagedDragon spawnDragon(@Nullable TeamImpl owner, @NotNull Vec3 at, int respawnCount) {
        if (stopped) {
            return null;
        }
        var location = new Location(at.x(), at.y(), at.z(), 0f, 0f, game.getWorld());
        var chunk = location.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }
        var preSpawned = new Entity[1];
        var spawned = Entities.spawn(EntityType.of("ender_dragon"), location, pre -> {
            preSpawned[0] = pre;
            pre.setPersistent(false); // never saved: gone after a crash/restart
            pre.setInvulnerable(settings.dragonInvulnerable());
            if (pre instanceof LivingEntity living) {
                living.setRemoveWhenFarAway(false);
            }
            if (settings.showName()) {
                pre.setCustomName(owner != null ? service.ownerName(owner) : service.neutralName());
                pre.setCustomNameVisible(true);
            }
            // registered before the entity is added so that WorldListener#onBedWarsSpawncancelled can un-cancel CUSTOM spawns of game entities
            EntitiesManagerImpl.getInstance().addEntityToGame(pre, game);
        });
        if (spawned == null) {
            if (preSpawned[0] != null) {
                EntitiesManagerImpl.getInstance().removeEntityFromGame(preSpawned[0]);
            }
            Debug.warn(game.getName() + ": could not spawn a sudden death dragon", true);
            return null;
        }
        PlatformService.getInstance().prepareSuddenDeathDragon(spawned); // Bukkit: phase HOVER + hide the vanilla boss bar
        // tangent to the ring so that the dragon starts circling instead of flying straight through the others
        double initialHeading = DragonSteering.yawTowards(spawnCentre.x() - at.x(), spawnCentre.z() - at.z()) + 90.0;
        var dragon = new ManagedDragon(this, spawned, owner, new DragonSteering.State(initialHeading), respawnCount);
        registry.put(dragon.uuid, dragon);
        dragons.add(dragon);
        dragon.task = Tasker.runRepeatedly(spawned, task -> tickDragon(dragon, task), 1, TaskerTime.TICKS);
        return dragon;
    }

    /**
     * Runs every tick on the dragon's thread (the main thread on Paper/Spigot).
     */
    void tickDragon(@NotNull ManagedDragon dragon, @NotNull TaskBase task) {
        if (stopped || dragon.slain) {
            task.cancel();
            return;
        }
        if (game.getStatus() != GameStatus.RUNNING) {
            task.cancel();
            service.stopSuddenDeath(game);
            return;
        }
        var entity = dragon.entity;
        if (entity.isDead()) {
            dragon.lost = true; // the watchdog decides about a respawn
            task.cancel();
            return;
        }
        dragon.ticks++;
        var current = entity.getLocation();
        var position = new Vec3(current.getX(), current.getY(), current.getZ());

        if (dragon.ticks % RETARGET_TICKS == 1 || dragon.target == null || resolveTarget(dragon, dragon.target) == null) {
            dragon.target = DragonTargeting.choose(position, candidates(dragon), dragon.target, TARGET_STICKINESS);
        }
        Vec3 targetPosition = null;
        if (dragon.target != null) {
            var prey = resolveTarget(dragon, dragon.target);
            if (prey != null) {
                var preyLocation = prey.getLocation();
                targetPosition = new Vec3(preyLocation.getX(), preyLocation.getY() + TARGET_Y_OFFSET, preyLocation.getZ());
            } else {
                dragon.target = null;
            }
        }

        var step = DragonSteering.step(dragon.steering, position, targetPosition, flightBox, params, random);
        var next = new Location(
                step.position().x(), step.position().y(), step.position().z(),
                DragonSteering.entityYaw(step.headingYaw()), 0f,
                current.getWorld()
        );
        teleport(entity, next);
        entity.setVelocity(new Vector3D(0, 0, 0)); // a new instance: Vector3D is mutable
    }

    private void teleport(@NotNull Entity entity, @NotNull Location next) {
        if (!asyncTeleport) {
            try {
                entity.teleportSync(next);
                return;
            } catch (UnsupportedOperationException ex) {
                asyncTeleport = true; // Folia: synchronous teleports are not allowed
            }
        }
        entity.teleport(next);
    }

    /**
     * In-game, non-spectator player of this game who is still a valid prey for this dragon; null otherwise.
     */
    private @Nullable BedWarsPlayer resolveTarget(@NotNull ManagedDragon dragon, @NotNull UUID uuid) {
        var player = PlayerManagerImpl.getInstance().getPlayer(uuid).orElse(null);
        if (player == null || player.getGame() != game || player.isSpectator()) {
            return null;
        }
        var team = game.getPlayerTeam(player);
        if (team == null || (!dragon.isNeutral() && team == dragon.owner)) {
            return null;
        }
        return player;
    }

    private @NotNull List<DragonTargeting.Candidate> candidates(@NotNull ManagedDragon dragon) {
        var list = new ArrayList<DragonTargeting.Candidate>();
        for (var player : game.getConnectedPlayers()) {
            if (player.isSpectator()) {
                continue;
            }
            var team = game.getPlayerTeam(player);
            if (team == null || (!dragon.isNeutral() && team == dragon.owner)) {
                continue;
            }
            var gameMode = player.getGameMode();
            if (gameMode.is("spectator") || gameMode.is("creative")) {
                continue;
            }
            var location = player.getLocation();
            if (!game.isLocationInArena(location)) {
                continue;
            }
            list.add(new DragonTargeting.Candidate(player.getUniqueId(), new Vec3(location.getX(), location.getY(), location.getZ())));
        }
        return list;
    }

    void startWatchdog() {
        watchdog = Tasker.runRepeatedly(DefaultThreads.GLOBAL_THREAD, this::watchdogTick, 20, TaskerTime.TICKS);
    }

    /**
     * Every second: respawn dragons that vanished and rename the dragons of eliminated teams.
     */
    void watchdogTick() {
        if (stopped) {
            return;
        }
        if (game.getStatus() != GameStatus.RUNNING) {
            service.stopSuddenDeath(game);
            return;
        }
        for (var dragon : List.copyOf(dragons)) {
            boolean gone = dragon.slain || dragon.lost || dragon.entity.isDead();
            if (!gone) {
                if (settings.showName() && dragon.owner != null && !dragon.owner.isAlive() && !dragon.neutralNamed) {
                    dragon.entity.setCustomName(service.neutralName()); // owner eliminated: the dragon hunts everybody
                    dragon.neutralNamed = true;
                }
                continue;
            }
            forget(dragon);
            if (!dragon.slain && settings.respawnLost() && dragon.respawnCount < MAX_RESPAWNS) {
                Debug.info(game.getName() + ": sudden death dragon vanished, spawning a replacement");
                spawnDragon(dragon.owner, spawnCentre, dragon.respawnCount + 1);
            }
        }
    }

    private void forget(@NotNull ManagedDragon dragon) {
        dragons.remove(dragon);
        registry.remove(dragon.uuid);
        if (dragon.task != null) {
            dragon.task.cancel();
        }
        EntitiesManagerImpl.getInstance().removeEntityFromGame(dragon.entity);
    }

    /**
     * Stops the sudden death and removes every dragon. Idempotent.
     */
    void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        if (watchdog != null) {
            watchdog.cancel();
        }
        for (var dragon : List.copyOf(dragons)) {
            forget(dragon);
            if (!dragon.entity.isDead()) {
                if (Server.isServerThread()) {
                    dragon.entity.remove(); // synchronous: must happen before RegionImpl#regen
                } else {
                    Tasker.run(dragon.entity, dragon.entity::remove);
                }
            }
        }
        dragons.clear();
        service.releaseMobGriefing(this);
    }

    int size() {
        return dragons.size();
    }
}
