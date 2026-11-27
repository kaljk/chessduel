package com.andrey.chessduel.duel;

import com.andrey.chessduel.chess.Bot;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DuelEvents {

    /** Игрок бьёт моба -> шахматная дуэль вместо драки. Креатив + Shift = обычный удар (чтобы можно было убирать мобов). */
    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent e) {
        if (e.getEntity().level().isClientSide) return;
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getTarget() instanceof Mob mob)) return;
        if (p.isCreative() && p.isShiftKeyDown()) return;
        e.setCanceled(true);
        DuelManager.start(p, mob, DuelManager.levelFor(mob));
    }

    /** Моб (или его стрела/взрыв) пытается ударить игрока -> дуэль, урона нет. Во время дуэли урона нет ни у кого. */
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        if (DuelManager.isDueling(victim)) { e.setCanceled(true); return; }
        if (victim instanceof ServerPlayer p) {
            if (DuelManager.hasCooldown(p)) { e.setCanceled(true); return; }
            if (e.getSource().getEntity() instanceof Mob attacker && !DuelManager.isDueling(attacker)) {
                e.setCanceled(true);
                if (e.getSource().getDirectEntity() instanceof Projectile proj) proj.discard(); // стрела не должна попасть
                DuelManager.start(p, attacker, DuelManager.levelFor(attacker));
            }
        }
    }

    /** Мобы не берут в цель игрока, который играет (или только что доиграл). */
    @SubscribeEvent
    public void onChangeTarget(LivingChangeTargetEvent e) {
        if (e.getNewTarget() instanceof ServerPlayer p && (DuelManager.isDueling(p) || DuelManager.hasCooldown(p)))
            e.setCanceled(true);
    }

    /** Зритель кликает ПКМ по участнику: по игроку -> видит со стороны игрока, по мобу -> со стороны моба. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide || e.getHand() != InteractionHand.MAIN_HAND) return;
        Entity target = e.getTarget();
        DuelManager.Duel d = DuelManager.getDuelOf(target);
        if (d == null) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getEntity() instanceof ServerPlayer viewer && !viewer.getUUID().equals(d.playerId))
            DuelManager.addSpectator(viewer, d, target instanceof Mob);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        DuelManager.tick(e.getServer());
        // крипер вплотную к игроку: начинаем дуэль до взрыва
        if (e.getServer().getTickCount() % 5 == 0) {
            for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
                if (DuelManager.isDueling(p) || DuelManager.hasCooldown(p)) continue;
                for (Creeper c : p.level().getEntitiesOfClass(Creeper.class, p.getBoundingBox().inflate(4.5),
                        cr -> cr.getTarget() == p && !DuelManager.isDueling(cr))) {
                    DuelManager.start(p, c, DuelManager.levelFor(c));
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DuelManager.onLogout(p);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent e) {
        DuelManager.endAll(e.getServer());
    }

    /** /chessduel practice [1-4] — партия без моба, чтобы проверить бота и интерфейс. */
    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("chessduel")
                .then(Commands.literal("practice")
                        .executes(ctx -> practice(ctx.getSource().getPlayerOrException(), 1))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> practice(ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "level"))))));
    }

    private int practice(ServerPlayer p, int level) {
        DuelManager.start(p, null, level);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Тренировка: уровень " + level + " (≈" + Bot.ELO[level] + " Эло)"));
        return 1;
    }
}
