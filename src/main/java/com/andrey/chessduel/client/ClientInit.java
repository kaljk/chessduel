package com.andrey.chessduel.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientInit {
    public static final KeyMapping SETTINGS_KEY = new KeyMapping("key.chessduel.settings",
            InputConstants.KEY_J, "key.categories.chessduel");

    private ClientInit() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientInit::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(ClientInit::onClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent e) { e.register(SETTINGS_KEY); }

    private static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (SETTINGS_KEY.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new SettingsScreen(null));
        }
    }
}
