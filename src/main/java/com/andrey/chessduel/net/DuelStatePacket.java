package com.andrey.chessduel.net;

import com.andrey.chessduel.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сервер -> клиент: полное состояние партии. status: 0 идёт, 1 победил игрок, 2 победил соперник, 3 ничья. */
public class DuelStatePacket {
    public final int mobEntityId, level, lastFrom, lastTo, status;
    public final String mobName, fen;
    public final boolean spectator, flip;

    public DuelStatePacket(int mobEntityId, int level, String mobName, String fen,
                           int lastFrom, int lastTo, int status, boolean spectator, boolean flip) {
        this.mobEntityId = mobEntityId; this.level = level; this.mobName = mobName; this.fen = fen;
        this.lastFrom = lastFrom; this.lastTo = lastTo; this.status = status;
        this.spectator = spectator; this.flip = flip;
    }

    public static void encode(DuelStatePacket p, FriendlyByteBuf b) {
        b.writeInt(p.mobEntityId); b.writeInt(p.level); b.writeUtf(p.mobName); b.writeUtf(p.fen);
        b.writeInt(p.lastFrom); b.writeInt(p.lastTo); b.writeInt(p.status);
        b.writeBoolean(p.spectator); b.writeBoolean(p.flip);
    }

    public static DuelStatePacket decode(FriendlyByteBuf b) {
        return new DuelStatePacket(b.readInt(), b.readInt(), b.readUtf(), b.readUtf(),
                b.readInt(), b.readInt(), b.readInt(), b.readBoolean(), b.readBoolean());
    }

    public static void handle(DuelStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleState(p)));
        ctx.get().setPacketHandled(true);
    }
}
