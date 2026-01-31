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

        // Run logic if we are the Server (for all planes) OR the Client Pilot (for our own plane)
        if (!isServer && !isClientPilot) return;

        // --- PREPARE DATA ---
        // 1. Get Real Wind Data
        Vec3 windVector = WindEngine.getWind(this.position(), this.level(), false, !Config.ENABLE_TORNADO_SUCTION.get(), true);
        double rawWindSpeed = windVector.length();
        double baseThreshold = Config.WIND_THRESHOLD.get(); // e.g. 30.0

        // 2. DRIFT LOGIC (The "Push")
        // Starts immediately at the threshold (e.g. > 30mph)
        if (rawWindSpeed > baseThreshold) {
            double influence = Config.WIND_INFLUENCE.get() / 10000.0;
            double effectiveSpeed = rawWindSpeed - baseThreshold;

            // Mass Inertia
            float baseMass = this.getProperties().get(VehicleStat.MASS);
            if (baseMass < 0.1) baseMass = 0.1f;
            double effectiveMass = Math.pow(baseMass, Config.MASS_SCALING.get());

            // Altitude Scaling
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
                    dampeningFactor = 1.0 - (reduction * 0.8);
                }
            }

            // Apply Drift
            double forceMagnitude = (effectiveSpeed * influence * altitudeBonus * dampeningFactor) / effectiveMass;
            Vec3 push = windVector.normalize().scale(forceMagnitude);
            this.setDeltaMovement(this.getDeltaMovement().add(push));

            // 3. AIR POCKETS (The "Drop")
            // DELAYED START: Only happens if wind is significantly higher than threshold (+50)
            double pocketThreshold = baseThreshold + 50.0;

            if (rawWindSpeed > pocketThreshold) {
                if (this.random.nextFloat() < Config.AIR_POCKET_CHANCE.get()) {

                    // Severity scales linearly from the POCKET threshold
                    double severity = Math.min((rawWindSpeed - pocketThreshold) / 50.0, 2.0);

                    double strengthBase = Config.AIR_POCKET_STRENGTH.get() / 10.0;
                    double pocketForce = strengthBase * (1.0 + severity);

                    this.setDeltaMovement(this.getDeltaMovement().add(0, -pocketForce, 0));

                    // FIX: Use player.displayClientMessage (Safe for Server)
                    // Added 'true' to send to Action Bar (Overlay) instead of Chat
                    if (Config.DEBUG_MODE.get() && this.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player player) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c⚠ AIR POCKET!"), true);
                    }
                }
            }

            // Debug Output
            if (Config.DEBUG_MODE.get() && this.tickCount % 20 == 0) {
                // FIX: Use player.displayClientMessage (Safe for Server)
                if (this.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player player) {
                    String color = "§a";
                    String side = isServer ? "§e[S]" : "§b[C]";
                    String dampMsg = (dampeningFactor < 0.99) ? String.format(" | Damp:%.2f", dampeningFactor) : "";
                    String msg = String.format("%s%s Wind:%.0f | Push:%.4f%s",
                            side, color, rawWindSpeed, forceMagnitude, dampMsg);

                    // 'false' sends to Chat
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), false);
                }
            }
        }
    }

    /**
     * VISUAL TURBULENCE (Linear Scaling with Delay)
     */
    @org.spongepowered.asm.mixin.Overwrite
    public Vector3f getWindEffect() {
        // --- 1. BASE DATA ---
        float wind = this.getWindStrength();
        Vec3 realWind = WindEngine.getWind(this.position(), this.level());
        double windSpeed = realWind.length();

        // --- 2. TURBULENCE LOGIC (The "Shake") ---
        // DELAYED START: Starts 30mph after the drift threshold.
        double baseThreshold = Config.WIND_THRESHOLD.get();
        double turbStart = baseThreshold + 30.0;

        double severity = 0.0;

        if (windSpeed > turbStart) {
            // Linear Scaling
            double excess = windSpeed - turbStart;
            double turbMult = Config.TURBULENCE_MULTIPLIER.get() / 100.0;

            // Add to base wind effect
            wind += (float) (excess * turbMult);

            // Calculate a 0.0-1.0 severity factor for the Shudder
            severity = Math.min(excess / 50.0, 1.0);
        }

        // --- 3. DIRECTIONAL BIAS ---
        Vec3 velocity = this.getDeltaMovement();
        double dot = 1.0;
        if (velocity.lengthSqr() > 0.01 && realWind.lengthSqr() > 0.01) {
            dot = Math.abs(velocity.normalize().dot(realWind.normalize()));
        }

        // --- 4. LAYER A: LOW FREQ (Sway) ---
        double lowFreq = this.tickCount / 20.0 / Math.max(0.1, this.getProperties().get(VehicleStat.MASS));
        float swayX = (float) Utils.cosNoise(lowFreq);
        if (swayX > 0) swayX *= Config.PITCH_DOWN_BIAS.get();
        float swayZ = (float) Utils.cosNoise(lowFreq + 100);

        // --- 5. LAYER B: HIGH FREQ (Shudder) ---
        // Only active if we are past the turbulence threshold
        double highFreq = this.tickCount / 2.0;
        float shudderIntensity = 0.0f;

        if (severity > 0) {
            shudderIntensity = (float) (severity * Config.VIBRATION_STRENGTH.get() * 0.1);
        }

        float buzzX = (float) (Utils.cosNoise(highFreq) * shudderIntensity);
        float buzzZ = (float) (Utils.cosNoise(highFreq + 50) * shudderIntensity);

        // --- 6. COMBINE ---
        float pitchBias = (float) Math.max(0.3, dot);
        float yawBias = (float) Math.max(0.3, 1.0 - dot);

        float finalX = (swayX * wind * pitchBias) + buzzX;
        float finalZ = (swayZ * wind * yawBias) + buzzZ;

        return new Vector3f(finalX, 0.0F, finalZ);
    }
}