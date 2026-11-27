package com.andrey.chessduel;

import com.andrey.chessduel.client.ClientInit;
import com.andrey.chessduel.client.ClientSettings;
import com.andrey.chessduel.duel.DuelEvents;
import com.andrey.chessduel.net.ModNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ChessDuelMod.MODID)
public class ChessDuelMod {
    public static final String MODID = "chessduel";

    public ChessDuelMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new DuelEvents());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientSettings.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientInit.init(modBus));
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(ModNetwork::register);
    }
}
