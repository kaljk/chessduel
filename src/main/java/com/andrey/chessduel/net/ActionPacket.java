package com.andrey.chessduel.net;

import com.andrey.chessduel.duel.DuelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: 0 = сдаться, 1 = зритель закрыл окно. */
public class ActionPacket {
    public static final int RESIGN = 0, LEAVE_SPECTATE = 1;
    public final int action;

    public ActionPacket(int action) { this.action = action; }

    public static void encode(ActionPacket p, FriendlyByteBuf b) { b.writeByte(p.action); }
    public static ActionPacket decode(FriendlyByteBuf b) { return new ActionPacket(b.readByte()); }

    public static void handle(ActionPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            if (p.action == RESIGN) DuelManager.resign(sender);
            else if (p.action == LEAVE_SPECTATE) DuelManager.removeSpectator(sender);
        });
        ctx.get().setPacketHandled(true);
    }
}
