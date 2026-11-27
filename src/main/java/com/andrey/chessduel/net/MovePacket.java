package com.andrey.chessduel.net;

import com.andrey.chessduel.duel.DuelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: ход игрока. Сервер сам проверяет легальность. */
public class MovePacket {
    public final int from, to;

    public MovePacket(int from, int to) { this.from = from; this.to = to; }

    public static void encode(MovePacket p, FriendlyByteBuf b) { b.writeByte(p.from); b.writeByte(p.to); }
    public static MovePacket decode(FriendlyByteBuf b) { return new MovePacket(b.readByte(), b.readByte()); }

    public static void handle(MovePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && p.from >= 0 && p.from < 64 && p.to >= 0 && p.to < 64)
                DuelManager.handleMove(sender, p.from, p.to);
        });
        ctx.get().setPacketHandled(true);
    }
}
