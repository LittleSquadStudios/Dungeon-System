package com.littlesquad.dungeon.internal.file;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.event.ObjectiveEvent;
import com.littlesquad.dungeon.api.event.requirement.Requirements;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RequirementsParser {
    private final FileConfiguration config;

    RequirementsParser (final FileConfiguration config) {
        this.config = config;
    }

    //Cast to 'FailingRequirement' the return for checking if the parsing encountered any error...
    public Requirements parse (final ObjectiveEvent event) {
        final class VariableRequirement {
            private int current;
            private final int objective;
            private VariableRequirement (final int objective) {
                this.objective = objective;
            }
        }
        final Map<String, Integer> baseSlayRequirements = new ConcurrentHashMap<>();
        //noinspection ClassCanBeRecord
        final class DistanceRequirement {
            private final Location loc;
            private final double distance;
            private DistanceRequirement (final Location loc,
                                         final double distance) {
                this.loc = loc;
                this.distance = distance;
            }
        }
        final List<DistanceRequirement> baseLTV = new CopyOnWriteArrayList<>();
        final class LocationPair {
            private final Location firstPos;
            private final Location lastPos;
            private LocationPair (final Location firstPos,
                                  final Location lastPos) {
                assert firstPos.getWorld().equals(lastPos.getWorld());
                this.firstPos = new Location(
                        firstPos.getWorld(),
                        Math.min(firstPos.getX(), lastPos.getX()),
                        Math.min(firstPos.getY(), lastPos.getY()),
                        Math.min(firstPos.getZ(), lastPos.getZ()));
                this.lastPos = new Location(
                        lastPos.getWorld(),
                        Math.max(firstPos.getX(), lastPos.getX()),
                        Math.max(firstPos.getY(), lastPos.getY()),
                        Math.max(firstPos.getZ(), lastPos.getZ()));
            }
        }
        final List<LocationPair> baseRTV = new CopyOnWriteArrayList<>();
        //empty key if block interaction
        final Map<Location, String> baseInteractions = new ConcurrentHashMap<>();
        final Map<String, Integer> baseItemRequirements = new ConcurrentHashMap<>();
        final class ParticipantRequirements {
            private final Object key;
            private final Map<String, VariableRequirement> slayRequirements;
            //LTV = Locations To Visit
            private final List<DistanceRequirement> nearLTV = new CopyOnWriteArrayList<>(baseLTV);
            //RTV = Regions To Visit
            private final List<LocationPair> rtv = new CopyOnWriteArrayList<>(baseRTV);
            private final Map<Location, String> interactions = new ConcurrentHashMap<>(baseInteractions);
            private final Map<String, VariableRequirement> itemRequirements;
            private final AtomicBoolean completed = new AtomicBoolean();
            private ParticipantRequirements (final Object key) {
                this.key = key;
                slayRequirements = new ConcurrentHashMap<>(baseSlayRequirements.size());
                baseSlayRequirements.forEach((s, obj) -> slayRequirements.put(s, new VariableRequirement(obj)));
                itemRequirements = new ConcurrentHashMap<>(baseItemRequirements.size());
                baseItemRequirements.forEach((s, obj) -> itemRequirements.put(s, new VariableRequirement(obj)));
            }
        }
        //key might be either a player or a party
        final Map<Object, ParticipantRequirements> participantRequirements = new ConcurrentHashMap<>();
        config.getStringList("events."
                        + event.getID()
                        + ".requirements")
                .parallelStream()
                .forEach(requirement -> {
                    final String[] words;
                    switch ((words = requirement
                            .split(" "))
                            .length) {
                        case 3:
                            switch (words[0].toLowerCase()) {
                                case "slay":
                                    final int mobNumber;
                                    try {
                                        mobNumber = Integer.parseInt(words[1]);
                                    } catch (final NumberFormatException _) {
                                        Main.getDungeonLogger().warning(Main
                                                .getMessageProvider()
                                                .getConsolePrefix()
                                                + Main
                                                .getMessageProvider()
                                                .getMessage("config.dungeon.number_format_error")
                                                + '\''
                                                + requirement
                                                + '\''
                                                + " in "
                                                + event.getDungeon().id());
                                        return;
                                    }
                                    try {
                                        EntityType.valueOf(words[2]);
                                    } catch (final IllegalArgumentException _) {
                                        if (MythicBukkit
                                                .inst()
                                                .getMobManager()
                                                .getMythicMob(words[2])
                                                .isEmpty()) {
                                            Main.getDungeonLogger().warning(Main
                                                    .getMessageProvider()
                                                    .getConsolePrefix()
                                                    + Main
                                                    .getMessageProvider()
                                                    .getMessage("config.dungeon.invalid_mob")
                                                    + '\''
                                                    + requirement
                                                    + '\''
                                                    + " in "
                                                    + event.getDungeon().id());
                                            return;
                                        }
                                    }
                                    baseSlayRequirements.put(words[2], mobNumber);
                                    return;
                                case "item":
                                    final int itemNumber;
                                    try {
                                        itemNumber = Integer.parseInt(words[2]);
                                    } catch (final NumberFormatException _) {
                                        Main.getDungeonLogger().warning(Main
                                                .getMessageProvider()
                                                .getConsolePrefix()
                                                + Main
                                                .getMessageProvider()
                                                .getMessage("config.dungeon.number_format_error")
                                                + '\''
                                                + requirement
                                                + '\''
                                                + " in "
                                                + event.getDungeon().id());
                                        return;
                                    }
                                    baseItemRequirements.put(words[1], itemNumber);
                                    return;
                            }
                            break;
                        case 4:
                            if (words[0].equals("interact")) {
                                final double x, y, z;
                                try {
                                    x = Double.parseDouble(words[1]);
                                    y = Double.parseDouble(words[2]);
                                    z = Double.parseDouble(words[3]);
                                } catch (final NumberFormatException _) {
                                    Main.getDungeonLogger().warning(Main
                                            .getMessageProvider()
                                            .getConsolePrefix()
                                            + Main
                                            .getMessageProvider()
                                            .getMessage("config.dungeon.number_format_error")
                                            + '\''
                                            + requirement
                                            + '\''
                                            + " in "
                                            + event.getDungeon().id());
                                    return;
                                }
                                baseInteractions.put(new Location(event.getDungeon().getWorld(), x, y, z), "");
                                return;
                            }
                            break;
                        case 5:
                            switch (words[0]) {
                                case "interact":
                                    final double x, y, z;
                                    try {
                                        x = Double.parseDouble(words[1]);
                                        y = Double.parseDouble(words[2]);
                                        z = Double.parseDouble(words[3]);
                                    } catch (final NumberFormatException _) {
                                        Main.getDungeonLogger().warning(Main
                                                .getMessageProvider()
                                                .getConsolePrefix()
                                                + Main
                                                .getMessageProvider()
                                                .getMessage("config.dungeon.number_format_error")
                                                + '\''
                                                + requirement
                                                + '\''
                                                + " in "
                                                + event.getDungeon().id());
                                        return;
                                    }
                                    try {
                                        EntityType.valueOf(words[4]);
                                    } catch (final IllegalArgumentException _) {
                                        if (MythicBukkit
                                                .inst()
                                                .getMobManager()
                                                .getMythicMob(words[4])
                                                .isEmpty()) {
                                            Main.getDungeonLogger().warning(Main
                                                    .getMessageProvider()
                                                    .getConsolePrefix()
                                                    + Main
                                                    .getMessageProvider()
                                                    .getMessage("config.dungeon.invalid_mob")
                                                    + '\''
                                                    + requirement
                                                    + '\''
                                                    + " in "
                                                    + event.getDungeon().id());
                                            return;
                                        }
                                    }
                                    baseInteractions.put(new Location(event.getDungeon().getWorld(), x, y, z), words[4]);
                                    return;
                                case "near":
                                    final double x0, y0, z0;
                                    final double radius;
                                    try {
                                        x0 = Double.parseDouble(words[1]);
                                        y0 = Double.parseDouble(words[2]);
                                        z0 = Double.parseDouble(words[3]);
                                        radius = Double.parseDouble(words[4]);
                                    } catch (final NumberFormatException _) {
                                        Main.getDungeonLogger().warning(Main
                                                .getMessageProvider()
                                                .getConsolePrefix()
                                                + Main
                                                .getMessageProvider()
                                                .getMessage("config.dungeon.number_format_error")
                                                + '\''
                                                + requirement
                                                + '\''
                                                + " in "
                                                + event.getDungeon().id());
                                        return;
                                    }
                                    baseLTV.add(new DistanceRequirement(new Location(event.getDungeon().getWorld(), x0, y0, z0), radius));
                                    return;
                            }
                            break;
                        case 7:
                            if (words[0].equals("enter")) {
                                final double x0, y0, z0, x1, y1, z1;
                                try {
                                    x0 = Double.parseDouble(words[1]);
                                    y0 = Double.parseDouble(words[2]);
                                    z0 = Double.parseDouble(words[3]);
                                    x1 = Double.parseDouble(words[4]);
                                    y1 = Double.parseDouble(words[5]);
                                    z1 = Double.parseDouble(words[6]);
                                } catch (final NumberFormatException _) {
                                    Main.getDungeonLogger().warning(Main
                                            .getMessageProvider()
                                            .getConsolePrefix()
                                            + Main
                                            .getMessageProvider()
                                            .getMessage("config.dungeon.number_format_error")
                                            + '\''
                                            + requirement
                                            + '\''
                                            + " in "
                                            + event.getDungeon().id());
                                    return;
                                }
                                baseRTV.add(new LocationPair(new Location(event.getDungeon().getWorld(), x0, y0, z0), new Location(event.getDungeon().getWorld(), x1, y1, z1)));
                                return;
                            }
                            break;
                    }
                    Main.getDungeonLogger().warning(Main
                            .getMessageProvider()
                            .getConsolePrefix()
                            + Main
                            .getMessageProvider()
                            .getMessage("config.dungeon.requirement_parsing_error")
                            + '\''
                            + requirement
                            + '\''
                            + " in "
                            + event.getDungeon().id());
                });
        return config.getString("events."
                + event.getID()
                + ".requirement_mode",
                "ALL")
                .equalsIgnoreCase("ALL")
                ? (type, e, args) -> Main.getWorkStealingExecutor().execute(() -> {
            final ParticipantRequirements req;
            if ((req = switch (type) {
                case SLAY -> {
                    final ParticipantRequirements requirements;
                    {
                        Object ent;
                        if (e instanceof final EntityDeathEvent ev
                                && !ev.isCancelled()
                                && ((ent = ev
                                .getDamageSource()
                                .getCausingEntity())
                                instanceof Player
                                || (ev
                                .getDamageSource()
                                .getCausingEntity()
                                instanceof final Projectile proj
                                && (ent = proj
                                .getShooter())
                                instanceof Player))) {
                            final Player player;
                            final AbstractParty party;
                            if (!event.isActiveFor((party = Main
                                    .getMMOCoreAPI()
                                    .getPlayerData(player = (Player) ent)
                                    .getParty())
                                    != null
                                    ? party
                                    .getOnlineMembers()
                                    .stream()
                                    .map(SynchronizedDataHolder::getPlayer)
                                    .toArray(Player[]::new)
                                    : new Player[]{player}))
                                yield null;
                            requirements = participantRequirements.computeIfAbsent(
                                    party != null ? party : player,
                                    ParticipantRequirements::new);
                        } else yield null;
                    }
                    final Entity ent;
                    final ActiveMob mythicMob;
                    final AtomicBoolean present = new AtomicBoolean();
                    if (requirements.slayRequirements.computeIfPresent(
                            (mythicMob = MythicBukkit
                                    .inst()
                                    .getMobManager()
                                    .getMythicMobInstance(ent
                                            = ((EntityDeathEvent) e)
                                            .getEntity()))
                                    != null
                                    ? mythicMob.getMobType()
                                    : ent.getType().name(),
                            (_, v) -> {
                                present.setPlain(true);
                                if (++v.current != v.objective)
                                    return v;
                                return null;
                            }) != null
                            || !present.getPlain())
                        yield null;
                    //Return the requirements only if they got updated!
                    yield requirements;
                }
                case MOVE -> {
                    final ParticipantRequirements requirements;
                    final Location loc;
                    if (e instanceof final PlayerMoveEvent ev && !ev.isCancelled()) {
                        final Player player;
                        final AbstractParty party;
                        if (!event.isActiveFor((party = Main
                                .getMMOCoreAPI()
                                .getPlayerData(player = ev.getPlayer())
                                .getParty())
                                != null
                                ? party
                                .getOnlineMembers()
                                .stream()
                                .map(SynchronizedDataHolder::getPlayer)
                                .toArray(Player[]::new)
                                : new Player[]{player}))
                            yield null;
                        requirements = participantRequirements.computeIfAbsent(
                                party != null ? party : player,
                                ParticipantRequirements::new);
                        loc = ev.getTo();
                    } else yield null;
                    if (requirements
                            .nearLTV
                            .stream()
                            .anyMatch(dr -> loc
                                    .distance(dr.loc)
                                    <= dr.distance
                                    && requirements
                                    .nearLTV
                                    .remove(dr))
                            | requirements
                            .rtv
                            .stream()
                            .anyMatch(lp -> lp
                                    .firstPos.getX()
                                    <= loc.getX()
                                    && lp.firstPos.getY()
                                    <= loc.getY()
                                    && lp.firstPos.getZ()
                                    <= loc.getZ()
                                    && lp.lastPos.getX()
                                    >= loc.getX()
                                    && lp.lastPos.getY()
                                    >= loc.getY()
                                    && lp.lastPos.getZ()
                                    >= loc.getZ()
                                    && requirements
                                    .rtv
                                    .remove(lp)))
                        yield requirements;
                    yield null;
                }
                case INTERACT -> {
                    final ParticipantRequirements requirements;
                    final Location loc;
                    final String target;
                    if (e instanceof final PlayerInteractEvent ev
                            && ev.useInteractedBlock() != Event.Result.DENY) {
                        final Player player;
                        final AbstractParty party;
                        if (!event.isActiveFor((party = Main
                                .getMMOCoreAPI()
                                .getPlayerData(player = ev.getPlayer())
                                .getParty())
                                != null
                                ? party
                                .getOnlineMembers()
                                .stream()
                                .map(SynchronizedDataHolder::getPlayer)
                                .toArray(Player[]::new)
                                : new Player[]{player}))
                            yield null;
                        requirements = participantRequirements.computeIfAbsent(
                                party != null ? party : player,
                                ParticipantRequirements::new);
                        assert ev.getClickedBlock() != null;
                        loc = ev.getClickedBlock()
                                .getLocation();
                        target = "";
                    } else if (e instanceof final PlayerInteractAtEntityEvent ev
                            && !ev.isCancelled()) {
                        final Player player;
                        final AbstractParty party;
                        if (!event.isActiveFor((party = Main
                                .getMMOCoreAPI()
                                .getPlayerData(player = ev.getPlayer())
                                .getParty())
                                != null
                                ? party
                                .getOnlineMembers()
                                .stream()
                                .map(SynchronizedDataHolder::getPlayer)
                                .toArray(Player[]::new)
                                : new Player[]{player}))
                            yield null;
                        requirements = participantRequirements.computeIfAbsent(
                                party != null ? party : player,
                                ParticipantRequirements::new);
                        loc = ev.getRightClicked()
                                .getLocation()
                                .toBlockLocation();
                        final ActiveMob mythicMob;
                        final Entity ent;
                        target = (mythicMob = MythicBukkit
                                .inst()
                                .getMobManager()
                                .getMythicMobInstance(ent = ev
                                        .getRightClicked()))
                                != null
                                ? mythicMob.getMobType()
                                : ent.getType().name();
                    } else yield null;
                    final AtomicBoolean present = new AtomicBoolean();
                    if (requirements.interactions.computeIfPresent(
                            loc,
                            (_, v) -> {
                                present.setPlain(true);
                                if (target.equalsIgnoreCase(v))
                                    return null;
                                return v;
                            }) != null
                            || !present.getPlain())
                        yield null;
                    yield requirements;
                }
                case ITEM -> {
                    final ParticipantRequirements requirements;
                    final String displayName;
                    if (e instanceof final EntityPickupItemEvent ev
                            && !ev.isCancelled()
                            && ev.getEntity()
                            instanceof final Player player) {
                        final AbstractParty party;
                        if (!event.isActiveFor((party = Main
                                .getMMOCoreAPI()
                                .getPlayerData(player)
                                .getParty())
                                != null
                                ? party
                                .getOnlineMembers()
                                .stream()
                                .map(SynchronizedDataHolder::getPlayer)
                                .toArray(Player[]::new)
                                : new Player[]{player}))
                            yield null;
                        requirements = participantRequirements.computeIfAbsent(
                                party != null ? party : player,
                                ParticipantRequirements::new);
                        displayName = (String) args[0];
                    } else yield null;
                    final AtomicBoolean present = new AtomicBoolean();
                    if (requirements.itemRequirements.computeIfPresent(
                            displayName,
                            (_, v) -> {
                                present.setPlain(true);
                                if (++v.current != v.objective)
                                    return v;
                                return null;
                            }) != null
                            || !present.getPlain())
                        yield null;
                    yield requirements;
                }
            }) != null
                    && req.slayRequirements.isEmpty()
                    && req.nearLTV.isEmpty()
                    && req.rtv.isEmpty()
                    && req.interactions.isEmpty()
                    && req.itemRequirements.isEmpty()
                    && req.completed.compareAndSet(false, true)) {
                participantRequirements.remove(req.key);
                final Player[] players;
                event.checkpointToUnlock().unlockFor(players
                        = req
                        .key
                        instanceof final AbstractParty party
                        ? party
                        .getOnlineMembers()
                        .stream()
                        .map(SynchronizedDataHolder::getPlayer)
                        .toArray(Player[]::new)
                        : new Player[]{(Player) req.key});
                event.executeCommandsFor(players);
                event.bossRoomToUnlock().join(players);
            }
        }) : (type, e, args) -> Main.getWorkStealingExecutor().execute(() -> {
            final ParticipantRequirements req;
            if ((req = switch (type) {
                case SLAY -> {
                    final ParticipantRequirements requirements;
                    {
                        Object ent;
                        if (e instanceof final EntityDeathEvent ev
                                && !ev.isCancelled()
                                && ((ent = ev
                                .getDamageSource()
                                .getCausingEntity())
                                instanceof Player
                                || (ev
                                .getDamageSource()
                                .getCausingEntity()
                                instanceof final Projectile proj
                                && (ent = proj
                                .getShooter())
                                instanceof Player))) {
                            final Player player;
                            final AbstractParty party;
                            if (!event.isActiveFor((party = Main
                                    .getMMOCoreAPI()
                                    .getPlayerData(player = (Player) ent)
                                    .getParty())
                                    != null
                                    ? party
                                    .getOnlineMembers()
                                    .stream()
                                    .map(SynchronizedDataHolder::getPlayer)
                                    .toArray(Player[]::new)
                                    : new Player[]{player}))
                                yield null;
                            requirements = participantRequirements.computeIfAbsent(
                                    party != null ? party : player,
                                    ParticipantRequirements::new);
                        } else yield null;
                    }
                    final Entity ent;
                    final ActiveMob mythicMob;
                    final AtomicBoolean present = new AtomicBoolean();
                    if (requirements.slayRequirements.computeIfPresent(
                            (mythicMob = MythicBukkit
                                    .inst()
                                    .getMobManager()
                                    .getMythicMobInstance(ent
                                            = ((EntityDeathEvent) e)
                                            .getEntity()))
                                    != null
                                    ? mythicMob.getMobType()
                                    : ent.getType().name(),
                            (_, v) -> {
                                present.setPlain(true);
                                if (++v.current != v.objective)
                                    return v;
                                return null;
                            }) != null
                            || !present.getPlain()
                            || !requirements.slayRequirements.isEmpty())
                        yield null;
                    yield requirements;
                }
                case MOVE -> {
                    final ParticipantRequirements requirements;
                    final Location loc;
                    if (e instanceof final PlayerMoveEvent ev && !ev.isCancelled()) {
                        final Player player;
                        final AbstractParty party;
                        if (!event.isActiveFor((party = Main
                                .getMMOCoreAPI()
                                .getPlayerData(player = ev.getPlayer())
                                .getParty())
                                != null
                                ? party
                                .getOnlineMembers()
                                .stream()
                                .map(SynchronizedDataHolder::getPlayer)
                                .toArray(Player[]::new)
                                : new Player[]{player}))
                            yield null;
                        requirements = participantRequirements.computeIfAbsent(
                                party != null ? party : player,
                                ParticipantRequirements::new);
                        loc = ev.getTo();
                    } else yield null;
                    if ((requirements
                            .nearLTV
                            .stream()
                            .anyMatch(dr -> loc
                                    .distance(dr.loc)
                                    <= dr.distance
                                    && requirements
                                    .nearLTV
                                    .remove(dr))
                            && requirements
                            .nearLTV
                            .isEmpty())
                            || (requirements
                            .rtv
                            .stream()
                            .anyMatch(lp -> lp
                                    .firstPos.getX()
                                    <= loc.getX()
                                    && lp.firstPos.getY()
                                    <= loc.getY()
                                    && lp.firstPos.getZ()
                                    <= loc.getZ()
                                    && lp.lastPos.getX()
                                    >= loc.getX()
                                    && lp.lastPos.getY()
                                    >= loc.getY()
                                    && lp.lastPos.getZ()
                                    >= loc.getZ()
                                    && requirements
                                    .rtv
                                    .remove(lp))
                            && requirements
                            .rtv
                            .isEmpty()))
                        yield requirements;
                    yield null;
                }
                case INTERACT -> {
                    final ParticipantRequirements requirements;
                    final Location loc;
                    final String target;
                    if (e instanceof final PlayerInteractEvent ev
                            && ev.useInteractedBlock() != Event.Result.DENY) {
                        final Player player;
                        final AbstractParty party;
                        if (!event.isActiveFor((party = Main
                                .getMMOCoreAPI()
                                .getPlayerData(player = ev.getPlayer())
                                .getParty())
                                != null
                                ? party
                                .getOnlineMembers()
                                .stream()
                                .map(SynchronizedDataHolder::getPlayer)
                                .toArray(Player[]::new)
                                : new Player[]{player}))
                            yield null;
                        requirements = participantRequirements.computeIfAbsent(
                                party != null ? party : player,
                                ParticipantRequirements::new);
                        assert ev.getClickedBlock() != null;
                        loc = ev.getClickedBlock()
                                .getLocation();
                        target = "";
                    } else if (e instanceof final PlayerInteractAtEntityEvent ev
                            && !ev.isCancelled()) {
                        final Player player;
                        final AbstractParty party;
                        if (!event.isActiveFor((party = Main
                                .getMMOCoreAPI()
                                .getPlayerData(player = ev.getPlayer())
                                .getParty())
                                != null
                                ? party
                                .getOnlineMembers()
                                .stream()
                                .map(SynchronizedDataHolder::getPlayer)
                                .toArray(Player[]::new)
                                : new Player[]{player}))
                            yield null;
                        requirements = participantRequirements.computeIfAbsent(
                                party != null ? party : player,
                                ParticipantRequirements::new);
                        loc = ev.getRightClicked()
                                .getLocation()
                                .toBlockLocation();
                        final ActiveMob mythicMob;
                        final Entity ent;
                        target = (mythicMob = MythicBukkit
                                .inst()
                                .getMobManager()
                                .getMythicMobInstance(ent = ev
                                        .getRightClicked()))
                                != null
                                ? mythicMob.getMobType()
                                : ent.getType().name();
                    } else yield null;
                    final AtomicBoolean present = new AtomicBoolean();
                    if (requirements.interactions.computeIfPresent(
                            loc,
                            (_, v) -> {
                                present.setPlain(true);
                                if (target.equalsIgnoreCase(v))
                                    return null;
                                return v;
                            }) != null
                            || !present.getPlain()
                            || !requirements.interactions.isEmpty())
                        yield null;
                    yield requirements;
                }
                case ITEM -> {
                    final ParticipantRequirements requirements;
                    final String displayName;
                    if (e instanceof final EntityPickupItemEvent ev
                            && !ev.isCancelled()
                            && ev.getEntity()
                            instanceof final Player player) {
                        final AbstractParty party;
                        if (!event.isActiveFor((party = Main
                                .getMMOCoreAPI()
                                .getPlayerData(player)
                                .getParty())
                                != null
                                ? party
                                .getOnlineMembers()
                                .stream()
                                .map(SynchronizedDataHolder::getPlayer)
                                .toArray(Player[]::new)
                                : new Player[]{player}))
                            yield null;
                        requirements = participantRequirements.computeIfAbsent(
                                party != null ? party : player,
                                ParticipantRequirements::new);
                        displayName = (String) args[0];
                    } else yield null;
                    final AtomicBoolean present = new AtomicBoolean();
                    if (requirements.itemRequirements.computeIfPresent(
                            displayName,
                            (_, v) -> {
                                present.setPlain(true);
                                if (++v.current != v.objective)
                                    return v;
                                return null;
                            }) != null
                            || !present.getPlain()
                            || !requirements.itemRequirements.isEmpty())
                        yield null;
                    yield requirements;
                }
            }) != null
                    && req
                    .completed
                    .compareAndSet(false, true)) {
                participantRequirements.remove(req.key);
                final Player[] players;
                event.checkpointToUnlock().unlockFor(players
                        = req
                        .key
                        instanceof final AbstractParty party
                        ? party
                        .getOnlineMembers()
                        .stream()
                        .map(SynchronizedDataHolder::getPlayer)
                        .toArray(Player[]::new)
                        : new Player[]{(Player) req.key});
                event.executeCommandsFor(players);
                event.bossRoomToUnlock().join(players);
            }
        });
    }
}
