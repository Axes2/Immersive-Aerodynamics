package com.axes.immersive_aerodynamics.mixin;

import com.axes.immersive_aerodynamics.Config;
import dev.protomanly.pmweather.weather.WindEngine;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.item.upgrade.VehicleStat;
import immersive_aircraft.util.Utils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AircraftEntity.class)
public abstract class MixinAircraftEntity extends EngineVehicle {

    public MixinAircraftEntity(EntityType<? extends EngineVehicle> entityType, Level level, boolean canExplodeOnCrash) {
        super(entityType, level, canExplodeOnCrash);
    }

    @Shadow public abstract float getWindStrength();

    @Inject(method = "updateVelocity", at = @At("TAIL"))
    private void immersiveAerodynamics$applyPhysics(CallbackInfo ci) {
        if (!this.isAlive()) return;
        boolean isServer = !this.level().isClientSide;
        boolean isClientPilot = this.level().isClientSide && this.isControlledByLocalInstance();
        if (!isServer && !isClientPilot) return;

        // --- PREPARE DATA ---
        Vec3 windVector = WindEngine.getWind(this.position(), this.level(), false, !Config.ENABLE_TORNADO_SUCTION.get(), true);
        double rawWindSpeed = windVector.length();
        double threshold = Config.WIND_THRESHOLD.get();

        // 1. STANDARD WIND DRIFT (Keep existing logic)
        double influence = Config.WIND_INFLUENCE.get() / 10000.0; // Scaled 9.0 -> 0.0009

        if (rawWindSpeed > threshold) {
            double effectiveSpeed = rawWindSpeed - threshold;

            float baseMass = this.getProperties().get(VehicleStat.MASS);
            if (baseMass < 0.1) baseMass = 0.1f;
            double effectiveMass = Math.pow(baseMass, Config.MASS_SCALING.get());

            double altitude = this.getY();
            double altitudeBonus = 1.0;
            if (altitude > 100) altitudeBonus += (altitude - 100) / 100.0;

            // Tailwind Dampening
            Vec3 currentVelocity = this.getDeltaMovement();
            double currentSpeed = currentVelocity.length();
            double dampeningFactor = 1.0;

            if (currentSpeed > 0.1) {
                Vec3 normWind = windVector.normalize();
                Vec3 normVel = currentVelocity.normalize();
                double alignment = normWind.dot(normVel);

                if (alignment > 0) {
                    double maxReasonableSpeed = 1.5;
                    double speedRatio = Math.min(currentSpeed / maxReasonableSpeed, 1.0);
                    double reduction = speedRatio * alignment;
                    dampeningFactor = 1.0 - (reduction * 0.8); // Soft cap (min 20% force)
                }
            }

            // Apply Drift Force
            double forceMagnitude = (effectiveSpeed * influence * altitudeBonus * dampeningFactor) / effectiveMass;
            Vec3 push = windVector.normalize().scale(forceMagnitude);
            this.setDeltaMovement(this.getDeltaMovement().add(push));

            // 2. FEATURE #2: AIR POCKETS (Downdrafts)
            // Logic: Random chance to slam the plane down if wind is strong
            if (this.random.nextFloat() < Config.AIR_POCKET_CHANCE.get()) {

                // Strength scales with how far above threshold
                double severity = Math.min((rawWindSpeed - threshold) / 50.0, 2.0); // 0.0 to 2.0
                double pocketForce = (Config.AIR_POCKET_STRENGTH.get() / 10.0) * (1.0 + severity); // 2.0 -> 0.2 base

                // Apply downward slam
                this.setDeltaMovement(this.getDeltaMovement().add(0, -pocketForce, 0));

                if (Config.DEBUG_MODE.get() && isClientPilot && this.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player) {
                    net.minecraft.client.Minecraft.getInstance().gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("§c⚠ AIR POCKET!"));
                }
            }

            // Debug Message for in game command
            if (Config.DEBUG_MODE.get() && this.tickCount % 20 == 0) {
                if (this.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player) {
                    String color = "§a";
                    String side = isServer ? "§e[S]" : "§b[C]";
                    String dampMsg = (dampeningFactor < 0.99) ? String.format(" | Damp:%.2f", dampeningFactor) : "";
                    String msg = String.format("%s%s Wind:%.0f | Push:%.4f%s",
                            side, color, rawWindSpeed, forceMagnitude, dampMsg);

                    if (isClientPilot) {
                        net.minecraft.client.Minecraft.getInstance().gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(msg));
                    }
                }
            }
        }
    }

    /**
     * DIRECTIONAL TURBULENCE (Axis Instability)
     * Headwind = Pitch Noise (X)
     * Crosswind = Yaw Noise (Z)
     */
    @org.spongepowered.asm.mixin.Overwrite
    public Vector3f getWindEffect() {
        // --- 1. BASE WIND CALCS ---
        float wind = this.getWindStrength();
        Vec3 realWind = WindEngine.getWind(this.position(), this.level());
        double windSpeed = realWind.length();
        double threshold = Config.WIND_THRESHOLD.get() * 0.5;

        if (windSpeed > threshold) {
            double turbMult = Config.TURBULENCE_MULTIPLIER.get() / 100.0;
            wind += (float) ((windSpeed - threshold) * turbMult);
        }

        // --- 2. DIRECTIONAL BIAS ---
        Vec3 velocity = this.getDeltaMovement();
        double dot = 1.0;
        if (velocity.lengthSqr() > 0.01 && realWind.lengthSqr() > 0.01) {
            dot = Math.abs(velocity.normalize().dot(realWind.normalize()));
        }

        // --- 3. LAYER A: LOW FREQ (The Sway) ---
        // Existing "Heavy" movement (Pitch/Yaw)
        double lowFreq = this.tickCount / 20.0 / Math.max(0.1, this.getProperties().get(VehicleStat.MASS));

        float swayX = (float) Utils.cosNoise(lowFreq);
        // Apply "Danger Bias" (Down is generally stronger)
        if (swayX > 0) swayX *= Config.PITCH_DOWN_BIAS.get();

        float swayZ = (float) Utils.cosNoise(lowFreq + 100);

        // --- 4. LAYER B: HIGH FREQ (Shudder) ---
        // New "Fast" movement (Vibration)
        // Frequency is much higher (div 2.0 instead of 20.0)
        double highFreq = this.tickCount / 2.0;

        // Intensity scales with wind speed.
        // We only start shaking if wind is decently strong (> threshold).
        float shudderIntensity = 0.0f;
        if (windSpeed > threshold) {
            // Scale: 0 at threshold, Max at +50mph
            double severity = Math.min((windSpeed - threshold) / 140.0, 1.0); //New change for turbulence scaling rate
            shudderIntensity = (float) (severity * Config.VIBRATION_STRENGTH.get() * 0.1);
            // Note: 0.1 multiplier keeps it more subtle. We want "buzz", not seizure.
        }

        float buzzX = (float) (Utils.cosNoise(highFreq) * shudderIntensity);
        float buzzZ = (float) (Utils.cosNoise(highFreq + 50) * shudderIntensity);

        // --- 5. COMBINE LAYERS ---
        float pitchBias = (float) Math.max(0.3, dot);
        float yawBias = (float) Math.max(0.3, 1.0 - dot);

        // Final X = (Sway * Bias) + Buzz
        // We add Buzz at the end so it shakes regardless of direction
        float finalX = (swayX * wind * pitchBias) + buzzX;
        float finalZ = (swayZ * wind * yawBias) + buzzZ;

        return new Vector3f(finalX, 0.0F, finalZ);
    }
}