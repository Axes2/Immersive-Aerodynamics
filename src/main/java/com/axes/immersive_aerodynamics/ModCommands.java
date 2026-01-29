package com.axes.immersive_aerodynamics;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;


public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("iaero")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")

                        // INFLUENCE: 0 to 100 (Instead of 0.0 to 10.0)
                        .then(Commands.literal("influence")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 100))
                                        .executes(context -> {
                                            double val = DoubleArgumentType.getDouble(context, "value");
                                            Config.WIND_INFLUENCE.set(val);
                                            Config.WIND_INFLUENCE.save();
                                            context.getSource().sendSuccess(() -> Component.literal("Wind Influence set to: " + val), true);
                                            return 1;
                                        })
                                )
                        )

                        // TURBULENCE: 0 to 50
                        .then(Commands.literal("turbulence")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 50))
                                        .executes(context -> {
                                            double val = DoubleArgumentType.getDouble(context, "value");
                                            Config.TURBULENCE_MULTIPLIER.set(val);
                                            Config.TURBULENCE_MULTIPLIER.save();
                                            context.getSource().sendSuccess(() -> Component.literal("Turbulence set to: " + val), true);
                                            return 1;
                                        })
                                )
                        )

                        // THRESHOLD, SCALING, DEBUG (Keep roughly the same)
                        .then(Commands.literal("threshold")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 500))
                                        .executes(context -> {
                                            double val = DoubleArgumentType.getDouble(context, "value");
                                            Config.WIND_THRESHOLD.set(val);
                                            Config.WIND_THRESHOLD.save();
                                            context.getSource().sendSuccess(() -> Component.literal("Threshold set to: " + val), true);
                                            return 1;
                                        })
                                )
                        )
                        // ... include scaling/debug commands from before ...
                        .then(Commands.literal("scaling")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 1.0))
                                        .executes(context -> {
                                            double val = DoubleArgumentType.getDouble(context, "value");
                                            Config.MASS_SCALING.set(val);
                                            Config.MASS_SCALING.save();
                                            context.getSource().sendSuccess(() -> Component.literal("Mass Scaling set to: " + val), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("debug")
                                .executes(context -> {
                                    boolean current = Config.DEBUG_MODE.get();
                                    Config.DEBUG_MODE.set(!current);
                                    Config.DEBUG_MODE.save();
                                    context.getSource().sendSuccess(() -> Component.literal("Debug Mode: " + (!current)), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("pocket_chance")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 1.0))
                                        .executes(context -> {
                                            Config.AIR_POCKET_CHANCE.set(DoubleArgumentType.getDouble(context, "value"));
                                            Config.AIR_POCKET_CHANCE.save();
                                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Pocket Chance set to: " + DoubleArgumentType.getDouble(context, "value")), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("pocket_strength")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 10.0))
                                        .executes(context -> {
                                            Config.AIR_POCKET_STRENGTH.set(DoubleArgumentType.getDouble(context, "value"));
                                            Config.AIR_POCKET_STRENGTH.save();
                                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Pocket Strength set to: " + DoubleArgumentType.getDouble(context, "value")), true);
                                            return 1;
                                        })
                                )
                        ).then(Commands.literal("pitch_bias")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 10.0))
                                        .executes(context -> {
                                            Config.PITCH_DOWN_BIAS.set(DoubleArgumentType.getDouble(context, "value"));
                                            Config.PITCH_DOWN_BIAS.save();
                                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Pitch Down Bias set to: " + DoubleArgumentType.getDouble(context, "value")), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("vibration")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 10.0))
                                        .executes(context -> {
                                            Config.VIBRATION_STRENGTH.set(DoubleArgumentType.getDouble(context, "value"));
                                            Config.VIBRATION_STRENGTH.save();
                                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Vibration Strength set to: " + DoubleArgumentType.getDouble(context, "value")), true);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }
}