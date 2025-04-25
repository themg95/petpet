package dev.mg95.petpet;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class PetPet implements ModInitializer {

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
            if (playerEntity.isSpectator() || !playerEntity.getMainHandStack().isEmpty() || !playerEntity.isSneaking())
                return ActionResult.PASS;

            playerEntity.swingHand(Hand.MAIN_HAND, true);

            if (world.getRandom().nextInt(2) != 0) return ActionResult.SUCCESS;

            double vx = entity.getRandom().nextGaussian() * 0.02;
            double vy = entity.getRandom().nextGaussian() * 0.02;
            double vz = entity.getRandom().nextGaussian() * 0.02;


            ((ServerWorld) world).spawnParticles(ParticleTypes.HEART, entity.getParticleX(1.0), entity.getRandomBodyY() + entity.getEyeHeight(entity.getPose()), entity.getParticleZ(1.0), 1, vx, vy, vz, 0);
            return ActionResult.SUCCESS;
        });
    }
}
