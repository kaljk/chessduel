package com.andrey.chessduel.client;

import com.andrey.chessduel.net.DuelStatePacket;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handleState(DuelStatePacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChessScreen cs) cs.update(p);
        else mc.setScreen(new ChessScreen(p));
    }
}
