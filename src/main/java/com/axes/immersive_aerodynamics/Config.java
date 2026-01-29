package com.axes.immersive_aerodynamics;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- PHYSICS SETTINGS ---
    public static final ModConfigSpec.DoubleValue WIND_INFLUENCE = BUILDER
            .comment("Wind Force Strength. (0-100). Default 9.0.")
            .defineInRange("windInfluence", 9.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue MASS_SCALING = BUILDER
            .comment("Mass Physics. 1.0 = Linear, 0.5 = Curved.")
            .defineInRange("massScaling", 0.6, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue WIND_THRESHOLD = BUILDER
            .comment("Minimum wind speed to feel effects.")
            .defineInRange("windThreshold", 50.0, 0.0, 200.0);

    // --- TURBULENCE V2 SETTINGS ---

    public static final ModConfigSpec.DoubleValue TURBULENCE_MULTIPLIER = BUILDER
            .comment("Global Turbulence Strength (Pitch/Yaw shaking). (0-50). Default 1.0.")
            .defineInRange("turbulenceMultiplier", 1.0, 0.0, 50.0);

    // Feature #2: Air Pockets
    public static final ModConfigSpec.DoubleValue AIR_POCKET_CHANCE = BUILDER
            .comment("Chance per tick to hit a downdraft in a storm. 0.01 = Once per 5 sec.")
            .defineInRange("airPocketChance", 0.01, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue AIR_POCKET_STRENGTH = BUILDER
            .comment("How hard an air pocket pushes you down. (0-10). Default 2.0.")
            .defineInRange("airPocketStrength", 2.0, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue PITCH_DOWN_BIAS = BUILDER
            .comment("How much stronger downward turbulence is compared to upward. 1.5 = Down is 50% stronger.")
            .defineInRange("pitchDownBias", 2.0, 1.0, 10.0);

    public static final ModConfigSpec.DoubleValue VIBRATION_STRENGTH = BUILDER
            .comment("Visual shaking intensity in storms (High frequency). (0.0-5.0)")
            .defineInRange("vibrationStrength", 0.5, 0.0, 10.0);

    // --- MISC ---
    public static final ModConfigSpec.BooleanValue ENABLE_TORNADO_SUCTION = BUILDER
            .define("enableTornadoSuction", true);

    public static final ModConfigSpec.BooleanValue DEBUG_MODE = BUILDER
            .define("debugMode", false);


    public static final ModConfigSpec SPEC = BUILDER.build();
}