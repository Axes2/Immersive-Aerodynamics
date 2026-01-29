package com.axes.immersive_aerodynamics;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(ImmersiveAerodynamics.MODID)
public class ImmersiveAerodynamics {
    public static final String MODID = "immersive_aerodynamics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ImmersiveAerodynamics(IEventBus modEventBus, ModContainer modContainer) {
        // 1. Register Config
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 2. Register Commands
        // We register this on the NeoForge bus (GAME events), not the Mod bus (LIFECYCLE events)
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}