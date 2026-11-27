package com.andrey.chessduel.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Настройки дизайна (хранятся в config/chessduel-client.toml). */
public final class ClientSettings {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue THEME;
    public static final ForgeConfigSpec.IntValue PIECES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        THEME = b.comment("Тема доски").defineInRange("boardTheme", 0, 0, 4);
        PIECES = b.comment("0 = символы, 1 = буквы").defineInRange("pieceStyle", 0, 0, 1);
        SPEC = b.build();
    }

    private ClientSettings() {}
}
