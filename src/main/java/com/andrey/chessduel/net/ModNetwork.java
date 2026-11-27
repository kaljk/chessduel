package com.andrey.chessduel.net;

import com.andrey.chessduel.ChessDuelMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ChessDuelMod.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, DuelStatePacket.class, DuelStatePacket::encode, DuelStatePacket::decode, DuelStatePacket::handle);
        CHANNEL.registerMessage(id++, MovePacket.class, MovePacket::encode, MovePacket::decode, MovePacket::handle);
        CHANNEL.registerMessage(id++, ActionPacket.class, ActionPacket::encode, ActionPacket::decode, ActionPacket::handle);
    }
}
