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

package org.screamingsandals.bedwars.party;

import lombok.RequiredArgsConstructor;
import org.screamingsandals.lib.event.EventExecutionOrder;
import org.screamingsandals.lib.event.OnEvent;
import org.screamingsandals.lib.event.player.PlayerChatEvent;
import org.screamingsandals.lib.event.player.PlayerJoinEvent;
import org.screamingsandals.lib.event.player.PlayerLeaveEvent;
import org.screamingsandals.lib.tasker.DefaultThreads;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.ServiceDependencies;

/**
 * Connection events and party chat of the built-in party system.
 */
@Service
@ServiceDependencies(dependsOn = {
        PartyManagerImpl.class
})
@RequiredArgsConstructor
public class PartyEventListener {
    private final PartyManagerImpl manager;

    @OnEvent
    public void onQuit(PlayerLeaveEvent event) {
        manager.handleQuit(event.player());
    }

    @OnEvent
    public void onJoin(PlayerJoinEvent event) {
        manager.handleJoin(event.player());
    }

    /**
     * ASYNC on Bukkit. LATE: after chat filters / mute plugins, before {@code PlayerListener.onChat} (LAST, which returns
     * when the event is cancelled).
     */
    @OnEvent(order = EventExecutionOrder.LATE)
    public void onChat(PlayerChatEvent event) {
        if (event.cancelled()) {
            return;
        }
        var uuid = event.player().getUuid();
        if (!manager.isChatToggled(uuid)) { // concurrent set
            return;
        }
        var message = event.message();
        var bypass = manager.getChatBypassPrefix(); // volatile settings
        if (!bypass.isEmpty() && message.startsWith(bypass)) {
            var rest = message.substring(bypass.length()).trim();
            if (!rest.isEmpty()) {
                event.message(rest); // normal chat continues with the prefix stripped
            }
            return;
        }
        event.cancelled(true);
        Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> manager.sendChat(uuid, message)); // all party state is main-thread only
    }
}
