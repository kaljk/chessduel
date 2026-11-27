package com.andrey.chessduel.duel;

import com.andrey.chessduel.chess.Board;
import com.andrey.chessduel.chess.Bot;
import com.andrey.chessduel.chess.Move;
import com.andrey.chessduel.net.DuelStatePacket;
import com.andrey.chessduel.net.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/** Вся серверная логика дуэлей. Партия живёт на сервере: клиент только показывает доску и шлёт клики. */
public final class DuelManager {
    private static final int BOT_DELAY_TICKS = 14;
    private static final int COOLDOWN_TICKS = 100;
    private static final int FREEZE_RADIUS = 16;
    private static final Random RND = new Random();

    public static final class Duel {
        public final UUID playerId;
        public final UUID mobId;            // null в тренировке
        final int mobEntityId, level;
        final String mobName;
        final Board board = Board.start();
        final Bot bot;
        final Map<UUID, Boolean> spectators = new HashMap<>(); // uuid -> смотреть со стороны моба
        final List<UUID> frozen = new ArrayList<>();
        final ServerLevel world;
        boolean prevNoAi, prevInvulnerable;
        int botTimer = -1, lastFrom = -1, lastTo = -1;

        Duel(ServerPlayer p, Mob mob, int level) {
            this.playerId = p.getUUID();
            this.mobId = mob == null ? null : mob.getUUID();
            this.mobEntityId = mob == null ? -1 : mob.getId();
            this.mobName = mob == null ? "Тренировка" : mob.getName().getString();
            this.level = level;
            this.bot = Bot.forLevel(level, RND);
            this.world = (ServerLevel) p.level();
        }
    }

    private static final Map<UUID, Duel> BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Duel> BY_MOB = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN_UNTIL = new HashMap<>();

    private DuelManager() {}

    // ---------- запросы ----------
    public static boolean isDueling(Entity e) {
        return BY_PLAYER.containsKey(e.getUUID()) || BY_MOB.containsKey(e.getUUID());
    }

    public static Duel getDuelOf(Entity e) {
        Duel d = BY_PLAYER.get(e.getUUID());
        return d != null ? d : BY_MOB.get(e.getUUID());
    }

    public static boolean hasCooldown(ServerPlayer p) {
        Integer t = COOLDOWN_UNTIL.get(p.getUUID());
        return t != null && p.server.getTickCount() < t;
    }

    /** Уровень сложности по здоровью моба: 1 лёгкий (300), 2 средний (500), 3 продвинутый (800), 4 босс (1000). */
    public static int levelFor(Mob mob) {
        float hp = mob.getMaxHealth();
        if (hp >= 150) return 4;
        if (hp >= 35) return 3;
        if (hp > 10) return 2;
        return 1;
    }

    // ---------- старт ----------
    public static boolean start(ServerPlayer p, Mob mob, int level) {
        if (BY_PLAYER.containsKey(p.getUUID()) || hasCooldown(p)) return false;
        if (mob != null && BY_MOB.containsKey(mob.getUUID())) return false;
        Duel d = new Duel(p, mob, level);
        BY_PLAYER.put(p.getUUID(), d);

        d.prevInvulnerable = p.isInvulnerable();
        p.setInvulnerable(true);

        if (mob != null) {
            BY_MOB.put(mob.getUUID(), d);
            d.prevNoAi = mob.isNoAi();
            freeze(mob);
        }
        // "мир застывает": замораживаем всех мобов рядом и снимаем с игрока прицел
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(FREEZE_RADIUS))) {
            if (m == mob) continue;
            if (!m.isNoAi()) { d.frozen.add(m.getUUID()); freeze(m); }
        }
        sendState(d);
        return true;
    }

    private static void freeze(Mob m) {
        m.setTarget(null);
        m.getNavigation().stop();
        m.setNoAi(true);
        m.setDeltaMovement(Vec3.ZERO);
        if (m instanceof Creeper c) c.setSwellDir(-1); // не взорваться, пока идёт партия
    }

    // ---------- ходы ----------
    public static void handleMove(ServerPlayer p, int from, int to) {
        Duel d = BY_PLAYER.get(p.getUUID());
        if (d == null || !d.board.whiteToMove || d.botTimer >= 0) return;
        Move m = d.board.find(from, to, Board.QUEEN);
        if (m == null) { sendState(d); return; }
        d.board.make(m);
        d.lastFrom = m.from; d.lastTo = m.to;
        if (!checkEnd(d)) { d.botTimer = BOT_DELAY_TICKS; sendState(d); }
    }

    public static void resign(ServerPlayer p) {
        Duel d = BY_PLAYER.get(p.getUUID());
        if (d != null) finish(d, 2);
    }

    /** Вызывается каждый серверный тик. */
    public static void tick(MinecraftServer server) {
        if (BY_PLAYER.isEmpty()) return;
        for (Duel d : new ArrayList<>(BY_PLAYER.values())) {
            ServerPlayer p = server.getPlayerList().getPlayer(d.playerId);
            if (p == null || p.level() != d.world) { finish(d, 3); continue; }
            Mob mob = d.mobId == null ? null : (d.world.getEntity(d.mobId) instanceof Mob m ? m : null);
            if (d.mobId != null && (mob == null || !mob.isAlive())) { finish(d, 3); continue; }

            if (d.botTimer > 0) d.botTimer--;
            else if (d.botTimer == 0) {
                d.botTimer = -1;
                Move bm = d.bot.choose(d.board);
                if (bm != null) {
                    d.board.make(bm);
                    d.lastFrom = bm.from; d.lastTo = bm.to;
                }
                if (!checkEnd(d)) sendState(d);
            }
        }
    }

    // ---------- конец партии ----------
    private static boolean checkEnd(Duel d) {
        return switch (d.board.result()) {
            case WHITE_WINS -> { finish(d, 1); yield true; }
            case BLACK_WINS -> { finish(d, 2); yield true; }
            case DRAW -> { finish(d, 3); yield true; }
            default -> false;
        };
    }

    /** status: 1 победа игрока, 2 поражение, 3 ничья. */
    private static void finish(Duel d, int status) {
        if (BY_PLAYER.remove(d.playerId) == null) return;
        sendState(d, status);

        MinecraftServer server = d.world.getServer();
        ServerPlayer p = server.getPlayerList().getPlayer(d.playerId);
        Mob mob = d.mobId == null ? null : (d.world.getEntity(d.mobId) instanceof Mob m ? m : null);
        if (d.mobId != null) BY_MOB.remove(d.mobId);

        for (UUID id : d.frozen) if (d.world.getEntity(id) instanceof Mob m) m.setNoAi(false);
        if (mob != null) mob.setNoAi(d.prevNoAi);

        if (p != null) {
            p.setInvulnerable(d.prevInvulnerable);
            COOLDOWN_UNTIL.put(d.playerId, server.getTickCount() + COOLDOWN_TICKS); // 5 сек. покоя после боя
            if (mob != null) mob.setTarget(null);
        }
        // победа: моб погибает как от удара игрока (лут, опыт)
        if (status == 1 && mob != null && p != null && mob.isAlive()) {
            mob.hurt(p.damageSources().playerAttack(p), Float.MAX_VALUE);
            if (mob.isAlive()) mob.kill(); // драконы и подобные не реагируют на hurt()
        }
    }

    public static void endAll(MinecraftServer server) {
        for (Duel d : new ArrayList<>(BY_PLAYER.values())) finish(d, 3);
        COOLDOWN_UNTIL.clear();
    }

    public static void onLogout(ServerPlayer p) {
        removeSpectator(p);
        Duel d = BY_PLAYER.get(p.getUUID());
        if (d != null) finish(d, 3);
    }

    // ---------- зрители ----------
    public static void addSpectator(ServerPlayer viewer, Duel d, boolean fromMobSide) {
        d.spectators.put(viewer.getUUID(), fromMobSide);
        send(viewer, d, d.board.toFen(), 0, true, fromMobSide);
    }

    public static void removeSpectator(ServerPlayer viewer) {
        for (Duel d : BY_PLAYER.values()) d.spectators.remove(viewer.getUUID());
    }

    // ---------- отправка ----------
    private static void sendState(Duel d) { sendState(d, 0); }

    private static void sendState(Duel d, int status) {
        MinecraftServer server = d.world.getServer();
        ServerPlayer p = server.getPlayerList().getPlayer(d.playerId);
        if (p != null) send(p, d, d.board.toFen(), status, false, false);
        for (Map.Entry<UUID, Boolean> e : d.spectators.entrySet()) {
            ServerPlayer s = server.getPlayerList().getPlayer(e.getKey());
            if (s != null) send(s, d, d.board.toFen(), status, true, e.getValue());
        }
    }

    private static void send(ServerPlayer to, Duel d, String fen, int status, boolean spectator, boolean flip) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> to),
                new DuelStatePacket(d.mobEntityId, d.level, d.mobName, fen, d.lastFrom, d.lastTo, status, spectator, flip));
    }
}
