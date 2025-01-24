package dev.mg95.petpet;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.*;

public class PetPet implements ModInitializer {
    private HashMap<Key, PettingSession> pettingSessions = new HashMap<>();
    private HashMap<Key, Long> totalPettingTimes = new HashMap<>();
    private int ticksSinceLastCheck = 0;
    private boolean checkingPets = false;

    @Override
    public void onInitialize() {
        Gson gson = new Gson();

        Type mapType = new TypeToken<HashMap<String, Long>>() {
        }.getType();

        try (FileReader reader = new FileReader("petpet.json")) {
            HashMap<String, Long> serializedMap = gson.fromJson(reader, mapType);
            for (var key : serializedMap.keySet()) {
                var splitKey = key.split(":");
                totalPettingTimes.put(new Key(UUID.fromString(splitKey[0]), UUID.fromString(splitKey[1])), serializedMap.get(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
            if (playerEntity.isSpectator() || !playerEntity.getMainHandStack().isEmpty() || !playerEntity.isSneaking())
                return ActionResult.PASS;

            playerEntity.swingHand(Hand.MAIN_HAND, true);
            handleSession(playerEntity, entity);

            if (world.getRandom().nextInt(2) != 0) return ActionResult.SUCCESS;

            double vx = entity.getRandom().nextGaussian() * 0.02;
            double vy = entity.getRandom().nextGaussian() * 0.02;
            double vz = entity.getRandom().nextGaussian() * 0.02;


            ((ServerWorld) world).spawnParticles(ParticleTypes.HEART, entity.getParticleX(1.0), entity.getRandomBodyY() + entity.getEyeHeight(entity.getPose()), entity.getParticleZ(1.0), 1, vx, vy, vz, 0);
            return ActionResult.SUCCESS;
        });

        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            if (ticksSinceLastCheck < 200) {
                ticksSinceLastCheck++;
                return;
            }
            ticksSinceLastCheck = 0;
            var time = System.currentTimeMillis();

            checkingPets = true;
            for (var key : pettingSessions.keySet()) {
                if (time - pettingSessions.get(key).lastPet > 1000) {
                    totalPettingTimes.putIfAbsent(key, 0L);
                    totalPettingTimes.put(key, totalPettingTimes.get(key) + pettingSessions.get(key).lastPet - pettingSessions.get(key).startTime);
                    pettingSessions.remove(key);
                }
            }
            checkingPets = false;

            var serializableMap = new HashMap<String, Long>();
            for (var key : totalPettingTimes.keySet()) {
                serializableMap.put(key.petter + ":" + key.receiver, totalPettingTimes.get(key));
            }
            try (FileWriter writer = new FileWriter("petpet.json")) {
                gson.toJson(serializableMap, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("petting_leaderboard")
                .executes(context -> {
                    var s = new StringBuilder();

                    s.append("§2Petting Leaderboard§r\n");

                    ArrayList<Map.Entry<Key, Long>> sortedEntries = new ArrayList<>(totalPettingTimes.entrySet());
                    sortedEntries.sort((entry1, entry2) -> Long.compare(totalPettingTimes.get(entry2.getKey()), totalPettingTimes.get(entry1.getKey())));

                    var i = 0;
                    for (var entry : sortedEntries) {
                        if (i >= 10) break;
                        var key = entry.getKey();
                        s.append(context.getSource().getServer().getUserCache().getByUuid(key.petter).get().getName());
                        s.append(" to ");
                        s.append(context.getSource().getServer().getUserCache().getByUuid(key.receiver).get().getName());
                        s.append(": ");
                        s.append(formatTime(entry.getValue()));
                        s.append("\n");
                        i++;
                    }

                    context.getSource().sendFeedback(() -> Text.literal(s.toString()), false);

                    return 1;
                })));
    }

    private void handleSession(PlayerEntity petGiver, Entity petTaker) {
        if (checkingPets) return;
        if (!petTaker.isPlayer()) return;

        var key = new Key(petGiver.getUuid(), petTaker.getUuid());
        var time = System.currentTimeMillis();
        if (!pettingSessions.containsKey(key)) {
            pettingSessions.put(key, new PettingSession(time, time));
        } else if (time - pettingSessions.get(key).lastPet > 1000) {
            totalPettingTimes.putIfAbsent(key, 0L);
            totalPettingTimes.put(key, totalPettingTimes.get(key) + pettingSessions.get(key).lastPet - pettingSessions.get(key).startTime);
            pettingSessions.remove(key);
        } else {
            pettingSessions.put(key, new PettingSession(pettingSessions.get(key).startTime, time));
        }
    }

    public static String formatTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));

        return String.format("%02d hours, %02d minutes, %02d seconds", hours, minutes, seconds);
    }
}
