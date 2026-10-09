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

package org.screamingsandals.bedwars.lang;

/**
 * Translation keys of the fork-local language overlay ({@code bedwars-fork/languages/language_en-US.json}).
 * <p>
 * Rules (enforced by ForkLanguageOverlayTest):
 * <ul>
 *   <li>every path starts with {@link #ROOT} ("fork"); segments are snake_case {@code [a-z0-9_]+}</li>
 *   <li>constant name = path without ROOT, segments joined by '_', upper-cased</li>
 *   <li>every constant exists in the English overlay (string or list of strings) and every overlay leaf has a constant</li>
 * </ul>
 * Usage: {@code Message.of(ForkLangKeys.COMMON_FEATURE_DISABLED)}; in YAML: {@code "@fork.common.feature_disabled"};
 * inside MiniMessage: {@code <bw-lang:fork.common.feature_disabled>}
 */
public final class ForkLangKeys {
    public static final String ROOT = "fork";

    // ---------------- common ----------------
    public static final String[] COMMON_FEATURE_DISABLED = {ROOT, "common", "feature_disabled"};

    // ---------------- in_game.timeline ----------------
    public static final String[] IN_GAME_TIMELINE_EVENT_SPAWNER_TIER = {ROOT, "in_game", "timeline", "event", "spawner_tier"};
    public static final String[] IN_GAME_TIMELINE_EVENT_BED_DESTRUCTION = {ROOT, "in_game", "timeline", "event", "bed_destruction"};
    public static final String[] IN_GAME_TIMELINE_EVENT_SUDDEN_DEATH = {ROOT, "in_game", "timeline", "event", "sudden_death"};
    public static final String[] IN_GAME_TIMELINE_EVENT_GAME_END = {ROOT, "in_game", "timeline", "event", "game_end"};
    public static final String[] IN_GAME_TIMELINE_EVENT_ANNOUNCEMENT = {ROOT, "in_game", "timeline", "event", "announcement"};
    public static final String[] IN_GAME_TIMELINE_SIDEBAR_NEXT_EVENT = {ROOT, "in_game", "timeline", "sidebar", "next_event"};
    public static final String[] IN_GAME_TIMELINE_SIDEBAR_TIME_LIMIT = {ROOT, "in_game", "timeline", "sidebar", "time_limit"};
    public static final String[] IN_GAME_TIMELINE_SIDEBAR_NONE = {ROOT, "in_game", "timeline", "sidebar", "none"};
    public static final String[] IN_GAME_TIMELINE_SIDEBAR_GAME_OVER = {ROOT, "in_game", "timeline", "sidebar", "game_over"};
    public static final String[] IN_GAME_TIMELINE_SPAWNER_TIER_CHAT = {ROOT, "in_game", "timeline", "spawner_tier", "chat"};
    public static final String[] IN_GAME_TIMELINE_SPAWNER_TIER_TITLE = {ROOT, "in_game", "timeline", "spawner_tier", "title"};
    public static final String[] IN_GAME_TIMELINE_SPAWNER_TIER_SUBTITLE = {ROOT, "in_game", "timeline", "spawner_tier", "subtitle"};
    public static final String[] IN_GAME_TIMELINE_ANNOUNCEMENT_CHAT = {ROOT, "in_game", "timeline", "announcement", "chat"};
    public static final String[] IN_GAME_TIMELINE_ANNOUNCEMENT_TITLE = {ROOT, "in_game", "timeline", "announcement", "title"};

    // ---------------- in_game.endgame ----------------
    public static final String[] IN_GAME_ENDGAME_BED_DESTRUCTION_TITLE_BED = {ROOT, "in_game", "endgame", "bed_destruction", "title", "bed"};
    public static final String[] IN_GAME_ENDGAME_BED_DESTRUCTION_TITLE_ANCHOR = {ROOT, "in_game", "endgame", "bed_destruction", "title", "anchor"};
    public static final String[] IN_GAME_ENDGAME_BED_DESTRUCTION_TITLE_CAKE = {ROOT, "in_game", "endgame", "bed_destruction", "title", "cake"};
    public static final String[] IN_GAME_ENDGAME_BED_DESTRUCTION_TITLE_ANY = {ROOT, "in_game", "endgame", "bed_destruction", "title", "any"};
    public static final String[] IN_GAME_ENDGAME_BED_DESTRUCTION_CHAT = {ROOT, "in_game", "endgame", "bed_destruction", "chat"};
    public static final String[] IN_GAME_ENDGAME_SUDDEN_DEATH_TITLE = {ROOT, "in_game", "endgame", "sudden_death", "title"};
    public static final String[] IN_GAME_ENDGAME_SUDDEN_DEATH_SUBTITLE = {ROOT, "in_game", "endgame", "sudden_death", "subtitle"};
    public static final String[] IN_GAME_ENDGAME_SUDDEN_DEATH_CHAT = {ROOT, "in_game", "endgame", "sudden_death", "chat"};
    public static final String[] IN_GAME_ENDGAME_SUDDEN_DEATH_DRAGON_BUFF = {ROOT, "in_game", "endgame", "sudden_death", "dragon_buff"};
    public static final String[] IN_GAME_ENDGAME_SUDDEN_DEATH_BOSSBAR = {ROOT, "in_game", "endgame", "sudden_death", "bossbar"};
    public static final String[] IN_GAME_ENDGAME_DRAGON_NAME = {ROOT, "in_game", "endgame", "dragon", "name"};
    public static final String[] IN_GAME_ENDGAME_DRAGON_NAME_NEUTRAL = {ROOT, "in_game", "endgame", "dragon", "name_neutral"};
    public static final String[] IN_GAME_ENDGAME_DRAGON_SLAIN = {ROOT, "in_game", "endgame", "dragon", "slain"};
    public static final String[] IN_GAME_ENDGAME_DRAW_TITLE = {ROOT, "in_game", "endgame", "draw", "title"};
    public static final String[] IN_GAME_ENDGAME_DRAW_SUBTITLE = {ROOT, "in_game", "endgame", "draw", "subtitle"};
    public static final String[] IN_GAME_ENDGAME_DRAW_CHAT = {ROOT, "in_game", "endgame", "draw", "chat"};
    public static final String[] IN_GAME_ENDGAME_TIE_BREAK_CHAT = {ROOT, "in_game", "endgame", "tie_break", "chat"};
    public static final String[] IN_GAME_ENDGAME_TIE_BREAK_REASON_TARGET = {ROOT, "in_game", "endgame", "tie_break", "reason", "target"};
    public static final String[] IN_GAME_ENDGAME_TIE_BREAK_REASON_PLAYERS = {ROOT, "in_game", "endgame", "tie_break", "reason", "players"};
    public static final String[] IN_GAME_ENDGAME_TIE_BREAK_REASON_KILLS = {ROOT, "in_game", "endgame", "tie_break", "reason", "kills"};
    public static final String[] IN_GAME_ENDGAME_TIE_BREAK_REASON_FINAL_KILLS = {ROOT, "in_game", "endgame", "tie_break", "reason", "final_kills"};

    // ---------------- in_game.upgrades ----------------
    public static final String[] IN_GAME_UPGRADES_PURCHASED = {ROOT, "in_game", "upgrades", "purchased"};
    public static final String[] IN_GAME_UPGRADES_PURCHASED_TIER = {ROOT, "in_game", "upgrades", "purchased_tier"};
    public static final String[] IN_GAME_UPGRADES_TRAP_PURCHASED = {ROOT, "in_game", "upgrades", "trap_purchased"};
    public static final String[] IN_GAME_UPGRADES_MAXED = {ROOT, "in_game", "upgrades", "maxed"};
    public static final String[] IN_GAME_UPGRADES_TRAP_QUEUE_FULL = {ROOT, "in_game", "upgrades", "trap_queue_full"};
    public static final String[] IN_GAME_UPGRADES_TRAP_ALREADY_QUEUED = {ROOT, "in_game", "upgrades", "trap_already_queued"};
    public static final String[] IN_GAME_UPGRADES_NOTHING_TO_UPGRADE = {ROOT, "in_game", "upgrades", "nothing_to_upgrade"};
    public static final String[] IN_GAME_UPGRADES_UNAVAILABLE = {ROOT, "in_game", "upgrades", "unavailable"};
    public static final String[] IN_GAME_UPGRADES_CANCELLED = {ROOT, "in_game", "upgrades", "cancelled"};
    public static final String[] IN_GAME_UPGRADES_LORE_TIER_OWNED = {ROOT, "in_game", "upgrades", "lore", "tier_owned"};
    public static final String[] IN_GAME_UPGRADES_LORE_TIER_NEXT = {ROOT, "in_game", "upgrades", "lore", "tier_next"};
    public static final String[] IN_GAME_UPGRADES_LORE_TIER_LOCKED = {ROOT, "in_game", "upgrades", "lore", "tier_locked"};
    public static final String[] IN_GAME_UPGRADES_LORE_COST = {ROOT, "in_game", "upgrades", "lore", "cost"};
    public static final String[] IN_GAME_UPGRADES_LORE_CLICK_TO_BUY = {ROOT, "in_game", "upgrades", "lore", "click_to_buy"};
    public static final String[] IN_GAME_UPGRADES_LORE_MAXED = {ROOT, "in_game", "upgrades", "lore", "maxed"};
    public static final String[] IN_GAME_UPGRADES_LORE_CANNOT_AFFORD = {ROOT, "in_game", "upgrades", "lore", "cannot_afford"};
    public static final String[] IN_GAME_UPGRADES_LORE_UNAVAILABLE = {ROOT, "in_game", "upgrades", "lore", "unavailable"};
    public static final String[] IN_GAME_UPGRADES_LORE_TRAP_QUEUE = {ROOT, "in_game", "upgrades", "lore", "trap_queue"};
    public static final String[] IN_GAME_UPGRADES_LORE_TRAP_QUEUE_FULL = {ROOT, "in_game", "upgrades", "lore", "trap_queue_full"};
    public static final String[] IN_GAME_UPGRADES_LORE_TRAP_ALREADY_QUEUED = {ROOT, "in_game", "upgrades", "lore", "trap_already_queued"};
    public static final String[] IN_GAME_UPGRADES_TRAP_QUEUE_SLOT_EMPTY_NAME = {ROOT, "in_game", "upgrades", "trap_queue_slot", "empty_name"};
    public static final String[] IN_GAME_UPGRADES_TRAP_QUEUE_SLOT_EMPTY_LORE = {ROOT, "in_game", "upgrades", "trap_queue_slot", "empty_lore"};
    public static final String[] IN_GAME_UPGRADES_TRAP_QUEUE_SLOT_FILLED_NAME = {ROOT, "in_game", "upgrades", "trap_queue_slot", "filled_name"};
    public static final String[] IN_GAME_UPGRADES_TRAP_QUEUE_SLOT_FILLED_LORE = {ROOT, "in_game", "upgrades", "trap_queue_slot", "filled_lore"};

    // ---------------- in_game.traps ----------------
    public static final String[] IN_GAME_TRAPS_TRIGGERED_TITLE = {ROOT, "in_game", "traps", "triggered_title"};
    public static final String[] IN_GAME_TRAPS_TRIGGERED_SUBTITLE = {ROOT, "in_game", "traps", "triggered_subtitle"};
    public static final String[] IN_GAME_TRAPS_TRIGGERED_TEAM = {ROOT, "in_game", "traps", "triggered_team"};
    public static final String[] IN_GAME_TRAPS_ALARM_TEAM = {ROOT, "in_game", "traps", "alarm_team"};
    public static final String[] IN_GAME_TRAPS_TRIGGERED_INTRUDER = {ROOT, "in_game", "traps", "triggered_intruder"};
    public static final String[] IN_GAME_TRAPS_NAMES_ITS_A_TRAP = {ROOT, "in_game", "traps", "names", "its_a_trap"};
    public static final String[] IN_GAME_TRAPS_NAMES_COUNTER_OFFENSIVE = {ROOT, "in_game", "traps", "names", "counter_offensive"};
    public static final String[] IN_GAME_TRAPS_NAMES_ALARM = {ROOT, "in_game", "traps", "names", "alarm"};
    public static final String[] IN_GAME_TRAPS_NAMES_MINER_FATIGUE = {ROOT, "in_game", "traps", "names", "miner_fatigue"};

    // ---------------- party ----------------
    public static final String[] PARTY_HELP = {ROOT, "party", "help"};
    public static final String[] PARTY_INVITE_SENT = {ROOT, "party", "invite", "sent"};
    public static final String[] PARTY_INVITE_SENT_OTHERS = {ROOT, "party", "invite", "sent_others"};
    public static final String[] PARTY_INVITE_RECEIVED = {ROOT, "party", "invite", "received"};
    public static final String[] PARTY_INVITE_RECEIVED_MEMBER = {ROOT, "party", "invite", "received_member"};
    public static final String[] PARTY_INVITE_BUTTON_ACCEPT = {ROOT, "party", "invite", "button_accept"};
    public static final String[] PARTY_INVITE_BUTTON_ACCEPT_HOVER = {ROOT, "party", "invite", "button_accept_hover"};
    public static final String[] PARTY_INVITE_BUTTON_DENY = {ROOT, "party", "invite", "button_deny"};
    public static final String[] PARTY_INVITE_BUTTON_DENY_HOVER = {ROOT, "party", "invite", "button_deny_hover"};
    public static final String[] PARTY_INVITE_EXPIRED_INVITER = {ROOT, "party", "invite", "expired_inviter"};
    public static final String[] PARTY_INVITE_EXPIRED_TARGET = {ROOT, "party", "invite", "expired_target"};
    public static final String[] PARTY_INVITE_DENIED_INVITER = {ROOT, "party", "invite", "denied_inviter"};
    public static final String[] PARTY_INVITE_DENIED_TARGET = {ROOT, "party", "invite", "denied_target"};
    public static final String[] PARTY_MEMBER_JOINED = {ROOT, "party", "member", "joined"};
    public static final String[] PARTY_MEMBER_JOINED_SELF = {ROOT, "party", "member", "joined_self"};
    public static final String[] PARTY_MEMBER_LEFT = {ROOT, "party", "member", "left"};
    public static final String[] PARTY_MEMBER_LEFT_SELF = {ROOT, "party", "member", "left_self"};
    public static final String[] PARTY_MEMBER_KICKED = {ROOT, "party", "member", "kicked"};
    public static final String[] PARTY_MEMBER_KICKED_SELF = {ROOT, "party", "member", "kicked_self"};
    public static final String[] PARTY_MEMBER_DISCONNECTED = {ROOT, "party", "member", "disconnected"};
    public static final String[] PARTY_MEMBER_DISCONNECTED_NO_TIMEOUT = {ROOT, "party", "member", "disconnected_no_timeout"};
    public static final String[] PARTY_MEMBER_RECONNECTED = {ROOT, "party", "member", "reconnected"};
    public static final String[] PARTY_MEMBER_REMOVED_OFFLINE = {ROOT, "party", "member", "removed_offline"};
    public static final String[] PARTY_LEADER_DISCONNECTED_TRANSFER = {ROOT, "party", "leader", "disconnected_transfer"};
    public static final String[] PARTY_LEADER_DISCONNECTED_DISBAND = {ROOT, "party", "leader", "disconnected_disband"};
    public static final String[] PARTY_LEADER_DISCONNECTED_NO_TIMEOUT = {ROOT, "party", "leader", "disconnected_no_timeout"};
    public static final String[] PARTY_LEADER_CHANGED = {ROOT, "party", "leader", "changed"};
    public static final String[] PARTY_LEADER_TRANSFERRED = {ROOT, "party", "leader", "transferred"};
    public static final String[] PARTY_LEADER_TRANSFERRED_OFFLINE = {ROOT, "party", "leader", "transferred_offline"};
    public static final String[] PARTY_DISBAND_BY_LEADER = {ROOT, "party", "disband", "by_leader"};
    public static final String[] PARTY_DISBAND_EMPTY = {ROOT, "party", "disband", "empty"};
    public static final String[] PARTY_DISBAND_LEADER_OFFLINE = {ROOT, "party", "disband", "leader_offline"};
    public static final String[] PARTY_DISBAND_LEADER_LEFT = {ROOT, "party", "disband", "leader_left"};
    public static final String[] PARTY_LIST_HEADER = {ROOT, "party", "list", "header"};
    public static final String[] PARTY_LIST_LEADER = {ROOT, "party", "list", "leader"};
    public static final String[] PARTY_LIST_MEMBERS = {ROOT, "party", "list", "members"};
    public static final String[] PARTY_LIST_INVITES = {ROOT, "party", "list", "invites"};
    public static final String[] PARTY_LIST_ENTRY_ONLINE = {ROOT, "party", "list", "entry_online"};
    public static final String[] PARTY_LIST_ENTRY_OFFLINE = {ROOT, "party", "list", "entry_offline"};
    public static final String[] PARTY_LIST_ENTRY_IN_GAME = {ROOT, "party", "list", "entry_in_game"};
    public static final String[] PARTY_LIST_SEPARATOR = {ROOT, "party", "list", "separator"};
    public static final String[] PARTY_LIST_NONE = {ROOT, "party", "list", "none"};
    public static final String[] PARTY_LIST_UNLIMITED = {ROOT, "party", "list", "unlimited"};
    public static final String[] PARTY_CHAT_FORMAT = {ROOT, "party", "chat", "format"};
    public static final String[] PARTY_CHAT_TOGGLED_ON = {ROOT, "party", "chat", "toggled_on"};
    public static final String[] PARTY_CHAT_TOGGLED_OFF = {ROOT, "party", "chat", "toggled_off"};
    public static final String[] PARTY_CHAT_TOGGLED_OFF_NO_PARTY = {ROOT, "party", "chat", "toggled_off_no_party"};
    public static final String[] PARTY_JOIN_ONLY_LEADER = {ROOT, "party", "join", "only_leader"};
    public static final String[] PARTY_JOIN_NO_ROOM = {ROOT, "party", "join", "no_room"};
    public static final String[] PARTY_JOIN_TOO_BIG_FOR_TEAM = {ROOT, "party", "join", "too_big_for_team"};
    public static final String[] PARTY_JOIN_MEMBER_BUSY = {ROOT, "party", "join", "member_busy"};
    public static final String[] PARTY_JOIN_MEMBER_BUSY_LEADER = {ROOT, "party", "join", "member_busy_leader"};
    public static final String[] PARTY_JOIN_MEMBER_FAILED = {ROOT, "party", "join", "member_failed"};
    public static final String[] PARTY_WARP_GAME_STARTED = {ROOT, "party", "warp", "game_started"};
    public static final String[] PARTY_ERROR_PLAYER_NOT_FOUND = {ROOT, "party", "error", "player_not_found"};
    public static final String[] PARTY_ERROR_CANNOT_INVITE_SELF = {ROOT, "party", "error", "cannot_invite_self"};
    public static final String[] PARTY_ERROR_ALREADY_INVITED = {ROOT, "party", "error", "already_invited"};
    public static final String[] PARTY_ERROR_TARGET_IN_PARTY = {ROOT, "party", "error", "target_in_party"};
    public static final String[] PARTY_ERROR_ALREADY_MEMBER = {ROOT, "party", "error", "already_member"};
    public static final String[] PARTY_ERROR_ALREADY_IN_PARTY = {ROOT, "party", "error", "already_in_party"};
    public static final String[] PARTY_ERROR_PARTY_FULL = {ROOT, "party", "error", "party_full"};
    public static final String[] PARTY_ERROR_NO_INVITE = {ROOT, "party", "error", "no_invite"};
    public static final String[] PARTY_ERROR_NO_INVITE_FROM = {ROOT, "party", "error", "no_invite_from"};
    public static final String[] PARTY_ERROR_NOT_A_MEMBER = {ROOT, "party", "error", "not_a_member"};
    public static final String[] PARTY_ERROR_CANNOT_KICK_SELF = {ROOT, "party", "error", "cannot_kick_self"};
    public static final String[] PARTY_ERROR_CANNOT_TRANSFER_SELF = {ROOT, "party", "error", "cannot_transfer_self"};
    public static final String[] PARTY_ERROR_TARGET_OFFLINE = {ROOT, "party", "error", "target_offline"};

    // ---------------- modes ----------------
    public static final String[] MODES_UNKNOWN_MODE = {ROOT, "modes", "unknown_mode"};
    public static final String[] MODES_NO_ARENA_AVAILABLE = {ROOT, "modes", "no_arena_available"};
    public static final String[] MODES_NO_ARENA_CONFIGURED = {ROOT, "modes", "no_arena_configured"};
    public static final String[] MODES_PARTY_TOO_LARGE = {ROOT, "modes", "party_too_large"};
    public static final String[] MODES_PARTY_LEADER_ONLY = {ROOT, "modes", "party_leader_only"};
    public static final String[] MODES_JOINING = {ROOT, "modes", "joining"};
    public static final String[] MODES_ARENA_MODE_SELECTION_ONLY = {ROOT, "modes", "arena_mode_selection_only"};
    public static final String[] MODES_TEAM_SELECTION_DISABLED = {ROOT, "modes", "team_selection_disabled"};
    public static final String[] MODES_PARTY_TEAM_CHOICE_LEADER_ONLY = {ROOT, "modes", "party_team_choice_leader_only"};
    public static final String[] MODES_PARTY_TEAM_FULL = {ROOT, "modes", "party_team_full"};
    public static final String[] MODES_PARTY_SPLIT = {ROOT, "modes", "party_split"};
    public static final String[] MODES_MAIN_LOBBY_NOT_SET = {ROOT, "modes", "main_lobby_not_set"};
    public static final String[] MODES_QUEUE_JOINED = {ROOT, "modes", "queue", "joined"};
    public static final String[] MODES_QUEUE_ALREADY = {ROOT, "modes", "queue", "already"};
    public static final String[] MODES_QUEUE_LEFT = {ROOT, "modes", "queue", "left"};
    public static final String[] MODES_QUEUE_EXPIRED = {ROOT, "modes", "queue", "expired"};
    public static final String[] MODES_QUEUE_NOT_QUEUED = {ROOT, "modes", "queue", "not_queued"};
    public static final String[] MODES_LIST_HEADER = {ROOT, "modes", "list", "header"};
    public static final String[] MODES_LIST_ENTRY = {ROOT, "modes", "list", "entry"};
    public static final String[] MODES_ADMIN_NPC_SPAWNED = {ROOT, "modes", "admin", "npc_spawned"};
    public static final String[] MODES_ADMIN_NO_MODES = {ROOT, "modes", "admin", "no_modes"};

    // ---------------- setup ----------------
    public static final String[] SETUP_USAGE = {ROOT, "setup", "usage"};
    public static final String[] SETUP_SELECTED = {ROOT, "setup", "selected"};
    public static final String[] SETUP_NO_SELECTION = {ROOT, "setup", "no_selection"};
    public static final String[] SETUP_NOT_IN_EDIT = {ROOT, "setup", "not_in_edit"};
    public static final String[] SETUP_ARENA_HAS_PLAYERS = {ROOT, "setup", "arena_has_players"};
    public static final String[] SETUP_INVALID_NAME = {ROOT, "setup", "invalid_name"};
    public static final String[] SETUP_CANCELLED_NEW = {ROOT, "setup", "cancelled_new"};
    public static final String[] SETUP_CANCELLED_RELOADED = {ROOT, "setup", "cancelled_reloaded"};
    public static final String[] SETUP_CANCEL_RELOAD_FAILED = {ROOT, "setup", "cancel_reload_failed"};
    public static final String[] SETUP_VARIANT_CHANGED = {ROOT, "setup", "variant_changed"};
    public static final String[] SETUP_VARIANT_UNCHANGED = {ROOT, "setup", "variant_unchanged"};
    public static final String[] SETUP_VARIANT_UNKNOWN_SPAWNER_TYPE = {ROOT, "setup", "variant_unknown_spawner_type"};
    public static final String[] SETUP_SAVE_BLOCKED = {ROOT, "setup", "save_blocked"};
    public static final String[] SETUP_LOBBY_REGION_CLEARED = {ROOT, "setup", "lobby_region_cleared"};
    public static final String[] SETUP_NEXT_STEP = {ROOT, "setup", "next_step"};
    public static final String[] SETUP_ALL_REQUIRED_DONE = {ROOT, "setup", "all_required_done"};
    public static final String[] SETUP_CLICK_TO_SUGGEST = {ROOT, "setup", "click_to_suggest"};
    public static final String[] SETUP_STATUS_HEADER = {ROOT, "setup", "status", "header"};
    public static final String[] SETUP_STATUS_MISSING = {ROOT, "setup", "status", "missing"};
    public static final String[] SETUP_STATUS_RECOMMENDED = {ROOT, "setup", "status", "recommended"};
    public static final String[] SETUP_STATUS_OPTIONAL = {ROOT, "setup", "status", "optional"};
    public static final String[] SETUP_STATUS_EVERYTHING_DONE = {ROOT, "setup", "status", "everything_done"};
    public static final String[] SETUP_ITEM_BOUNDS = {ROOT, "setup", "item", "bounds"};
    public static final String[] SETUP_ITEM_LOBBY = {ROOT, "setup", "item", "lobby"};
    public static final String[] SETUP_ITEM_LOBBY_REGION = {ROOT, "setup", "item", "lobby_region"};
    public static final String[] SETUP_ITEM_SPECTATOR = {ROOT, "setup", "item", "spectator"};
    public static final String[] SETUP_ITEM_TEAMS = {ROOT, "setup", "item", "teams"};
    public static final String[] SETUP_ITEM_TEAM_SPAWN = {ROOT, "setup", "item", "team_spawn"};
    public static final String[] SETUP_ITEM_TEAM_TARGET = {ROOT, "setup", "item", "team_target"};
    public static final String[] SETUP_ITEM_TEAM_GENERATOR = {ROOT, "setup", "item", "team_generator"};
    public static final String[] SETUP_ITEM_TEAM_SHOP = {ROOT, "setup", "item", "team_shop"};
    public static final String[] SETUP_ITEM_TEAM_UPGRADES = {ROOT, "setup", "item", "team_upgrades"};
    public static final String[] SETUP_ITEM_SPAWNERS = {ROOT, "setup", "item", "spawners"};
    public static final String[] SETUP_ITEM_SHOPS = {ROOT, "setup", "item", "shops"};
    public static final String[] SETUP_ITEM_STORE_COUNT = {ROOT, "setup", "item", "store_count"};
    public static final String[] SETUP_ITEM_UPGRADE_SHOPS = {ROOT, "setup", "item", "upgrade_shops"};
    public static final String[] SETUP_ITEM_DIAMOND = {ROOT, "setup", "item", "diamond"};
    public static final String[] SETUP_ITEM_EMERALD = {ROOT, "setup", "item", "emerald"};
    public static final String[] SETUP_SET_TEAM_COLOR_USED = {ROOT, "setup", "set", "team_color_used"};
    public static final String[] SETUP_SET_NO_BED_FOUND = {ROOT, "setup", "set", "no_bed_found"};
    public static final String[] SETUP_SET_NO_TARGET_BLOCK = {ROOT, "setup", "set", "no_target_block"};
    public static final String[] SETUP_SET_GENERATORS_REPLACED = {ROOT, "setup", "set", "generators_replaced"};
    public static final String[] SETUP_SET_SPAWNER_ALREADY_HERE = {ROOT, "setup", "set", "spawner_already_here"};
    public static final String[] SETUP_SET_UPGRADE_SHOP_FILE_MISSING = {ROOT, "setup", "set", "upgrade_shop_file_missing"};

    // ---------------- clone ----------------
    public static final String[] CLONE_PREVIEW_HEADER = {ROOT, "clone", "preview", "header"};
    public static final String[] CLONE_PREVIEW_SOURCE = {ROOT, "clone", "preview", "source"};
    public static final String[] CLONE_PREVIEW_TARGET = {ROOT, "clone", "preview", "target"};
    public static final String[] CLONE_PREVIEW_BLOCKS = {ROOT, "clone", "preview", "blocks"};
    public static final String[] CLONE_PREVIEW_LOBBY_MOVED = {ROOT, "clone", "preview", "lobby_moved"};
    public static final String[] CLONE_PREVIEW_LOBBY_REGION_COPIED = {ROOT, "clone", "preview", "lobby_region_copied"};
    public static final String[] CLONE_PREVIEW_LOBBY_SHARED = {ROOT, "clone", "preview", "lobby_shared"};
    public static final String[] CLONE_PREVIEW_LOBBY_REGION_DROPPED = {ROOT, "clone", "preview", "lobby_region_dropped"};
    public static final String[] CLONE_PREVIEW_PLAYERS_IN_TARGET = {ROOT, "clone", "preview", "players_in_target"};
    public static final String[] CLONE_PREVIEW_BLOCK_ENTITIES_LIMITED = {ROOT, "clone", "preview", "block_entities_limited"};
    public static final String[] CLONE_PREVIEW_LIMITATIONS = {ROOT, "clone", "preview", "limitations"};
    public static final String[] CLONE_PREVIEW_ENTITIES_COPIED = {ROOT, "clone", "preview", "entities_copied"};
    public static final String[] CLONE_PREVIEW_ENTITIES_NOT_COPIED = {ROOT, "clone", "preview", "entities_not_copied"};
    public static final String[] CLONE_PREVIEW_CONFIRM = {ROOT, "clone", "preview", "confirm"};
    public static final String[] CLONE_CONFIRM_BUTTON = {ROOT, "clone", "confirm_button"};
    public static final String[] CLONE_NO_PENDING = {ROOT, "clone", "no_pending"};
    public static final String[] CLONE_STARTED = {ROOT, "clone", "started"};
    public static final String[] CLONE_PROGRESS = {ROOT, "clone", "progress"};
    public static final String[] CLONE_PHASE_BLOCK_ENTITIES = {ROOT, "clone", "phase_block_entities"};
    public static final String[] CLONE_PHASE_ENTITIES = {ROOT, "clone", "phase_entities"};
    public static final String[] CLONE_PHASE_NAME_BLOCKS = {ROOT, "clone", "phase_name", "blocks"};
    public static final String[] CLONE_PHASE_NAME_BLOCK_ENTITIES = {ROOT, "clone", "phase_name", "block_entities"};
    public static final String[] CLONE_PHASE_NAME_ENTITIES = {ROOT, "clone", "phase_name", "entities"};
    public static final String[] CLONE_PHASE_NAME_FINISHING = {ROOT, "clone", "phase_name", "finishing"};
    public static final String[] CLONE_FINISHED = {ROOT, "clone", "finished"};
    public static final String[] CLONE_FAILED = {ROOT, "clone", "failed"};
    public static final String[] CLONE_FAILED_LOAD = {ROOT, "clone", "failed_load"};
    public static final String[] CLONE_CANCELLED = {ROOT, "clone", "cancelled"};
    public static final String[] CLONE_NONE_RUNNING = {ROOT, "clone", "none_running"};
    public static final String[] CLONE_STATUS = {ROOT, "clone", "status"};
    public static final String[] CLONE_ERROR_NAME_TAKEN = {ROOT, "clone", "error", "name_taken"};
    public static final String[] CLONE_ERROR_BUSY = {ROOT, "clone", "error", "busy"};
    public static final String[] CLONE_ERROR_SOURCE_IN_USE = {ROOT, "clone", "error", "source_in_use"};
    public static final String[] CLONE_ERROR_SOURCE_INCOMPLETE = {ROOT, "clone", "error", "source_incomplete"};
    public static final String[] CLONE_ERROR_INVALID_COORDINATE = {ROOT, "clone", "error", "invalid_coordinate"};
    public static final String[] CLONE_ERROR_UNKNOWN_WORLD = {ROOT, "clone", "error", "unknown_world"};
    public static final String[] CLONE_ERROR_OUT_OF_WORLD = {ROOT, "clone", "error", "out_of_world"};
    public static final String[] CLONE_ERROR_OVERLAPS_ARENA = {ROOT, "clone", "error", "overlaps_arena"};
    public static final String[] CLONE_ERROR_OVERLAPS_SOURCE = {ROOT, "clone", "error", "overlaps_source"};
    public static final String[] CLONE_ERROR_OVERLAPS_CLONE = {ROOT, "clone", "error", "overlaps_clone"};
    public static final String[] CLONE_ERROR_SAME_PLACE = {ROOT, "clone", "error", "same_place"};
    public static final String[] CLONE_ERROR_TOO_LARGE = {ROOT, "clone", "error", "too_large"};
    public static final String[] CLONE_ERROR_JOIN_LOCKED = {ROOT, "clone", "error", "join_locked"};

    private ForkLangKeys() {
    }
}
